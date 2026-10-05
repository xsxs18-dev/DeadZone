package com.kaan.deadzone.world;

import com.kaan.deadzone.DeadZone;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.world.gen.structure.StructureType;

public class ModStructures {
	public static final StructureType<RuinStructure> RUIN = Registry.register(Registries.STRUCTURE_TYPE, DeadZone.id("ruin"), () -> RuinStructure.CODEC);

	public static final StructurePieceType BUILDING = Registry.register(
			Registries.STRUCTURE_PIECE, DeadZone.id("building"), (StructurePieceType) (context, nbt) -> new BuildingPiece(nbt)
	);

	public static final RegistryKey<LootTable> LOOT_HOUSE = loot("house");
	public static final RegistryKey<LootTable> LOOT_CITY = loot("city");
	public static final RegistryKey<LootTable> LOOT_BUNKER = loot("bunker");
	public static final RegistryKey<LootTable> LOOT_MILITARY = loot("military");
	public static final RegistryKey<LootTable> LOOT_TOWER = loot("tower");
	public static final RegistryKey<LootTable> LOOT_HOSPITAL = loot("hospital");
	public static final RegistryKey<LootTable> LOOT_POLICE = loot("police");
	public static final RegistryKey<LootTable> LOOT_SUPERMARKET = loot("supermarket");
	public static final RegistryKey<LootTable> LOOT_GAS = loot("gas_station");
	public static final RegistryKey<LootTable> LOOT_SECRET = loot("secret_vault");
	public static final RegistryKey<LootTable> LOOT_SECRET_LAB = loot("secret_lab");

	private static RegistryKey<LootTable> loot(String name) {
		return RegistryKey.of(RegistryKeys.LOOT_TABLE, DeadZone.id("chests/" + name));
	}

	public static void register() {
	}
}
