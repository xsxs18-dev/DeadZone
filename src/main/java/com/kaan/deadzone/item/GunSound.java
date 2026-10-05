package com.kaan.deadzone.item;

import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** Schussgeräusche aus Vanilla-Sounds zusammengemischt, damit die Mod keine eigenen Audiodateien braucht. */
public enum GunSound {
	PISTOL(SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 0.9F, 1.7F, 0.25F, 2.0F),
	HEAVY_PISTOL(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0F, 1.2F, 0.45F, 1.6F),
	SMG(SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 0.8F, 1.9F, 0.15F, 2.0F),
	RIFLE(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0F, 1.5F, 0.35F, 1.8F),
	SHOTGUN(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.2F, 0.8F, 0.7F, 1.3F),
	SNIPER(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.4F, 0.7F, 1.0F, 1.1F),
	LMG(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0F, 1.3F, 0.3F, 1.7F),
	LAUNCHER(SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.2F, 0.6F, 0.2F, 1.5F),
	ROCKET(SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5F, 0.4F, 0.4F, 1.2F);

	private final SoundEvent main;
	private final float volume;
	private final float pitch;
	private final float boomVolume;
	private final float boomPitch;

	GunSound(SoundEvent main, float volume, float pitch, float boomVolume, float boomPitch) {
		this.main = main;
		this.volume = volume;
		this.pitch = pitch;
		this.boomVolume = boomVolume;
		this.boomPitch = boomPitch;
	}

	public void play(World world, double x, double y, double z) {
		float jitter = 0.95F + world.random.nextFloat() * 0.1F;
		world.playSound(null, x, y, z, this.main, SoundCategory.PLAYERS, this.volume, this.pitch * jitter);
		world.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, this.boomVolume, this.boomPitch * jitter);
	}
}
