package com.kaan.deadzone.item;

import com.kaan.deadzone.registry.ModComponents;
import com.kaan.deadzone.registry.ModDamageTypes;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * Hitscan-Waffe: Kugeln sind keine Entities, sondern ein Strahl, der sofort trifft.
 * Rechtsklick schießt, Schleichen + Rechtsklick lädt nach. Automatikwaffen feuern, solange die Taste gehalten wird.
 */
public class GunItem extends Item {
	private static final Map<PlayerEntity, Long> NEXT_SHOT = new WeakHashMap<>();
	private static final DustParticleEffect TRACER = new DustParticleEffect(0xFFE07A, 0.5F);

	private final GunStats stats;

	public GunItem(GunStats stats, Item.Settings settings) {
		super(settings);
		this.stats = stats;
	}

	public GunStats stats() {
		return this.stats;
	}

	public static int magazine(ItemStack stack) {
		return stack.getOrDefault(ModComponents.MAGAZINE, 0);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (user.isSneaking()) {
			if (world instanceof ServerWorld serverWorld) {
				this.reload(serverWorld, user, stack, true);
			}
			return ActionResult.CONSUME;
		}
		if (this.stats.automatic()) {
			user.setCurrentHand(hand);
		}
		if (world instanceof ServerWorld serverWorld) {
			this.tryFire(serverWorld, user, stack);
		}
		return ActionResult.CONSUME;
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (world instanceof ServerWorld serverWorld && user instanceof PlayerEntity player) {
			this.tryFire(serverWorld, player, stack);
		}
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return this.stats.automatic() ? 72000 : 0;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.NONE;
	}

	// ------------------------------------------------------------------ Schießen

	private void tryFire(ServerWorld world, PlayerEntity player, ItemStack stack) {
		if (player.getItemCooldownManager().isCoolingDown(stack)) {
			return;
		}
		long now = world.getTime();
		Long next = NEXT_SHOT.get(player);
		if (next != null && now < next) {
			return;
		}
		int mag = magazine(stack);
		if (mag <= 0) {
			NEXT_SHOT.put(player, now + 10);
			if (!this.reload(world, player, stack, false)) {
				world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 0.6F, 1.6F);
				player.sendMessage(Text.translatable("hud.deadzone.no_ammo", Text.translatable(this.stats.ammo().item().getTranslationKey())).formatted(Formatting.RED), true);
			}
			return;
		}

		NEXT_SHOT.put(player, now + this.stats.fireDelay());
		stack.set(ModComponents.MAGAZINE, mag - 1);
		this.shoot(world, player);
		this.showHud(player, stack);

