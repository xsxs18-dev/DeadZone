package com.kaan.deadzone.event;

import com.kaan.deadzone.entity.DzZombie;
import com.kaan.deadzone.registry.ModEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;

import java.util.List;

/**
 * Jede 7. Nacht ist Blutmond: Warnung beim Einbruch der Nacht, dann rücken alle 30 Sekunden
 * Zombie-Horden auf jeden Spieler im Überlebensmodus zu. Gedeckelt, damit es nicht laggt.
 */
public final class BloodMoon {
	public static final int EVERY_DAYS = 7;
	private static final int MAX_ZOMBIES_NEAR_PLAYER = 35;
	private static boolean active;

	private BloodMoon() {
	}

	public static boolean isBloodMoon(ServerWorld world) {
		long day = world.getTimeOfDay() / 24000L;
		return day % EVERY_DAYS == EVERY_DAYS - 1 && world.isNight();
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 20 != 0) {
				return;
			}
			ServerWorld world = server.getOverworld();
			boolean now = isBloodMoon(world) && world.getDifficulty() != Difficulty.PEACEFUL;
			if (now && !active) {
				announce(world);
			}
			active = now;
			if (now && server.getTicks() % 600 == 0) {
				for (ServerPlayerEntity player : world.getPlayers()) {
					if (!player.isCreative() && !player.isSpectator()) {
						spawnHorde(world, player);
					}
				}
			}
		});
	}

	private static void announce(ServerWorld world) {
		for (ServerPlayerEntity player : world.getPlayers()) {
			player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 80, 30));
			player.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("title.deadzone.blood_moon").formatted(Formatting.DARK_RED, Formatting.BOLD)));
			player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.translatable("title.deadzone.blood_moon.sub").formatted(Formatting.RED)));
			world.playSound(null, player.getBlockPos(), SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.HOSTILE, 64.0F, 0.6F);
		}
	}

	private static void spawnHorde(ServerWorld world, ServerPlayerEntity player) {
		List<ZombieEntity> near = world.getEntitiesByClass(ZombieEntity.class, player.getBoundingBox().expand(48), e -> e instanceof DzZombie);
		if (near.size() >= MAX_ZOMBIES_NEAR_PLAYER) {
			return;
		}
		int count = 5 + world.random.nextInt(4);
		double angle = world.random.nextDouble() * Math.PI * 2;
		double dist = 24 + world.random.nextDouble() * 10;
		int cx = MathHelper.floor(player.getX() + Math.cos(angle) * dist);
		int cz = MathHelper.floor(player.getZ() + Math.sin(angle) * dist);
		EntityType<?>[] pool = {ModEntities.WALKER, ModEntities.WALKER, ModEntities.RUNNER, ModEntities.RUNNER, ModEntities.LEAPER,
				ModEntities.CRAWLER, ModEntities.SPITTER, ModEntities.BLOATER, ModEntities.SCREAMER, ModEntities.BRUTE};
		for (int i = 0; i < count; i++) {
			int x = cx + world.random.nextInt(7) - 3;
			int z = cz + world.random.nextInt(7) - 3;
			int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos pos = new BlockPos(x, y, z);
			if (!world.getWorldBorder().contains(pos) || !world.isChunkLoaded(pos)) {
				continue;
			}
			EntityType<?> type = pool[world.random.nextInt(pool.length)];
			if (!(type.create(world, SpawnReason.EVENT) instanceof ZombieEntity zombie)) {
				continue;
			}
			zombie.refreshPositionAndAngles(x + 0.5, y, z + 0.5, world.random.nextFloat() * 360F, 0);
			if (!world.isSpaceEmpty(zombie)) {
				continue;
			}
			zombie.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null);
			zombie.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 120, 0));
			zombie.setTarget(player);
			world.spawnEntity(zombie);
			world.spawnParticles(ParticleTypes.CRIMSON_SPORE, x + 0.5, y + 1, z + 0.5, 15, 0.4, 0.8, 0.4, 0.01);
		}
	}
}
