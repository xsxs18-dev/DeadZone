package com.kaan.deadzone.registry;

import com.kaan.deadzone.DeadZone;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

public class ModItemGroup {
	public static final RegistryKey<ItemGroup> KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, DeadZone.id("deadzone"));

	public static void register() {
		Registry.register(Registries.ITEM_GROUP, KEY, FabricItemGroup.builder()
				.icon(() -> new ItemStack(ModItems.AK47))
				.displayName(Text.translatable("itemGroup.deadzone"))
				.entries((context, entries) -> {
					ModItems.GUNS.forEach(entries::add);
					ModItems.KNIVES.forEach(entries::add);
					entries.add(ModItems.AMMO_9MM);
					entries.add(ModItems.AMMO_556);
					entries.add(ModItems.SHOTGUN_SHELL);
					entries.add(ModItems.AMMO_50CAL);
					entries.add(ModItems.EXPLOSIVE_ROUND);
					entries.add(ModItems.GRENADE);
					entries.add(ModItems.MOLOTOV);
					entries.add(ModItems.ANTIDOTE);
					entries.add(ModItems.BANDAGE);
					entries.add(ModItems.MEDKIT);
					entries.add(ModItems.IMMUNE_SERUM);
					entries.add(ModItems.MILITARY_HELMET);
					entries.add(ModItems.MILITARY_VEST);
					entries.add(ModItems.MILITARY_PANTS);
					entries.add(ModItems.MILITARY_BOOTS);
					entries.add(ModItems.GAS_MASK);
					entries.add(ModItems.NV_GOGGLES);
					ModItems.SPAWN_EGGS.forEach(entries::add);
				})
				.build());
	}
}