		if (mag - 1 <= 0) {
			this.reload(world, player, stack, false);
		}
	}

	private void shoot(ServerWorld world, PlayerEntity player) {
		Random random = player.getRandom();
		Vec3d eye = player.getEyePos();
		Vec3d look = player.getRotationVec(1.0F);
		float yawRad = player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
		Vec3d right = new Vec3d(-MathHelper.cos(yawRad), 0, -MathHelper.sin(yawRad));
		Vec3d muzzle = eye.add(look.multiply(0.7)).add(right.multiply(0.3)).add(0, -0.18, 0);

		float spread = this.stats.spread() * (player.isSneaking() ? 0.5F : 1.0F);
		double spreadRad = spread * MathHelper.RADIANS_PER_DEGREE;
		for (int i = 0; i < this.stats.pellets(); i++) {
			Vec3d dir = look.add(random.nextGaussian() * spreadRad, random.nextGaussian() * spreadRad, random.nextGaussian() * spreadRad).normalize();
			this.fireRay(world, player, eye, muzzle, dir);
		}

		this.stats.sound().play(world, player.getX(), player.getEyeY(), player.getZ());
		// Mündungsfeuer nur für andere Spieler: direkt vor der eigenen Kamera würde es die Sicht verdecken
		for (ServerPlayerEntity viewer : world.getPlayers()) {
			if (viewer != player) {
				world.spawnParticles(viewer, ParticleTypes.SMOKE, false, false, muzzle.x, muzzle.y, muzzle.z, 2, 0.02, 0.02, 0.02, 0.01);
				world.spawnParticles(viewer, ParticleTypes.FLAME, false, false, muzzle.x, muzzle.y, muzzle.z, 1, 0.0, 0.0, 0.0, 0.0);
			}
		}
		if (this.stats.explosion() > 0) {
			player.addVelocity(look.multiply(-0.25));
			player.knockedBack = true;
		}
	}

	private void fireRay(ServerWorld world, PlayerEntity player, Vec3d eye, Vec3d muzzle, Vec3d dir) {
		double range = this.stats.range();
		Vec3d end = eye.add(dir.multiply(range));
		BlockHitResult blockHit = world.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
		Vec3d stop = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getPos();

		record Hit(Entity entity, Vec3d pos, double dist) {
		}
		List<Hit> hits = new ArrayList<>();
		Box area = new Box(eye, stop).expand(1.0);
		for (Entity e : world.getOtherEntities(player, area, e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e.canHit())) {
			Optional<Vec3d> p = e.getBoundingBox().expand(0.15).raycast(eye, stop);
			p.ifPresent(pos -> hits.add(new Hit(e, pos, pos.squaredDistanceTo(eye))));
		}
		hits.sort(Comparator.comparingDouble(Hit::dist));

		Vec3d impact = stop;
		int maxHits = 1 + this.stats.pierce();
		int done = 0;
		for (Hit hit : hits) {
			if (done >= maxHits) {
				break;
			}
			impact = hit.pos();
			if (this.stats.explosion() > 0) {
				break;
			}
			this.damageEntity(world, player, hit.entity(), hit.pos(), Math.sqrt(hit.dist()));
			done++;
		}
		boolean hitEntity = done > 0 || (this.stats.explosion() > 0 && !hits.isEmpty());

		this.drawTracer(world, muzzle, impact);

		if (this.stats.explosion() > 0) {
			world.createExplosion(player, impact.x, impact.y, impact.z, this.stats.explosion(),
					this.stats.breaksBlocks() ? World.ExplosionSourceType.TNT : World.ExplosionSourceType.NONE);
			return;
		}
		if (!hitEntity && blockHit.getType() == HitResult.Type.BLOCK) {
			BlockState state = world.getBlockState(blockHit.getBlockPos());
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), impact.x, impact.y, impact.z, 6, 0.05, 0.05, 0.05, 0.1);
			if ((state.isIn(ConventionalBlockTags.GLASS_BLOCKS) || state.isIn(ConventionalBlockTags.GLASS_PANES))
					&& player instanceof ServerPlayerEntity sp && sp.canModifyAt(world, blockHit.getBlockPos())) {
				world.breakBlock(blockHit.getBlockPos(), false, player);
			}
		}
	}

	private void damageEntity(ServerWorld world, PlayerEntity player, Entity target, Vec3d hitPos, double distance) {
		float damage = this.stats.damage();
		if (this.stats.pellets() > 1) {
			damage *= (float) Math.max(0.4, 1.0 - distance / this.stats.range());
		}
		boolean headshot = this.stats.pellets() == 1 && hitPos.y >= target.getY() + target.getHeight() * 0.75;
		if (headshot) {
			damage *= 1.75F;
		}
		DamageSource source = ModDamageTypes.of(world, ModDamageTypes.BULLET, player);
		target.timeUntilRegen = 0;
		if (target.damage(world, source, damage)) {
			if (headshot) {
				world.spawnParticles(ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z, 12, 0.1, 0.1, 0.1, 0.3);
				world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.5F, 1.4F);
				player.sendMessage(Text.translatable("hud.deadzone.headshot").formatted(Formatting.RED, Formatting.BOLD), true);
			} else {
				world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, hitPos.x, hitPos.y, hitPos.z, 2, 0.1, 0.1, 0.1, 0.1);
			}
		}
	}

	private void drawTracer(ServerWorld world, Vec3d from, Vec3d to) {
		Vec3d delta = to.subtract(from);
		double length = delta.length();
		if (length < 1.0) {
			return;
		}
		ParticleEffect particle = this.stats.explosion() > 0 ? ParticleTypes.LARGE_SMOKE : TRACER;
		double step = this.stats.explosion() > 0 ? 1.0 : 2.0;
		int points = (int) Math.min(length / step, 24);
		for (int i = 1; i <= points; i++) {
			Vec3d p = from.add(delta.multiply(i * step / length));
			world.spawnParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
	}

	// ------------------------------------------------------------------ Nachladen

	/** Füllt das Magazin aus dem Inventar auf. Im Kreativmodus kostet das keine Munition. */
	private boolean reload(ServerWorld world, PlayerEntity player, ItemStack stack, boolean manual) {
		int mag = magazine(stack);
		int needed = this.stats.magazine() - mag;
		if (needed <= 0 || player.getItemCooldownManager().isCoolingDown(stack)) {
			return manual;
		}
		int taken;
		if (player.isCreative()) {
			taken = needed;
		} else {
			taken = takeAmmo(player.getInventory(), this.stats.ammo().item(), needed);
		}
		if (taken <= 0) {
			if (manual) {
				player.sendMessage(Text.translatable("hud.deadzone.no_ammo", Text.translatable(this.stats.ammo().item().getTranslationKey())).formatted(Formatting.RED), true);
			}
			return false;
		}
		stack.set(ModComponents.MAGAZINE, mag + taken);
		player.getItemCooldownManager().set(stack, this.stats.reloadTicks());
		player.clearActiveItem();
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_CROSSBOW_LOADING_END.value(), SoundCategory.PLAYERS, 1.0F, 0.8F);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.PLAYERS, 0.6F, 1.6F);
		player.sendMessage(Text.translatable("hud.deadzone.reloading").formatted(Formatting.YELLOW), true);
		return true;
	}

	private static int takeAmmo(PlayerInventory inventory, Item ammo, int wanted) {
		int taken = 0;
		for (int i = 0; i < inventory.size() && taken < wanted; i++) {
			ItemStack s = inventory.getStack(i);
			if (s.isOf(ammo)) {
				int n = Math.min(s.getCount(), wanted - taken);
				s.decrement(n);
				taken += n;
			}
		}
		return taken;
	}

	private void showHud(PlayerEntity player, ItemStack stack) {
		int reserve = player.isCreative() ? -1 : player.getInventory().count(this.stats.ammo().item());
		Text reserveText = reserve < 0 ? Text.literal("∞") : Text.literal(String.valueOf(reserve));
		player.sendMessage(
				Text.empty()
						.append(stack.getName().copy().formatted(Formatting.GOLD))
						.append(Text.literal("   " + magazine(stack) + " / " + this.stats.magazine()).formatted(Formatting.WHITE, Formatting.BOLD))
						.append(Text.literal("   | ").formatted(Formatting.DARK_GRAY))
						.append(reserveText.copy().formatted(Formatting.GRAY)),
				true
		);
	}

	// ------------------------------------------------------------------ Anzeige

	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getItemBarStep(ItemStack stack) {
		return Math.round(13.0F * magazine(stack) / this.stats.magazine());
	}

	@Override
	public int getItemBarColor(ItemStack stack) {
		return magazine(stack) == 0 ? 0xFF4040 : 0xFFC83D;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
		GunStats s = this.stats;
		tooltip.accept(Text.translatable("tooltip.deadzone.magazine", magazine(stack), s.magazine()).formatted(Formatting.GRAY));
		String damage = s.pellets() > 1 ? s.pellets() + "×" + fmt(s.damage()) : fmt(s.damage());
		tooltip.accept(Text.translatable("tooltip.deadzone.damage", damage).formatted(Formatting.GRAY));
		tooltip.accept(Text.translatable("tooltip.deadzone.rate", fmt(s.shotsPerSecond())).formatted(Formatting.GRAY));
		tooltip.accept(Text.translatable("tooltip.deadzone.ammo", Text.translatable(s.ammo().item().getTranslationKey())).formatted(Formatting.GRAY));
		if (s.automatic()) {
			tooltip.accept(Text.translatable("tooltip.deadzone.automatic").formatted(Formatting.AQUA));
		}
		if (s.pierce() > 0) {
			tooltip.accept(Text.translatable("tooltip.deadzone.pierce", s.pierce() + 1).formatted(Formatting.AQUA));
		}
		if (s.explosion() > 0) {
			tooltip.accept(Text.translatable("tooltip.deadzone.explosive").formatted(Formatting.RED));
		}
		tooltip.accept(Text.translatable("tooltip.deadzone.controls").formatted(Formatting.DARK_GRAY));
	}

	private static String fmt(float f) {
		return f == (int) f ? String.valueOf((int) f) : String.format(java.util.Locale.ROOT, "%.1f", f);
	}
}
