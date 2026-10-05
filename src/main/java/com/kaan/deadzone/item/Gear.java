package com.kaan.deadzone.item;

import com.kaan.deadzone.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

/** Gasmaske schützt vor Gift und Gas, Nachtsichtbrille gibt Nachtsicht. Wird jede Sekunde geprüft. */
public final class Gear {
	private Gear() {
	}

	public static boolean hasGasMask(LivingEntity entity) {
		return entity.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.GAS_MASK);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 20 != 0) {
				return;
			}
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				if (hasGasMask(player)) {
					player.removeStatusEffect(StatusEffects.POISON);
					player.removeStatusEffect(StatusEffects.HUNGER);
				} else if (player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.NV_GOGGLES)) {
					player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 260, 0, true, false, true));
				}
			}
		});
	}
}
