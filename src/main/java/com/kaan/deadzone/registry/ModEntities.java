package com.kaan.deadzone.registry;

import com.kaan.deadzone.DeadZone;
import com.kaan.deadzone.entity.PatientZero;
import com.kaan.deadzone.entity.ThrownWeaponEntity;
import com.kaan.deadzone.entity.Zombies;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.biome.BiomeKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModEntities {
	public record Kind(String name, EntityType<? extends ZombieEntity> type) {
	}

	public static final List<Kind> ALL = new ArrayList<>();

	private record Spawn(EntityType<? extends ZombieEntity> type, Supplier<DefaultAttributeContainer.Builder> attributes, int weight, int min, int max) {
	}

	private static final List<Spawn> SPAWNS = new ArrayList<>();

	public static final EntityType<Zombies.Walker> WALKER = register("walker", Zombies.Walker::new, Zombies.Walker::attributes, 60, 3, 5);
	public static final EntityType<Zombies.Runner> RUNNER = register("runner", Zombies.Runner::new, Zombies.Runner::attributes, 30, 2, 4);
	public static final EntityType<Zombies.Crawler> CRAWLER = register("crawler", Zombies.Crawler::new, Zombies.Crawler::attributes, 25, 1, 3);
	public static final EntityType<Zombies.Bloater> BLOATER = register("bloater", Zombies.Bloater::new, Zombies.Bloater::attributes, 10, 1, 1);
	public static final EntityType<Zombies.Spitter> SPITTER = register("spitter", Zombies.Spitter::new, Zombies.Spitter::attributes, 15, 1, 2);
	public static final EntityType<Zombies.Brute> BRUTE = register("brute", Zombies.Brute::new, Zombies.Brute::attributes, 4, 1, 1);
	public static final EntityType<Zombies.Screamer> SCREAMER = register("screamer", Zombies.Screamer::new, Zombies.Screamer::attributes, 8, 1, 1);
	public static final EntityType<Zombies.Soldier> SOLDIER = register("soldier", Zombies.Soldier::new, Zombies.Soldier::attributes, 12, 1, 3);
	public static final EntityType<Zombies.Leaper> LEAPER = register("leaper", Zombies.Leaper::new, Zombies.Leaper::attributes, 15, 1, 2);
	public static final EntityType<Zombies.Toxic> TOXIC = register("toxic", Zombies.Toxic::new, Zombies.Toxic::attributes, 10, 1, 2);
	/** Boss im Geheimbunker, spawnt nie natürlich. */
	public static final EntityType<PatientZero> PATIENT_ZERO = register("patient_zero", PatientZero::new, PatientZero::attributes, 0, 1, 1);

	public static final EntityType<ThrownWeaponEntity.Grenade> GRENADE = registerThrown("grenade", ThrownWeaponEntity.Grenade::new);
	public static final EntityType<ThrownWeaponEntity.Molotov> MOLOTOV = registerThrown("molotov", ThrownWeaponEntity.Molotov::new);

	private static <T extends ThrownWeaponEntity> EntityType<T> registerThrown(String name, EntityType.EntityFactory<T> factory) {
		RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, DeadZone.id(name));
		return Registry.register(Registries.ENTITY_TYPE, key, EntityType.Builder.create(factory, SpawnGroup.MISC)
				.dimensions(0.25F, 0.25F).maxTrackingRange(4).trackingTickInterval(10).build(key));
	}

	private static <T extends ZombieEntity> EntityType<T> register(
			String name, EntityType.EntityFactory<T> factory, Supplier<DefaultAttributeContainer.Builder> attributes, int weight, int min, int max
	) {
		RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, DeadZone.id(name));
		EntityType<T> type = Registry.register(
				Registries.ENTITY_TYPE,
				key,
				EntityType.Builder.create(factory, SpawnGroup.MONSTER)
						.dimensions(0.6F, 1.95F)
						.eyeHeight(1.74F)
						.passengerAttachments(2.0125F)
						.vehicleAttachment(-0.7F)
						.maxTrackingRange(8)
						.notAllowedInPeaceful()
						.build(key)
		);
		ALL.add(new Kind(name, type));
		SPAWNS.add(new Spawn(type, attributes, weight, min, max));
		return type;
	}

	/** Natürliches Spawnen nur nachts. Spawner, Eier und Befehle gehen immer. */
	private static boolean canSpawnAtNight(EntityType<? extends HostileEntity> type, ServerWorldAccess world, SpawnReason reason, BlockPos pos, Random random) {
		if (reason == SpawnReason.NATURAL || reason == SpawnReason.CHUNK_GENERATION || reason == SpawnReason.STRUCTURE) {
			if (!world.toServerWorld().isNight()) {
				return false;
			}
		}
		return HostileEntity.canSpawnInDark(type, world, reason, pos, random);
	}

	@SuppressWarnings("unchecked")
	public static void register() {
		for (Spawn spawn : SPAWNS) {
			EntityType<ZombieEntity> type = (EntityType<ZombieEntity>) spawn.type();
			FabricDefaultAttributeRegistry.register(type, spawn.attributes().get());
			SpawnRestriction.register(type, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModEntities::canSpawnAtNight);
			if (spawn.weight() <= 0) {
				continue;
			}
			BiomeModifications.addSpawn(
					BiomeSelectors.foundInOverworld().and(BiomeSelectors.excludeByKey(BiomeKeys.MUSHROOM_FIELDS, BiomeKeys.DEEP_DARK)),
					SpawnGroup.MONSTER, type, spawn.weight(), spawn.min(), spawn.max()
			);
		}
	}
}
