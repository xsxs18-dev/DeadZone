package com.kaan.deadzone.entity;

import com.kaan.deadzone.item.Gear;
import com.kaan.deadzone.registry.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/** Die 10 Zombie-Arten. Jede Fähigkeit läuft mit Cooldowns, damit auch viele Zombies kaum Leistung kosten. */
public final class Zombies {
	private Zombies() {
	}

	private static DefaultAttributeContainer.Builder base(double health, double speed, double damage, double armor) {
		return ZombieEntity.createZombieAttributes()
				.add(EntityAttributes.MAX_HEALTH, health)
				.add(EntityAttributes.MOVEMENT_SPEED, speed)
				.add(EntityAttributes.ATTACK_DAMAGE, damage)
				.add(EntityAttributes.ARMOR, armor);
	}

	private static boolean isValidVictim(LivingEntity e) {
		return e.isAlive() && !(e instanceof ZombieEntity) && !(e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator()));
	}

	// ---------------------------------------------------------------- 1. Streuner
	/** Standard-Zombie. Schwach, kommt aber in großen Gruppen und bricht Türen auf. */
	public static class Walker extends DzZombie {
		public Walker(EntityType<? extends ZombieEntity> type, World world) {
			super(type, world);
		}

		public static DefaultAttributeContainer.Builder attributes() {
			return base(20, 0.23, 3, 2);
		}

		@Override
		protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
			this.setCanBreakDoors(true);
		}
	}

	// ---------------------------------------------------------------- 2. Sprinter
	/** Sehr schnell, wenig Leben, sieht dich von weit weg. */
	public static class Runner extends DzZombie {
		public Runner(EntityType<? extends ZombieEntity> type, World world) {
			super(type, world);
		}

		public static DefaultAttributeContainer.Builder attributes() {
			return base(14, 0.34, 2.5, 0).add(EntityAttributes.FOLLOW_RANGE, 48);
		}

		@Override
		protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		}

		@Override
		protected void specialTick(ServerWorld world) {
			if (this.getTarget() != null && this.age % 4 == 0 && this.isOnGround() && this.getVelocity().horizontalLengthSquared() > 0.01) {
				world.spawnParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.1, this.getZ(), 1, 0.1, 0.0, 0.1, 0.0);
			}
		}
	}

	// ---------------------------------------------------------------- 3. Kriecher
	/** Halb so groß, schwer zu treffen, beißt in die Beine (Langsamkeit). */
	public static class Crawler extends DzZombie {
		public Crawler(EntityType<? extends ZombieEntity> type, World world) {
			super(type, world);
		}

		public static DefaultAttributeContainer.Builder attributes() {
			return base(12, 0.27, 3, 0).add(EntityAttributes.SCALE, 0.55);
		}

		@Override
		protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		}

		@Override
		public boolean tryAttack(ServerWorld world, Entity target) {
			boolean hit = super.tryAttack(world, target);
			if (hit && target instanceof LivingEntity living) {
				living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), this);
			}
			return hit;
		}
	}

	// ---------------------------------------------------------------- 4. Platzer
	/** Aufgebläht und langsam. Explodiert neben dir und hinterlässt eine Giftwolke. */
	public static class Bloater extends DzZombie {
		private int fuse = -1;

		public Bloater(EntityType<? extends ZombieEntity> type, World world) {
			super(type, world);
		}

		public static DefaultAttributeContainer.Builder attributes() {
			return base(26, 0.17, 4, 0).add(EntityAttributes.SCALE, 1.3);
		}

		@Override
		protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		}

		@Override
		protected void specialTick(ServerWorld world) {
			LivingEntity target = this.getTarget();
			if (this.fuse >= 0) {
				this.fuse++;
				this.getNavigation().stop();
				world.spawnParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.4, this.getZ(), 2, 0.3, 0.3, 0.3, 0.02);
				if (this.fuse >= 30) {
					this.explode(world);
				}
			} else if (target != null && this.squaredDistanceTo(target) < 2.4 * 2.4) {
				this.fuse = 0;
				this.playSound(SoundEvents.ENTITY_CREEPER_PRIMED, 1.0F, 0.6F);
			}
		}

		private void explode(ServerWorld world) {
			this.dead = true;
			world.createExplosion(this, this.getX(), this.getY() + 1.0, this.getZ(), 2.2F, World.ExplosionSourceType.NONE);
			this.spawnGasCloud(world);
			this.discard();
		}

		private void spawnGasCloud(ServerWorld world) {
			AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(world, this.getX(), this.getY(), this.getZ());
			cloud.setRadius(3.0F);
			cloud.setRadiusOnUse(-0.3F);
			cloud.setWaitTime(5);
			cloud.setDuration(120);
			cloud.setRadiusGrowth(-cloud.getRadius() / cloud.getDuration());
			cloud.addEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 0));
			world.spawnEntity(cloud);
		}

		@Override
		public void onDeath(DamageSource damageSource) {
			super.onDeath(damageSource);
			if (this.getEntityWorld() instanceof ServerWorld world) {
				this.spawnGasCloud(world);
			}
		}
	}

	// ---------------------------------------------------------------- 5. Spucker
	/** Spuckt aus bis zu 16 Blöcken Säure, die vergiftet. */
	public static class Spitter extends DzZombie {
		private int spitCooldown = 40;

		public Spitter(EntityType<? extends ZombieEntity> type, World world) {
			super(type, world);
		}

		public static DefaultAttributeContainer.Builder attributes() {
			return base(18, 0.24, 2, 0);
		}

		@Override
		protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		}

		@Override
		protected void specialTick(ServerWorld world) {
			if (--this.spitCooldown > 0) {
				return;
			}
			LivingEntity target = this.getTarget();
			if (target == null || !isValidVictim(target)) {
				return;
			}
			double distSq = this.squaredDistanceTo(target);
			if (distSq < 3.5 * 3.5 || distSq > 16 * 16 || !this.canSee(target)) {
				return;
			}
			this.spitCooldown = 60;
			this.getLookControl().lookAt(target, 30.0F, 30.0F);
			Vec3d from = this.getEyePos();
			Vec3d to = target.getEyePos().add(0, -0.3, 0);
			this.playSound(SoundEvents.ENTITY_LLAMA_SPIT, 1.2F, 0.6F);
			Vec3d delta = to.subtract(from);
			int steps = (int) (delta.length() / 0.6);
			for (int i = 1; i <= steps; i++) {
				double t = (double) i / steps;
				Vec3d p = from.add(delta.multiply(t)).add(0, Math.sin(t * Math.PI) * 0.6, 0);
				world.spawnParticles(ParticleTypes.ITEM_SLIME, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
			}
			if (world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() == HitResult.Type.MISS) {
				if (target.damage(world, this.getDamageSources().mobAttack(this), 3.0F) && !Gear.hasGasMask(target)) {
					target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60, 0), this);
				}
			}
		}
	}

	// ---------------------------------------------------------------- 6. Koloss
	/** Riesiger Tank-Zombie. Stampft auf den Boden und schleudert alles in die Luft. */
	public static class Brute extends DzZombie {
		private int slamCooldown = 60;

		public Brute(EntityType<? extends ZombieEntity> type, World world) {
			super(type, world);
			this.experiencePoints = 25;
		}

		public static DefaultAttributeContainer.Builder attributes() {
			return base(80, 0.21, 9, 8)
					.add(EntityAttributes.SCALE, 1.7)
					.add(EntityAttributes.KNOCKBACK_RESISTANCE, 1.0)
					.add(EntityAttributes.ATTACK_KNOCKBACK, 1.5)
					.add(EntityAttributes.STEP_HEIGHT, 1.0);
		}

		@Override
		protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		}

		@Override
		protected void specialTick(ServerWorld world) {
			if (--this.slamCooldown > 0) {
				return;
			}
			LivingEntity target = this.getTarget();
			if (target == null || !this.isOnGround() || this.squaredDistanceTo(target) > 4.5 * 4.5) {
				return;
			}
			this.slamCooldown = 100;
			BlockState ground = world.getBlockState(this.getBlockPos().down());
			if (!ground.isAir()) {
				world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, ground), this.getX(), this.getY() + 0.1, this.getZ(), 60, 2.0, 0.1, 2.0, 0.2);
			}
			world.spawnParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.5, this.getZ(), 3, 1.5, 0.2, 1.5, 0.0);
			world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 0.8F, 0.5F);
			for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(4.5, 1.5, 4.5), Zombies::isValidVictim)) {
				Vec3d push = e.getEntityPos().subtract(this.getEntityPos()).multiply(1, 0, 1);
				push = push.lengthSquared() < 1.0E-4 ? Vec3d.ZERO : push.normalize().multiply(0.7);
				if (e.damage(world, this.getDamageSources().mobAttack(this), 6.0F)) {
					e.addVelocity(push.x, 0.9, push.z);
					e.knockedBack = true;
				}
			}
		}
	}

	// ---------------------------------------------------------------- 7. Schreier
	/** Schreit, macht alle Zombies in der Nähe schneller und stärker und ruft Verstärkung. */
	public static class Screamer extends DzZombie {
		private int screamCooldown = 40;

		public Screamer(EntityType<? extends ZombieEntity> type, World world) {
			super(type, world);
		}

		public static DefaultAttributeContainer.Builder attributes() {
			return base(16, 0.28, 2, 0);
		}

		@Override
		protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		}

		@Override
		protected void specialTick(ServerWorld world) {
			if (--this.screamCooldown > 0) {
				return;
			}
			LivingEntity target = this.getTarget();
			if (!(target instanceof PlayerEntity) || !isValidVictim(target) || this.squaredDistanceTo(target) > 24 * 24 || !this.canSee(target)) {
				return;
			}
			this.screamCooldown = 240;
			world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_GHAST_SCREAM, SoundCategory.HOSTILE, 3.0F, 0.7F);
			world.spawnParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getEyeY(), this.getZ(), 1, 0, 0, 0, 0);
			for (ZombieEntity z : world.getEntitiesByClass(ZombieEntity.class, this.getBoundingBox().expand(32), e -> e != this && e.isAlive())) {
				z.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1));
				z.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 0));
				z.setTarget(target);
			}
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0), this);
			if (world.isNight() && this.random.nextFloat() < 0.35F) {
				for (int i = 0; i < 2; i++) {
					this.summonWalker(world, target);
				}
			}
		}

		private void summonWalker(ServerWorld world, LivingEntity target) {
			ZombieEntity walker = ModEntities.WALKER.create(world, SpawnReason.REINFORCEMENT);
			if (walker == null) {
				return;
			}
			for (int attempt = 0; attempt < 8; attempt++) {
				double x = this.getX() + MathHelper.nextDouble(this.random, -6, 6);
				double z = this.getZ() + MathHelper.nextDouble(this.random, -6, 6);
				for (int dy = 1; dy >= -2; dy--) {
					BlockPos pos = BlockPos.ofFloored(x, this.getY() + dy, z);
					walker.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, this.random.nextFloat() * 360F, 0);
					if (world.getBlockState(pos.down()).isSolidBlock(world, pos.down()) && world.isSpaceEmpty(walker)) {
						walker.initialize(world, world.getLocalDifficulty(pos), SpawnReason.REINFORCEMENT, null);
						walker.setTarget(target);
						world.spawnEntity(walker);
						world.spawnParticles(ParticleTypes.LARGE_SMOKE, walker.getX(), walker.getY() + 0.5, walker.getZ(), 10, 0.3, 0.5, 0.3, 0.01);
						return;
					}
				}
			}
		}
	}

	// ---------------------------------------------------------------- 8. Soldat
	/** Ehemaliger Soldat mit Helm und Weste. Zäh und lässt oft Munition fallen. */
	public static class Soldier extends DzZombie {
		public Soldier(EntityType<? extends ZombieEntity> type, World world) {
			super(type, world);
			this.experiencePoints = 10;
		}

		public static DefaultAttributeContainer.Builder attributes() {
			return base(30, 0.25, 5, 4).add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.5);
		}

		@Override
		protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
			this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			this.setEquipmentDropChance(EquipmentSlot.HEAD, 0.05F);
			if (random.nextFloat() < 0.5F) {
				this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
				this.setEquipmentDropChance(EquipmentSlot.CHEST, 0.05F);
			}
		}
	}

	// ---------------------------------------------------------------- 9. Springer
	/** Springt aus bis zu 12 Blöcken auf dich zu. Nimmt keinen Fallschaden. */
	public static class Leaper extends DzZombie {
		private int leapCooldown = 40;

		public Leaper(EntityType<? extends ZombieEntity> type, World world) {
			super(type, world);
		}

		public static DefaultAttributeContainer.Builder attributes() {
			return base(18, 0.29, 4, 0).add(EntityAttributes.JUMP_STRENGTH, 0.6);
		}

		@Override
		protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		}

		@Override
		public boolean handleFallDamage(double fallDistance, float damagePerDistance, DamageSource damageSource) {
			return false;
		}

		@Override
		protected void specialTick(ServerWorld world) {
			if (--this.leapCooldown > 0) {
				return;
			}
			LivingEntity target = this.getTarget();
			if (target == null || !isValidVictim(target) || !this.isOnGround()) {
				return;
			}
			double distSq = this.squaredDistanceTo(target);
			if (distSq < 3 * 3 || distSq > 12 * 12 || !this.canSee(target)) {
				return;
			}
			this.leapCooldown = 70;
			Vec3d dir = target.getEntityPos().subtract(this.getEntityPos());
			double horizontal = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
			double strength = Math.min(1.6, 0.35 + horizontal * 0.12);
			this.setVelocity(dir.x / horizontal * strength, 0.55 + Math.max(0, dir.y) * 0.08, dir.z / horizontal * strength);
			this.velocityDirty = true;
			this.getLookControl().lookAt(target, 30.0F, 30.0F);
			this.playSound(SoundEvents.ENTITY_ZOMBIE_AMBIENT, 1.5F, 1.7F);
			world.spawnParticles(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 8, 0.3, 0.05, 0.3, 0.02);
		}
	}

	// ---------------------------------------------------------------- 10. Strahlen-Zombie
	/** Verstrahlt. Vergiftet alles in 4 Blöcken Umkreis und ist selbst immun gegen Gift. */
	public static class Toxic extends DzZombie {
		private static final DustParticleEffect GLOW = new DustParticleEffect(0x7CFF3A, 1.0F);

		public Toxic(EntityType<? extends ZombieEntity> type, World world) {
			super(type, world);
			this.experiencePoints = 10;
		}

		public static DefaultAttributeContainer.Builder attributes() {
			return base(28, 0.22, 3, 2);
		}

		@Override
		protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		}

		@Override
		public boolean canHaveStatusEffect(StatusEffectInstance effect) {
			return !effect.getEffectType().equals(StatusEffects.POISON) && super.canHaveStatusEffect(effect);
		}

		@Override
		protected void specialTick(ServerWorld world) {
			if (this.age % 5 == 0) {
				world.spawnParticles(GLOW, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0.4, 0.6, 0.4, 0.0);
			}
			if (this.age % 20 != 0) {
				return;
			}
			for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(4.0), e -> isValidVictim(e) && !Gear.hasGasMask(e))) {
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60, 0), this);
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 60, 1), this);
			}
		}
	}
}
