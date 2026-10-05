package com.kaan.deadzone.registry;

import com.kaan.deadzone.DeadZone;
import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModComponents {
	/** Patronen im Magazin einer Waffe. Ändert sich bei jedem Schuss, daher ohne Wechsel-Animation. */
	public static final ComponentType<Integer> MAGAZINE = Registry.register(
			Registries.DATA_COMPONENT_TYPE,
			DeadZone.id("magazine"),
			ComponentType.<Integer>builder().codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT).skipsHandAnimation().build()
	);

	public static void register() {
	}
}
