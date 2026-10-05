package com.kaan.deadzone.client.shorts;

import com.kaan.deadzone.entity.DzZombie;
import com.kaan.deadzone.registry.ModEntities;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Nur für die Video-Produktion (wird vor dem Release entfernt): spielt ein Drehbuch ab und nimmt
 * jedes gerenderte Bild in 1080x1920 auf. Das Spiel wird per /tick rate so verlangsamt, dass jedes
 * Bild genau 1/60 s Spielzeit ist, dadurch ist das Video flüssig, egal wie schnell gerendert wird.
 */
public final class ShortsDirector {
	public static final int W = 1080;
	public static final int H = 1920;
	public static final int FPS = 60;

	public static ShortsDirector INSTANCE;

	public record Pose(double x, double y, double z, float yaw, float pitch) {
	}

	/** Kamera-Vorgabe für das aktuelle Bild, null = normale Spieler-Ansicht. */
	public static volatile Pose camera;

	private enum Phase { TITLE, JOINING, SETUP, ROLLING, FLUSH, DONE }

	interface FrameAction {
		void run(ShortsDirector d, MinecraftClient c, int f, double t);
	}

	interface CamFn {
		Pose at(ShortsDirector d, int f, double t);
	}

	static final class Shot {
		final String name;
		final int frames;
		int setupTicks = 120;
		boolean hud;
		int fov = 80;
		java.util.function.BiConsumer<ShortsDirector, MinecraftClient> setup = (d, c) -> {
		};
		java.util.function.BiConsumer<ShortsDirector, MinecraftClient> beforeRoll = (d, c) -> {
		};
		/** Wird vor dem Start geprüft; solange false, wird weiter gewartet (z. B. bis Chunks geladen sind). */
		java.util.function.BiPredicate<ShortsDirector, MinecraftClient> ready = (d, c) -> true;
		FrameAction frame = (d, c, f, t) -> {
		};
		CamFn cam;

		Shot(String name, int frames) {
			this.name = name;
			this.frames = frames;
		}
	}

	private final File outDir;
	private final List<Shot> shots = new ArrayList<>();
	private Phase phase = Phase.TITLE;
	private int titleTicks;
	private boolean listening;
	private int retries;
	private int shotIndex = -1;
	private int setupLeft;
	private int shotFrame;
	private int globalFrame;
	private int flushFrames;
	private final List<int[]> shotStarts = new ArrayList<>();

	// Aufnahme
	private Process ffmpeg;
	private OutputStream ffmpegIn;
	private Thread writerThread;
	private final TreeMap<Integer, byte[]> pending = new TreeMap<>();
	private int nextToWrite;
	private int submitted;
	private volatile boolean writerDone;
	private BufferedWriter soundLog;

	// Tempo-Anpassung
	private long lastFrameNanos;
	private double emaFrameMs = 33;

	// Szenen-Daten (werden beim Setup gemessen)
	final int ox = Integer.getInteger("deadzone.shorts.cityX", -968);
	final int oz = Integer.getInteger("deadzone.shorts.cityZ", -968);
	final int sx = ox - 105;
	final int sz = oz - 105;
	int y0 = 70;
	int highwayRow = 2;
	int gapX;
	final int bunkerX = Integer.getInteger("deadzone.shorts.bunkerX", 312);
	final int bunkerZ = Integer.getInteger("deadzone.shorts.bunkerZ", 232);
	int bunkerSurface = 70;
	LivingEntity aimTarget;

	private ShortsDirector(File outDir) {
		this.outDir = outDir;
	}

	public static void register() {
		if (System.getProperty("deadzone.shorts") == null) {
			return;
		}
		File out = new File(MinecraftClient.getInstance().runDirectory, "shorts");
		out.mkdirs();
		INSTANCE = new ShortsDirector(out);
		Script.build(INSTANCE.shots);
		ClientTickEvents.END_CLIENT_TICK.register(INSTANCE::tick);
	}

	// ------------------------------------------------------------------ Hilfen für das Drehbuch

