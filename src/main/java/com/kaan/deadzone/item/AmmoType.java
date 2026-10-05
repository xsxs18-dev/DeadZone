package com.kaan.deadzone.item;

import com.kaan.deadzone.registry.ModItems;
import net.minecraft.item.Item;

import java.util.function.Supplier;

public enum AmmoType {
	PISTOL(() -> ModItems.AMMO_9MM),
	RIFLE(() -> ModItems.AMMO_556),
	SHELL(() -> ModItems.SHOTGUN_SHELL),
	SNIPER(() -> ModItems.AMMO_50CAL),
	EXPLOSIVE(() -> ModItems.EXPLOSIVE_ROUND);

	private final Supplier<Item> item;

	AmmoType(Supplier<Item> item) {
		this.item = item;
	}

	public Item item() {
		return this.item.get();
	}
}
