package com.kaan.deadzone.registry;

import com.kaan.deadzone.DeadZone;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

public class ModDamageTypes {
	public static final RegistryKey<DamageType> BULLET = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, DeadZone.id("bullet"));
	public static final RegistryKey<DamageType> INFECTION = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, DeadZone.id("infection"));

	public static DamageSource of(World world, RegistryKey<DamageType> key, @Nullable Entity attacker) {
		return new DamageSource(world.getRegistryManager().getOrThrow(RegistryKeys.DAMAGE_TYPE).getOrThrow(key), attacker);
	}
}
