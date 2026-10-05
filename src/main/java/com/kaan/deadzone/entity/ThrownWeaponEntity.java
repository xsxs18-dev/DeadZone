package com.kaan.deadzone.entity;

import com.kaan.deadzone.registry.ModEntities;
import com.kaan.deadzone.registry.ModItems;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Geworfene Granate oder Molotow-Cocktail. Wirkt beim Aufprall. */
public abstract class ThrownWeaponEntity extends ThrownItemEntity {
	protected ThrownWeaponEntity(EntityType<? extends ThrownItemEntity> type, World world) {
		super(type, world);
	}

	protected ThrownWeaponEntity(EntityType<? extends ThrownItemEntity> type, LivingEntity owner, World world, ItemStack stack) {
		super(type, owner, world, stack);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.getEntityWorld() instanceof ServerWorld world && this.age % 2 == 0) {
			world.spawnParticles(this.trailParticle(), this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
		}
	}

	protected abstract net.minecraft.particle.ParticleEffect trailParticle();

	protected abstract void detonate(ServerWorld world);

	@Override
	protected void onCollision(HitResult hitResult) {
		super.onCollision(hitResult);
		if (this.getEntityWorld() instanceof ServerWorld world && !this.isRemoved()) {
			this.detonate(world);
			this.discard();
		}
	}

	/** Splittergranate: Explosion ohne Blockschaden. */
	public static class Grenade extends ThrownWeaponEntity {
		public Grenade(EntityType<? extends Grenade> type, World world) {
			super(type, world);
		}

		public Grenade(World world, LivingEntity owner, ItemStack stack) {
			super(ModEntities.GRENADE, owner, world, stack);
		}

		@Override
		protected Item getDefaultItem() {
			return ModItems.GRENADE;
		}

		@Override
		protected net.minecraft.particle.ParticleEffect trailParticle() {
			return ParticleTypes.SMOKE;
		}

		@Override
		protected void detonate(ServerWorld world) {
			world.createExplosion(this, this.getX(), this.getY(), this.getZ(), 3.0F, World.ExplosionSourceType.NONE);
		}
	}

	/** Molotow-Cocktail: setzt eine Fläche und alles darin in Brand. */
	public static class Molotov extends ThrownWeaponEntity {
		public Molotov(EntityType<? extends Molotov> type, World world) {
			super(type, world);
		}

		public Molotov(World world, LivingEntity owner, ItemStack stack) {
			super(ModEntities.MOLOTOV, owner, world, stack);
		}

		@Override
		protected Item getDefaultItem() {
			return ModItems.MOLOTOV;
		}

		@Override
		protected net.minecraft.particle.ParticleEffect trailParticle() {
			return ParticleTypes.FLAME;
		}

		@Override
		protected void detonate(ServerWorld world) {
			world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.2F, 0.8F);
			world.playSound(null, this.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.2F, 0.7F);
			world.spawnParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.3, this.getZ(), 60, 1.5, 0.4, 1.5, 0.05);
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 20, 1.2, 0.5, 1.2, 0.02);
			BlockPos center = this.getBlockPos();
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					if (dx * dx + dz * dz > 5) {
						continue;
					}
					for (int dy = 1; dy >= -2; dy--) {
						BlockPos pos = center.add(dx, dy, dz);
						if (world.getBlockState(pos).isAir() && world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), net.minecraft.util.math.Direction.UP)) {
							world.setBlockState(pos, AbstractFireBlock.getState(world, pos));
							break;
						}
					}
				}
			}
			for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(3.0), LivingEntity::isAlive)) {
				e.setOnFireFor(8.0F);
			}
		}
	}
}