	void cmd(MinecraftClient client, String... commands) {
		IntegratedServer server = client.getServer();
		if (server == null) {
			return;
		}
		server.execute(() -> {
			ServerCommandSource source = server.getCommandSource().withOutput(new CommandOutput() {
				@Override
				public void sendMessage(Text message) {
					System.out.println("[SHORTS] " + message.getString());
				}

				@Override
				public boolean shouldReceiveFeedback() {
					return true;
				}

				@Override
				public boolean shouldTrackOutput() {
					return true;
				}

				@Override
				public boolean shouldBroadcastConsoleToOps() {
					return false;
				}
			});
			for (String c : commands) {
				server.getCommandManager().parseAndExecute(source, c);
			}
		});
	}

	static double ease(double t) {
		t = MathHelper.clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}

	static Pose look(double x, double y, double z, double tx, double ty, double tz) {
		double dx = tx - x;
		double dy = ty - y;
		double dz = tz - z;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		return new Pose(x, y, z, yaw, pitch);
	}

	static double lerp(double t, double a, double b) {
		return a + (b - a) * t;
	}

	private static boolean isRoad(net.minecraft.block.BlockState st) {
		return st.isOf(Blocks.BLACK_CONCRETE) || st.isOf(Blocks.YELLOW_TERRACOTTA) || st.isOf(Blocks.POLISHED_BLACKSTONE)
				|| st.isOf(Blocks.COBBLED_DEEPSLATE) || st.isOf(Blocks.ANDESITE);
	}

	/** Misst Straßenhöhe, Hochstraße und Abbruchkante der Mega-Stadt. false, solange die Chunks noch fehlen. */
	boolean measureCity(MinecraftClient c) {
		java.util.Map<Integer, Integer> votes = new java.util.HashMap<>();
		int x = this.sx + 90;
		for (int z = this.sz + 10; z < this.sz + 200; z += 7) {
			int rel = z - this.sz;
			if ((rel >= 56 && rel <= 66) || (rel >= 143 && rel <= 153)) {
				continue;
			}
			for (int y = 200; y > -30; y--) {
				net.minecraft.block.BlockState st = c.world.getBlockState(new BlockPos(x, y, z));
				if (st.isAir() || st.isOf(Blocks.MOSS_CARPET)) {
					continue;
				}
				if (isRoad(st)) {
					votes.merge(y, 1, Integer::sum);
				}
				break;
			}
		}
		int bestY = 0;
		int bestVotes = 0;
		for (var e : votes.entrySet()) {
			if (e.getValue() > bestVotes) {
				bestVotes = e.getValue();
				bestY = e.getKey();
			}
		}
		if (bestVotes < 4) {
			System.out.println("[SHORTS] city not loaded yet " + votes);
			return false;
		}
		this.y0 = bestY;
		int deck = this.y0 + 12;
		int best = -1;
		int bestCount = -1;
		for (int row : new int[]{2, 5}) {
			int z = this.sz + row * 29 + 3;
			int count = 0;
			for (int xx = this.sx + 10; xx < this.sx + 200; xx += 10) {
				if (!c.world.getBlockState(new BlockPos(xx, deck, z)).isAir()) {
					count++;
				}
			}
			if (count > bestCount) {
				bestCount = count;
				best = row;
			}
		}
		this.highwayRow = best;
		int zc = this.sz + this.highwayRow * 29 + 3;
		boolean seenDeck = false;
		this.gapX = this.sx + 70;
		for (int xx = this.sx + 2; xx < this.sx + 200; xx++) {
			boolean air = true;
			for (int k = 0; k < 4; k++) {
				if (!c.world.getBlockState(new BlockPos(xx + k, deck, zc)).isAir()) {
					air = false;
					break;
				}
			}
			if (!air) {
				seenDeck = true;
			} else if (seenDeck && xx - this.sx > 20) {
				this.gapX = xx;
				break;
			}
		}
		System.out.println("[SHORTS] city y0=" + this.y0 + " highwayRow=" + this.highwayRow + " gapX=" + this.gapX + " votes=" + votes);
		return true;
	}

	boolean measureBunker(MinecraftClient c) {
		for (int y = 160; y > -40; y--) {
			if (c.world.getBlockState(new BlockPos(this.bunkerX, y, this.bunkerZ)).isOf(Blocks.SPRUCE_TRAPDOOR)) {
				this.bunkerSurface = y;
				System.out.println("[SHORTS] bunker surface=" + this.bunkerSurface);
				return true;
			}
		}
		System.out.println("[SHORTS] bunker not loaded yet");
		return false;
	}

