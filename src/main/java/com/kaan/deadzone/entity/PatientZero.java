package com.kaan.deadzone.entity;

import com.kaan.deadzone.registry.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;

/**
 * Boss im Geheimbunker: das erste infizierte Versuchsobjekt. Bossleiste, Bodenstampfer, Sprungangriff,
 * ruft Sprinter herbei und wird unter 50 % Leben rasend.
 */
public class PatientZero extends DzZombie {
	private final ServerBossBar bossBar = new ServerBossBar(
			Text.translatable("entity.deadzone.patient_zero").formatted(Formatting.DARK_RED, Formatting.BOLD),
			BossBar.Color.RED, BossBar.Style.NOTCHED_10);
	private int slamCooldown = 80;
	private int leapCooldown = 120;
	private int summonCooldown = 200;
	private boolean enraged;

	public PatientZero(EntityType<? extends ZombieEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 200;
		this.setPersistent();
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return ZombieEntity.createZombieAttributes()
				.add(EntityAttributes.MAX_HEALTH, 300)
				.add(EntityAttributes.MOVEMENT_SPEED, 0.25)
				.add(EntityAttributes.ATTACK_DAMAGE, 14)
				.add(EntityAttributes.ARMOR, 10)
				.add(EntityAttributes.ARMOR_TOUGHNESS, 4)
				.add(EntityAttributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(EntityAttributes.ATTACK_KNOCKBACK, 2.0)
				.add(EntityAttributes.FOLLOW_RANGE, 48)
				.add(EntityAttributes.SCALE, 2.2)
				.add(EntityAttributes.STEP_HEIGHT, 1.5);
	}

	@Override
	protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
	}

	@Override
	public boolean cannotDespawn() {
		return true;
	}

	@Override
	public boolean handleFallDamage(double fallDistance, float damagePerDistance, DamageSource damageSource) {
		return false;
	}

	@Override
	public void onStoppedTrackingBy(ServerPlayerEntity player) {
		super.onStoppedTrackingBy(player);
		this.bossBar.removePlayer(player);
	}

	/** Bossleiste nur in der Nähe zeigen, nicht durch 40 Blöcke Erde hindurch. */
	private void updateBossBarViewers(ServerWorld world) {
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.squaredDistanceTo(this) < 40 * 40) {
				this.bossBar.addPlayer(player);
			} else {
				this.bossBar.removePlayer(player);
			}
		}
	}

	@Override
	protected void specialTick(ServerWorld world) {
		this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
		if (this.age % 20 == 0) {
			this.updateBossBarViewers(world);
		}
		LivingEntity target = this.getTarget();
		if (!this.enraged && this.getHealth() < this.getMaxHealth() * 0.5F) {
			this.enraged = true;
			this.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, -1, 1));
			this.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, -1, 1));
			this.bossBar.setColor(BossBar.Color.PURPLE);
			world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 3.0F, 0.6F);
			world.spawnParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getEyeY(), this.getZ(), 1, 0, 0, 0, 0);
		}
		if (this.age % 10 == 0) {
			world.spawnParticles(this.enraged ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMOKE,
					this.getX(), this.getY() + this.getHeight() * 0.6, this.getZ(), 2, 0.5, 0.8, 0.5, 0.01);
		}
		if (target == null || !target.isAlive()) {
			return;
		}
		double distSq = this.squaredDistanceTo(target);
		if (--this.slamCooldown <= 0 && this.isOnGround() && distSq < 6 * 6) {
			this.slamCooldown = this.enraged ? 60 : 100;
			this.slam(world);
		}
		if (--this.leapCooldown <= 0 && this.isOnGround() && distSq > 7 * 7 && distSq < 20 * 20 && this.canSee(target)) {
			this.leapCooldown = this.enraged ? 80 : 140;
			Vec3d dir = target.getEntityPos().subtract(this.getEntityPos());
			double horizontal = Math.max(0.1, Math.sqrt(dir.x * dir.x + dir.z * dir.z));
			double strength = Math.min(2.0, 0.4 + horizontal * 0.11);
			this.setVelocity(dir.x / horizontal * strength, 0.75, dir.z / horizontal * strength);
			this.velocityDirty = true;
			world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 2.0F, 0.6F);
		}
		if (--this.summonCooldown <= 0 && target instanceof PlayerEntity) {
			this.summonCooldown = this.enraged ? 240 : 400;
			world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_GHAST_SCREAM, SoundCategory.HOSTILE, 3.0F, 0.5F);
			for (int i = 0; i < (this.enraged ? 4 : 2); i++) {
				ZombieEntity runner = ModEntities.RUNNER.create(world, SpawnReason.REINFORCEMENT);
				if (runner == null) {
					continue;
				}
				double x = this.getX() + MathHelper.nextDouble(this.random, -4, 4);
				double z = this.getZ() + MathHelper.nextDouble(this.random, -4, 4);
				runner.refreshPositionAndAngles(x, this.getY(), z, this.random.nextFloat() * 360F, 0);
				if (world.isSpaceEmpty(runner)) {
					runner.initialize(world, world.getLocalDifficulty(BlockPos.ofFloored(x, this.getY(), z)), SpawnReason.REINFORCEMENT, null);
					runner.setTarget(target);
					world.spawnEntity(runner);
					world.spawnParticles(ParticleTypes.LARGE_SMOKE, x, this.getY() + 0.5, z, 12, 0.3, 0.5, 0.3, 0.01);
				}
			}
		}
	}

	private void slam(ServerWorld world) {
		BlockState ground = world.getBlockState(this.getBlockPos().down());
		if (!ground.isAir()) {
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, ground), this.getX(), this.getY() + 0.1, this.getZ(), 120, 3.0, 0.1, 3.0, 0.3);
		}
		world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 0.5, this.getZ(), 1, 0, 0, 0, 0);
		world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 1.5F, 0.4F);
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(6, 2, 6),
				e -> e.isAlive() && !(e instanceof ZombieEntity) && !(e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator())))) {
			Vec3d push = e.getEntityPos().subtract(this.getEntityPos()).multiply(1, 0, 1);
			push = push.lengthSquared() < 1.0E-4 ? Vec3d.ZERO : push.normalize().multiply(1.1);
			if (e.damage(world, this.getDamageSources().mobAttack(this), 10.0F)) {
				e.addVelocity(push.x, 1.1, push.z);
				e.knockedBack = true;
			}
		}
	}

	@Override
	public void onDeath(DamageSource damageSource) {
		super.onDeath(damageSource);
		this.bossBar.clearPlayers();
		if (this.getEntityWorld() instanceof ServerWorld world) {
			world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1, this.getZ(), 3, 1, 1, 1, 0);
			world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 2.0F, 0.8F);
		}
	}
}
