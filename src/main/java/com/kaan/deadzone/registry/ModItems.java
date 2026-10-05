package com.kaan.deadzone.registry;

import com.kaan.deadzone.DeadZone;
import com.kaan.deadzone.entity.ThrownWeaponEntity;
import com.kaan.deadzone.item.AntidoteItem;
import com.kaan.deadzone.item.HealItem;
import com.kaan.deadzone.item.ThrowableWeaponItem;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvents;
import java.util.EnumMap;
import java.util.Map;
import com.kaan.deadzone.item.GunItem;
import com.kaan.deadzone.item.GunSound;
import com.kaan.deadzone.item.GunStats;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponents;
import net.minecraft.component.type.UseEffectsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Rarity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.kaan.deadzone.item.AmmoType.EXPLOSIVE;
import static com.kaan.deadzone.item.AmmoType.PISTOL;
import static com.kaan.deadzone.item.AmmoType.RIFLE;
import static com.kaan.deadzone.item.AmmoType.SHELL;
import static com.kaan.deadzone.item.AmmoType.SNIPER;

public class ModItems {
	public static final List<Item> GUNS = new ArrayList<>();
	public static final List<Item> KNIVES = new ArrayList<>();
	public static final List<Item> SPAWN_EGGS = new ArrayList<>();

	// ---------------------------------------------------------------- Munition
	public static final Item AMMO_9MM = register("ammo_9mm", Item::new, new Item.Settings());
	public static final Item AMMO_556 = register("ammo_556", Item::new, new Item.Settings());
	public static final Item SHOTGUN_SHELL = register("shotgun_shell", Item::new, new Item.Settings());
	public static final Item AMMO_50CAL = register("ammo_50cal", Item::new, new Item.Settings().rarity(Rarity.UNCOMMON));
	public static final Item EXPLOSIVE_ROUND = register("explosive_round", Item::new, new Item.Settings().maxCount(16).rarity(Rarity.RARE));

	// ---------------------------------------------------------------- 20 Waffen
	// Pistolen
	public static final Item PISTOL_M9 = gun("pistol_m9", Rarity.COMMON, GunStats.of(PISTOL, 5, GunSound.PISTOL).delay(5).mag(15, 30).spread(1.5F).range(48));
	public static final Item REVOLVER = gun("revolver", Rarity.COMMON, GunStats.of(PISTOL, 9, GunSound.HEAVY_PISTOL).delay(14).mag(6, 50).spread(0.8F).range(56));
	public static final Item DESERT_EAGLE = gun("desert_eagle", Rarity.UNCOMMON, GunStats.of(PISTOL, 11, GunSound.HEAVY_PISTOL).delay(12).mag(7, 40).spread(1.2F).range(56));
	// Maschinenpistolen
	public static final Item UZI = gun("uzi", Rarity.COMMON, GunStats.of(PISTOL, 3.5F, GunSound.SMG).delay(2).mag(25, 30).spread(4).range(36).auto());
	public static final Item MP5 = gun("mp5", Rarity.UNCOMMON, GunStats.of(PISTOL, 4, GunSound.SMG).delay(2).mag(30, 35).spread(2.5F).range(44).auto());
	public static final Item P90 = gun("p90", Rarity.RARE, GunStats.of(PISTOL, 3.5F, GunSound.SMG).delay(1).mag(50, 45).spread(3).range(40).auto());
	// Sturmgewehre
	public static final Item AK47 = gun("ak47", Rarity.UNCOMMON, GunStats.of(RIFLE, 7, GunSound.RIFLE).delay(3).mag(30, 45).spread(2.5F).range(64).auto());
	public static final Item M4A1 = gun("m4a1", Rarity.UNCOMMON, GunStats.of(RIFLE, 6, GunSound.RIFLE).delay(2).mag(30, 40).spread(1.8F).range(64).auto());
	public static final Item SCAR = gun("scar", Rarity.RARE, GunStats.of(RIFLE, 8, GunSound.RIFLE).delay(3).mag(20, 40).spread(1.5F).range(72).auto());
	public static final Item DMR = gun("dmr", Rarity.RARE, GunStats.of(RIFLE, 12, GunSound.RIFLE).delay(8).mag(10, 45).spread(0.3F).range(96).pierce(1));
	// Schrotflinten
	public static final Item PUMP_SHOTGUN = gun("pump_shotgun", Rarity.COMMON, GunStats.of(SHELL, 3.5F, GunSound.SHOTGUN).delay(18).mag(7, 50).spread(7).pellets(8).range(24));
	public static final Item DOUBLE_BARREL = gun("double_barrel", Rarity.COMMON, GunStats.of(SHELL, 4, GunSound.SHOTGUN).delay(8).mag(2, 40).spread(10).pellets(10).range(20));
	public static final Item AUTO_SHOTGUN = gun("auto_shotgun", Rarity.EPIC, GunStats.of(SHELL, 2.5F, GunSound.SHOTGUN).delay(5).mag(20, 60).spread(8).pellets(7).range(20).auto());
	// Scharfschützengewehre
	public static final Item HUNTING_RIFLE = gun("hunting_rifle", Rarity.UNCOMMON, GunStats.of(RIFLE, 16, GunSound.SNIPER).delay(25).mag(5, 50).spread(0.2F).range(120).pierce(1));
	public static final Item SNIPER_RIFLE = gun("sniper_rifle", Rarity.RARE, GunStats.of(SNIPER, 28, GunSound.SNIPER).delay(35).mag(5, 70).spread(0).range(160).pierce(2));
	public static final Item BARRETT = gun("barrett", Rarity.EPIC, GunStats.of(SNIPER, 40, GunSound.SNIPER).delay(25).mag(10, 80).spread(0.1F).range(200).pierce(4));
	// Schwere Waffen
	public static final Item M249 = gun("m249", Rarity.RARE, GunStats.of(RIFLE, 6, GunSound.LMG).delay(2).mag(100, 100).spread(4).range(64).auto());
	public static final Item MINIGUN = gun("minigun", Rarity.EPIC, GunStats.of(RIFLE, 5, GunSound.LMG).delay(1).mag(200, 120).spread(6).range(56).auto());
	public static final Item GRENADE_LAUNCHER = gun("grenade_launcher", Rarity.EPIC, GunStats.of(EXPLOSIVE, 0, GunSound.LAUNCHER).delay(20).mag(6, 60).spread(1).range(64).explosion(2.5F, false));
	public static final Item RPG = gun("rpg", Rarity.EPIC, GunStats.of(EXPLOSIVE, 0, GunSound.ROCKET).delay(20).mag(1, 60).spread(0.5F).range(120).explosion(4.0F, true));