	/** Weiches Zielen auf ein Ziel (für die Ich-Perspektive). */
	void aim(MinecraftClient c, Entity target, double headFactor, float smoothing) {
		if (target == null || c.player == null) {
			return;
		}
		Vec3d eye = c.player.getEyePos();
		Vec3d to = new Vec3d(target.getX(), target.getY() + target.getHeight() * headFactor, target.getZ());
		Pose p = look(eye.x, eye.y, eye.z, to.x, to.y, to.z);
		float yaw = c.player.getYaw() + MathHelper.wrapDegrees(p.yaw() - c.player.getYaw()) * smoothing;
		float pitch = c.player.getPitch() + (p.pitch() - c.player.getPitch()) * smoothing;
		c.player.setYaw(yaw);
		c.player.setPitch(pitch);
		c.player.lastYaw = yaw;
		c.player.lastPitch = pitch;
		c.player.setHeadYaw(yaw);
	}

	/** Nächster Zombie, optional ohne Kolosse (die sind fürs RPG reserviert). */
	LivingEntity nearestZombieExcept(MinecraftClient c, boolean skipBrutes) {
		LivingEntity best = null;
		double bestD = 45 * 45;
		for (Entity e : c.world.getEntities()) {
			if (!(e instanceof DzZombie z) || !z.isAlive() || z.getHealth() <= 0 || (skipBrutes && z.getType() == ModEntities.BRUTE)) {
				continue;
			}
			double dist = z.squaredDistanceTo(c.player);
			if (dist < bestD) {
				best = z;
				bestD = dist;
			}
		}
		return best;
	}

	LivingEntity nearestZombie(MinecraftClient c, boolean bruteOnly) {
		LivingEntity best = null;
		double bestD = 45 * 45;
		Vec3d look = c.player.getRotationVec(1.0F);
		for (Entity e : c.world.getEntities()) {
			if (!(e instanceof DzZombie z) || !z.isAlive() || z.getHealth() <= 0) {
				continue;
			}
			if (bruteOnly && z.getType() != ModEntities.BRUTE) {
				continue;
			}
			Vec3d d = z.getEntityPos().subtract(c.player.getEntityPos());
			double dist = d.lengthSquared();
			if (dist < bestD && d.normalize().dotProduct(look) > -0.2) {
				best = z;
				bestD = dist;
			}
		}
		return best;
	}

	// ------------------------------------------------------------------ Ablauf

	private void tick(MinecraftClient client) {
		switch (this.phase) {
			case TITLE -> {
				if (!this.listening) {
					this.listening = true;
					client.getSoundManager().registerListener((sound, set, range) -> this.onSound(sound, range));
				}
				if (client.currentScreen instanceof TitleScreen && ++this.titleTicks == 60) {
					this.phase = Phase.JOINING;
					client.createIntegratedServerLoader().start(System.getProperty("deadzone.shorts"), () -> client.setScreen(new TitleScreen()));
				}
			}
			case JOINING -> {
				if (client.player != null && client.world != null) {
					this.applyResolution(client);
					this.cmd(client, "gamerule advance_time false", "gamerule spawn_mobs false", "gamerule fire_spread_radius_around_player 0",
							"gamerule advance_weather false", "weather clear", "recipe give @a *", "gamerule send_command_feedback false");
					client.options.getDamageTiltStrength().setValue(0.0);
					client.options.getDistortionEffectScale().setValue(0.0);
					client.options.getFovEffectScale().setValue(0.0);
					client.options.getBobView().setValue(true);
					this.nextShot(client);
				}
			}
			case SETUP -> {
				if (client.currentScreen != null) {
					client.setScreen(null);
				}
				if (camera != null && client.player != null && client.player.isSpectator()) {
					client.player.setPosition(camera.x(), camera.y(), camera.z());
				}
				if (--this.setupLeft <= 0) {
					Shot shot = this.shots.get(this.shotIndex);
					if (shot.ready.test(this, client) || ++this.retries > 20) {
						this.retries = 0;
						this.startRolling(client);
					} else {
						this.setupLeft = 40;
					}
				}
			}
			case ROLLING -> {
				if (client.currentScreen != null) {
					client.setScreen(null);
				}
				Pose p = camera;
				if (p != null && client.player != null && client.player.isSpectator()) {
					client.player.setPosition(p.x(), p.y(), p.z());
				}
			}
			default -> {
			}
		}
	}

