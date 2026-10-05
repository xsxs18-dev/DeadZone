package com.kaan.deadzone.entity;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Basis aller DeadZone-Zombies: brennen nicht, werden nie zu Babys oder Ertrunkenen,
 * und sind tagsüber geschwächt (doppelter Schaden, langsamer).
 */
public abstract class DzZombie extends ZombieEntity {
	public static final float DAYLIGHT_DAMAGE_MULTIPLIER = 2.0F;

	protected DzZombie(EntityType<? extends ZombieEntity> type, World world) {
		super(type, world);
	}

	public static boolean isVulnerable(World world) {
		return world.isDay();
	}

	@Override
	protected boolean burnsInDaylight() {
		return false;
	}

	@Override
	protected boolean canConvertInWater() {
		return false;
	}

	@Override
	public void setBaby(boolean baby) {
		super.setBaby(false);
	}

	@Override
	public @Nullable EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
		EntityData data = super.initialize(world, difficulty, spawnReason, new ZombieEntity.ZombieData(false, false));
		this.setCanPickUpLoot(false);
		return data;
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (isVulnerable(world)) {
			amount *= DAYLIGHT_DAMAGE_MULTIPLIER;
		}
		return super.damage(world, source, amount);
	}

	@Override
	protected void mobTick(ServerWorld world) {
		super.mobTick(world);
		if (this.age % 20 == 0 && isVulnerable(world)) {
			this.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, true, false));
			world.spawnParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + this.getHeight() * 0.8, this.getZ(), 2, 0.2, 0.2, 0.2, 0.01);
		}
		this.specialTick(world);
	}

	/** Spezialfähigkeit, läuft jeden Server-Tick. */
	protected void specialTick(ServerWorld world) {
	}
}
