package com.kaan.deadzone.registry;

import com.kaan.deadzone.DeadZone;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;

public class ModEffects {
	/** Nur Anzeige: der echte Infektions-Timer liegt im Spieler-Attachment, damit Milch ihn nicht entfernen kann. */
	public static final RegistryEntry<StatusEffect> INFECTED = Registry.registerReference(
			Registries.STATUS_EFFECT, DeadZone.id("infected"), new InfectedEffect()
	);

	public static void register() {
	}

	private static class InfectedEffect extends StatusEffect {
		InfectedEffect() {
			super(StatusEffectCategory.HARMFUL, 0x5BC236);
		}
	}
}