	private void nextShot(MinecraftClient client) {
		this.shotIndex++;
		if (this.shotIndex >= this.shots.size()) {
			this.phase = Phase.FLUSH;
			this.flushFrames = 90;
			camera = null;
			return;
		}
		Shot shot = this.shots.get(this.shotIndex);
		System.out.println("[SHORTS] setup " + shot.name);
		this.setTickRate(client, 20.0F);
		shot.setup.accept(this, client);
		this.setupLeft = shot.setupTicks;
		this.phase = Phase.SETUP;
		camera = shot.cam != null ? shot.cam.at(this, 0, 0) : null;
	}

	private void startRolling(MinecraftClient client) {
		Shot shot = this.shots.get(this.shotIndex);
		shot.beforeRoll.accept(this, client);
		this.shotFrame = 0;
		this.shotStarts.add(new int[]{this.globalFrame, shot.frames});
		this.phase = Phase.ROLLING;
		this.lastFrameNanos = 0;
		this.setTickRate(client, (float) MathHelper.clamp(20.0 * (1000.0 / this.emaFrameMs) / FPS, 1.0, 20.0));
		System.out.println("[SHORTS] rolling " + shot.name + " at frame " + this.globalFrame);
	}

	private void setTickRate(MinecraftClient client, float rate) {
		IntegratedServer server = client.getServer();
		if (server != null) {
			server.execute(() -> server.getTickManager().setTickRate(rate));
		}
	}

	private void applyResolution(MinecraftClient client) {
		WindowAccess window = (WindowAccess) (Object) client.getWindow();
		if (window.deadzone$getFbWidth() != W || window.deadzone$getFbHeight() != H) {
			window.deadzone$setFbSize(W, H);
			client.onResolutionChanged();
		}
	}

	/** Am Anfang jedes Bildes (vor dem Rendern). */
	public void onFrameStart(MinecraftClient client) {
		if (this.phase == Phase.TITLE) {
			return;
		}
		if (client.world != null) {
			this.applyResolution(client);
		}
		if (this.phase == Phase.SETUP || this.phase == Phase.ROLLING) {
			Shot shot = this.shots.get(this.shotIndex);
			client.options.hudHidden = !shot.hud;
			client.options.getFov().setValue(shot.fov);
			client.options.setPerspective(Perspective.FIRST_PERSON);
			if (this.phase == Phase.ROLLING) {
				double t = (double) this.shotFrame / FPS;
				shot.frame.run(this, client, this.shotFrame, t);
				camera = shot.cam != null ? shot.cam.at(this, this.shotFrame, t) : null;
			}
		}
	}

