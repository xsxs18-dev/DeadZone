package com.kaan.deadzone;

import com.kaan.deadzone.event.BloodMoon;
import com.kaan.deadzone.infection.InfectionManager;
import com.kaan.deadzone.item.Gear;
import com.kaan.deadzone.registry.ModComponents;
import com.kaan.deadzone.registry.ModEffects;
import com.kaan.deadzone.registry.ModEntities;
import com.kaan.deadzone.registry.ModItemGroup;
import com.kaan.deadzone.registry.ModItems;
import com.kaan.deadzone.world.ModStructures;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeadZone implements ModInitializer {
	public static final String MOD_ID = "deadzone";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModComponents.register();
		ModEffects.register();
		ModEntities.register();
		ModItems.register();
		ModItemGroup.register();
		ModStructures.register();
		InfectionManager.register();
		Gear.register();
		BloodMoon.register();
		LOGGER.info("DeadZone geladen – überlebe die Nacht.");
	}
}
