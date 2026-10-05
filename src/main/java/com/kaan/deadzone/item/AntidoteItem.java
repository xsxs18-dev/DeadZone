package com.kaan.deadzone.item;

import com.kaan.deadzone.infection.InfectionManager;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.function.Consumer;

/** Die grüne Flasche: heilt die Zombie-Infektion und setzt den Treffer-Zähler zurück. */
public class AntidoteItem extends Item {
	public AntidoteItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (!world.isClient() && user instanceof ServerPlayerEntity player) {
			InfectionManager.cure(player);
		}
		return super.finishUsing(stack, world, user);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
		tooltip.accept(Text.translatable("tooltip.deadzone.antidote").formatted(Formatting.GREEN));
	}
}
