package com.kaan.deadzone.item;

import com.kaan.deadzone.infection.InfectionManager;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.function.Consumer;

/** Verband, Medikit und Immunserum. */
public class HealItem extends Item {
	public enum Kind {
		BANDAGE, MEDKIT, SERUM
	}

	private final Kind kind;

	public HealItem(Kind kind, Item.Settings settings) {
		super(settings);
		this.kind = kind;
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (!world.isClient()) {
			switch (this.kind) {
				case BANDAGE -> {
					user.heal(6.0F);
					user.removeStatusEffect(StatusEffects.POISON);
				}
				case MEDKIT -> {
					user.heal(user.getMaxHealth());
					user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1));
					user.removeStatusEffect(StatusEffects.POISON);
					user.removeStatusEffect(StatusEffects.WEAKNESS);
				}
				case SERUM -> {
					if (user instanceof ServerPlayerEntity player) {
						InfectionManager.makeImmune(player);
					}
				}
			}
		}
		return super.finishUsing(stack, world, user);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
		tooltip.accept(Text.translatable("tooltip.deadzone." + this.kind.name().toLowerCase(java.util.Locale.ROOT)).formatted(Formatting.GREEN));
	}
}