	/** Nach dem Rendern, bevor das Bild auf den Bildschirm kommt. */
	public void onFrameEnd(MinecraftClient client) {
		if (this.phase == Phase.FLUSH) {
			if (--this.flushFrames <= 0) {
				this.finish(client);
			}
			return;
		}
		if (this.phase != Phase.ROLLING) {
			return;
		}
		long now = System.nanoTime();
		if (this.lastFrameNanos != 0) {
			double ms = (now - this.lastFrameNanos) / 1.0E6;
			this.emaFrameMs = this.emaFrameMs * 0.9 + ms * 0.1;
		}
		this.lastFrameNanos = now;
		if (this.shotFrame % 30 == 0) {
			this.setTickRate(client, (float) MathHelper.clamp(20.0 * (1000.0 / this.emaFrameMs) / FPS, 1.0, 20.0));
		}
		this.capture(client.getFramebuffer(), this.globalFrame);
		// Gegendruck: nicht mehr als 8 fertige, aber noch nicht geschriebene Bilder im Speicher
		synchronized (this.pending) {
			while (this.pending.size() > 8) {
				try {
					this.pending.wait(50);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
		this.globalFrame++;
		this.shotFrame++;
		if (this.shotFrame >= this.shots.get(this.shotIndex).frames) {
			this.nextShot(client);
		}
	}

	// ------------------------------------------------------------------ Aufnahme

	private void ensureFfmpeg() {
		if (this.ffmpeg != null) {
			return;
		}
		try {
			File raw = new File(this.outDir, "raw.mp4");
			ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-y", "-loglevel", "warning", "-f", "rawvideo", "-pix_fmt", "rgba",
					"-s", W + "x" + H, "-r", String.valueOf(FPS), "-i", "-", "-vf", "vflip",
					"-c:v", "libx264", "-preset", "veryfast", "-crf", "12", "-pix_fmt", "yuv420p", raw.getAbsolutePath());
			pb.redirectErrorStream(true);
			pb.redirectOutput(new File(this.outDir, "ffmpeg.log"));
			this.ffmpeg = pb.start();
			this.ffmpegIn = this.ffmpeg.getOutputStream();
			this.soundLog = Files.newBufferedWriter(new File(this.outDir, "sounds.jsonl").toPath());
			this.writerThread = new Thread(this::writerLoop, "shorts-writer");
			this.writerThread.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void capture(Framebuffer fb, int index) {
		this.ensureFfmpeg();
		GpuTexture tex = fb.getColorAttachment();
		int w = fb.textureWidth;
		int h = fb.textureHeight;
		if (w != W || h != H) {
			System.out.println("[SHORTS] wrong framebuffer size " + w + "x" + h);
		}
		this.submitted++;
		GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "shorts frame", 9, (long) w * h * 4);
		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		encoder.copyTextureToBuffer(tex, buffer, 0L, () -> {
			byte[] bytes = new byte[W * H * 4];
			try (GpuBuffer.MappedView view = encoder.mapBuffer(buffer, true, false)) {
				view.data().get(bytes, 0, Math.min(bytes.length, view.data().remaining()));
			}
			buffer.close();
			synchronized (this.pending) {
				this.pending.put(index, bytes);
				this.pending.notifyAll();
			}
		}, 0);
	}

	private void writerLoop() {
		try {
			while (true) {
				byte[] frame;
				synchronized (this.pending) {
					while (!this.pending.containsKey(this.nextToWrite)) {
						if (this.writerDone && this.nextToWrite >= this.submitted) {
							this.ffmpegIn.close();
							return;
						}
						this.pending.wait(200);
					}
					frame = this.pending.remove(this.nextToWrite);
				}
				this.ffmpegIn.write(frame);
				synchronized (this.pending) {
					this.pending.notifyAll();
				}
				this.nextToWrite++;
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void onSound(SoundInstance sound, float range) {
		if (this.phase != Phase.ROLLING || this.soundLog == null || sound.getCategory() == SoundCategory.MUSIC
				|| sound.getCategory() == SoundCategory.RECORDS || sound.getSound() == null) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		Camera cam = client.gameRenderer.getCamera();
		Vec3d cp = cam.getCameraPos();
		try {
			this.soundLog.write(String.format(Locale.ROOT,
					"{\"f\":%d,\"loc\":\"%s\",\"vol\":%.4f,\"pitch\":%.4f,\"x\":%.2f,\"y\":%.2f,\"z\":%.2f,\"rel\":%b,\"range\":%.2f,\"cx\":%.2f,\"cy\":%.2f,\"cz\":%.2f,\"yaw\":%.2f}%n",
					this.globalFrame, sound.getSound().getLocation(), sound.getVolume(), sound.getPitch(), sound.getX(), sound.getY(), sound.getZ(),
					sound.isRelative() || sound.getAttenuationType() == SoundInstance.AttenuationType.NONE,
					Float.isInfinite(range) ? -1.0F : range, cp.x, cp.y, cp.z, cam.getYaw()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void finish(MinecraftClient client) {
		this.phase = Phase.DONE;
		try {
			this.soundLog.close();
			try (BufferedWriter w = Files.newBufferedWriter(new File(this.outDir, "shots.json").toPath())) {
				StringBuilder sb = new StringBuilder("[");
				for (int i = 0; i < this.shotStarts.size(); i++) {
					int[] s = this.shotStarts.get(i);
					sb.append(i == 0 ? "" : ",").append("{\"name\":\"").append(this.shots.get(i).name).append("\",\"start\":").append(s[0])
							.append(",\"frames\":").append(s[1]).append("}");
				}
				w.write(sb.append("]").toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		synchronized (this.pending) {
			this.writerDone = true;
			this.pending.notifyAll();
		}
		new Thread(() -> {
			try {
				this.writerThread.join();
				this.ffmpeg.waitFor();
				System.out.println("[SHORTS] DONE " + this.globalFrame + " frames");
			} catch (InterruptedException ignored) {
			}
			client.execute(client::scheduleStop);
		}, "shorts-finish").start();
	}

	public interface WindowAccess {
		int deadzone$getFbWidth();

		int deadzone$getFbHeight();

		void deadzone$setFbSize(int w, int h);
	}
}