	// ---------------------------------------------------------------- Messer
	public static final Item KITCHEN_KNIFE = knife("kitchen_knife", ToolMaterial.IRON, 1.0F, -1.4F);
	public static final Item COMBAT_KNIFE = knife("combat_knife", ToolMaterial.IRON, 2.0F, -1.2F);
	public static final Item MACHETE = knife("machete", ToolMaterial.IRON, 3.5F, -2.2F);
	public static final Item KARAMBIT = knife("karambit", ToolMaterial.DIAMOND, 1.5F, -1.0F);

	// ---------------------------------------------------------------- Gegenmittel
	public static final Item ANTIDOTE = register("antidote", AntidoteItem::new, new Item.Settings()
			.maxCount(16)
			.rarity(Rarity.RARE)
			.component(DataComponentTypes.CONSUMABLE, ConsumableComponents.drink().build())
			.useRemainder(Items.GLASS_BOTTLE));

	// ---------------------------------------------------------------- Wurfwaffen, Heilung
	public static final Item GRENADE = register("grenade",
			settings -> new ThrowableWeaponItem(ThrownWeaponEntity.Grenade::new, settings), new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON));
	public static final Item MOLOTOV = register("molotov",
			settings -> new ThrowableWeaponItem(ThrownWeaponEntity.Molotov::new, settings), new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON));
	public static final Item BANDAGE = register("bandage", settings -> new HealItem(HealItem.Kind.BANDAGE, settings), new Item.Settings()
			.maxCount(16)
			.component(DataComponentTypes.CONSUMABLE, ConsumableComponent.builder().consumeSeconds(1.2F).useAction(UseAction.BRUSH)
					.sound(SoundEvents.ITEM_ARMOR_EQUIP_LEATHER).consumeParticles(false).build()));
	public static final Item MEDKIT = register("medkit", settings -> new HealItem(HealItem.Kind.MEDKIT, settings), new Item.Settings()
			.maxCount(4)
			.rarity(Rarity.UNCOMMON)
			.component(DataComponentTypes.CONSUMABLE, ConsumableComponent.builder().consumeSeconds(3.0F).useAction(UseAction.BRUSH)
					.sound(SoundEvents.ITEM_ARMOR_EQUIP_LEATHER).consumeParticles(false).build()));
	public static final Item IMMUNE_SERUM = register("immune_serum", settings -> new HealItem(HealItem.Kind.SERUM, settings), new Item.Settings()
			.maxCount(1)
			.rarity(Rarity.EPIC)
			.component(DataComponentTypes.CONSUMABLE, ConsumableComponents.drink().build())
			.component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
			.useRemainder(Items.GLASS_BOTTLE));

	// ---------------------------------------------------------------- Ausrüstung
	public static final TagKey<Item> REPAIRS_MILITARY = TagKey.of(RegistryKeys.ITEM, DeadZone.id("repairs_military"));

	private static ArmorMaterial material(String asset, int durability, int helmet, int chest, int legs, int boots, float toughness) {
		Map<EquipmentType, Integer> defense = new EnumMap<>(EquipmentType.class);
		defense.put(EquipmentType.HELMET, helmet);
		defense.put(EquipmentType.CHESTPLATE, chest);
		defense.put(EquipmentType.LEGGINGS, legs);
		defense.put(EquipmentType.BOOTS, boots);
		return new ArmorMaterial(durability, defense, 10, SoundEvents.ITEM_ARMOR_EQUIP_IRON, toughness, 0.05F, REPAIRS_MILITARY,
				RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, DeadZone.id(asset)));
	}

	public static final ArmorMaterial MILITARY = material("military", 30, 3, 7, 5, 3, 1.5F);
	public static final ArmorMaterial GAS_MASK_MATERIAL = material("gas_mask", 20, 1, 1, 1, 1, 0F);
	public static final ArmorMaterial NV_GOGGLES_MATERIAL = material("nv_goggles", 20, 1, 1, 1, 1, 0F);

	public static final Item MILITARY_HELMET = register("military_helmet", Item::new, new Item.Settings().armor(MILITARY, EquipmentType.HELMET).rarity(Rarity.UNCOMMON));
	public static final Item MILITARY_VEST = register("military_vest", Item::new, new Item.Settings().armor(MILITARY, EquipmentType.CHESTPLATE).rarity(Rarity.UNCOMMON));
	public static final Item MILITARY_PANTS = register("military_pants", Item::new, new Item.Settings().armor(MILITARY, EquipmentType.LEGGINGS).rarity(Rarity.UNCOMMON));
	public static final Item MILITARY_BOOTS = register("military_boots", Item::new, new Item.Settings().armor(MILITARY, EquipmentType.BOOTS).rarity(Rarity.UNCOMMON));
	public static final Item GAS_MASK = register("gas_mask", Item::new, new Item.Settings().armor(GAS_MASK_MATERIAL, EquipmentType.HELMET).rarity(Rarity.RARE));
	public static final Item NV_GOGGLES = register("nv_goggles", Item::new, new Item.Settings().armor(NV_GOGGLES_MATERIAL, EquipmentType.HELMET).rarity(Rarity.RARE));

	static {
		for (ModEntities.Kind kind : ModEntities.ALL) {
			SPAWN_EGGS.add(register(kind.name() + "_spawn_egg", SpawnEggItem::new, new Item.Settings().spawnEgg(kind.type())));
		}
	}

	private static Item gun(String name, Rarity rarity, GunStats.Builder stats) {
		GunStats built = stats.build();
		Item item = register(name, settings -> new GunItem(built, settings), new Item.Settings()
				.maxCount(1)
				.rarity(rarity)
				.component(ModComponents.MAGAZINE, built.magazine())
				.component(DataComponentTypes.USE_EFFECTS, new UseEffectsComponent(true, false, 1.0F)));
		GUNS.add(item);
		return item;
	}

	private static Item knife(String name, ToolMaterial material, float damage, float speed) {
		Item item = register(name, Item::new, new Item.Settings().sword(material, damage, speed));
		KNIVES.add(item);
		return item;
	}

	private static Item register(String path, Function<Item.Settings, Item> factory, Item.Settings settings) {
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, DeadZone.id(path));
		return Items.register(key, factory, settings);
	}

	public static void register() {
	}
}
