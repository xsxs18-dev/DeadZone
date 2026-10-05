package com.kaan.deadzone.infection;

import com.kaan.deadzone.DeadZone;
import com.kaan.deadzone.registry.ModDamageTypes;
import com.kaan.deadzone.registry.ModEffects;
import com.kaan.deadzone.registry.ModEntities;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Zombie-Treffer geben 10 s Schwäche. Ab dem 18. Treffer hat jeder Treffer 12 % Chance zu infizieren.
 * Nach 20 Minuten Infektion verwandelt sich der Spieler in einen Zombie und stirbt.
 * Der Zustand liegt in einem Attachment am Spieler (bleibt über Neustarts erhalten, wird beim Tod zurückgesetzt).
 */
public final class InfectionManager {
	public static final int HIT_THRESHOLD = 18;
	public static final float INFECTION_CHANCE = 0.12F;
	public static final int INFECTION_TICKS = 20 * 60 * 20;
	public static final int WEAKNESS_TICKS = 20 * 10;

	public record InfectionData(int hits, int ticksLeft) {
		public static final InfectionData NONE = new InfectionData(0, -1);
		public static final Codec<InfectionData> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("hits").forGetter(InfectionData::hits),
				Codec.INT.fieldOf("ticks_left").forGetter(InfectionData::ticksLeft)
		).apply(i, InfectionData::new));

		public boolean infected() {
			return this.ticksLeft >= 0;
		}
	}

	public static final AttachmentType<InfectionData> DATA = AttachmentRegistry.createPersistent(DeadZone.id("infection"), InfectionData.CODEC);

	/** Gesetzt durch das Immunserum vom Boss. Bleibt auch nach dem Tod. */
	public static final AttachmentType<Boolean> IMMUNE = AttachmentRegistry.create(DeadZone.id("immune"),
			builder -> builder.persistent(Codec.BOOL).copyOnDeath());

	private InfectionManager() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (entity instanceof ServerPlayerEntity player && !blocked && damageTaken > 0
					&& source.getAttacker() instanceof ZombieEntity && !player.isCreative() && !player.isSpectator()) {
				onZombieHit(player);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 20 == 0) {
				for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
					tick(player);
				}
			}
		});
	}

	public static InfectionData get(ServerPlayerEntity player) {
		return player.getAttachedOrElse(DATA, InfectionData.NONE);
	}

	private static void onZombieHit(ServerPlayerEntity player) {
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, WEAKNESS_TICKS, 0));
		InfectionData data = get(player);
		if (data.infected() || player.getAttachedOrElse(IMMUNE, false)) {
			return;
		}
		int hits = data.hits() + 1;
		boolean infect = hits >= HIT_THRESHOLD && player.getRandom().nextFloat() < INFECTION_CHANCE;
		player.setAttached(DATA, new InfectionData(hits, infect ? INFECTION_TICKS : -1));
		if (infect) {
			player.addStatusEffect(new StatusEffectInstance(ModEffects.INFECTED, INFECTION_TICKS, 0, false, false, true));
			player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 60, 20));
			player.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("title.deadzone.infected").formatted(Formatting.DARK_GREEN, Formatting.BOLD)));
			player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.translatable("title.deadzone.infected.sub").formatted(Formatting.GREEN)));
			player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ZOMBIE_INFECT, SoundCategory.PLAYERS, 1.0F, 0.8F);
		} else if (hits >= HIT_THRESHOLD) {
			player.sendMessage(Text.translatable("hud.deadzone.risk", Math.round(INFECTION_CHANCE * 100)).formatted(Formatting.GOLD), true);
		} else {
			player.sendMessage(Text.translatable("hud.deadzone.hits", hits, HIT_THRESHOLD).formatted(Formatting.GRAY), true);
		}
	}

	private static void tick(ServerPlayerEntity player) {
		InfectionData data = get(player);
		if (!data.infected() || !player.isAlive()) {
			return;
		}
		int left = data.ticksLeft() - 20;
		if (left <= 0) {
			turnIntoZombie(player);
			return;
		}
		player.setAttached(DATA, new InfectionData(data.hits(), left));
		// Anzeige immer wieder setzen: Milch entfernt den Effekt, aber nicht die Infektion.
		player.addStatusEffect(new StatusEffectInstance(ModEffects.INFECTED, left, 0, false, false, true));
		if (left <= 20 * 60) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0, false, false));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, false, false));
		} else if (left <= 20 * 60 * 5) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 40, 0, false, false));
		}
		if (left % (20 * 60) == 0) {
			player.sendMessage(Text.translatable("chat.deadzone.infection_warning", left / (20 * 60)).formatted(Formatting.GREEN), false);
		}
	}

	private static void turnIntoZombie(ServerPlayerEntity player) {
		ServerWorld world = player.getEntityWorld();
		player.removeAttached(DATA);
		player.removeStatusEffect(ModEffects.INFECTED);

		ZombieEntity zombie = ModEntities.WALKER.create(world, SpawnReason.CONVERSION);
		if (zombie != null) {
			zombie.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0);
			zombie.initialize(world, world.getLocalDifficulty(player.getBlockPos()), SpawnReason.CONVERSION, null);
			zombie.setCustomName(player.getName().copy().formatted(Formatting.DARK_GREEN));
			zombie.setCustomNameVisible(true);
			ItemStack head = new ItemStack(Items.PLAYER_HEAD);
			head.set(DataComponentTypes.PROFILE, ProfileComponent.ofStatic(player.getGameProfile()));
			zombie.equipStack(EquipmentSlot.HEAD, head);
			zombie.setEquipmentDropChance(EquipmentSlot.HEAD, 1.0F);
			zombie.setPersistent();
			world.spawnEntity(zombie);
		}
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
		world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.PLAYERS, 1.5F, 0.7F);
		player.damage(world, ModDamageTypes.of(world, ModDamageTypes.INFECTION, null), Float.MAX_VALUE);
	}

	public static void makeImmune(ServerPlayerEntity player) {
		cure(player);
		player.setAttached(IMMUNE, true);
		player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 60, 20));
		player.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("title.deadzone.immune").formatted(Formatting.AQUA, Formatting.BOLD)));
		player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.translatable("title.deadzone.immune.sub").formatted(Formatting.WHITE)));
		player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0F, 1.0F);
	}

	public static void cure(ServerPlayerEntity player) {
		boolean wasInfected = get(player).infected();
		player.removeAttached(DATA);
		player.removeStatusEffect(ModEffects.INFECTED);
		player.removeStatusEffect(StatusEffects.WEAKNESS);
		player.removeStatusEffect(StatusEffects.NAUSEA);
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0));
		player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.PLAYERS, 0.6F, 1.4F);
		player.sendMessage(Text.translatable(wasInfected ? "hud.deadzone.cured" : "hud.deadzone.cured_hits").formatted(Formatting.GREEN), true);
	}
}
