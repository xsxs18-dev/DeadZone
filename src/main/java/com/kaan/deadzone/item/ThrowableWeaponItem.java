package com.kaan.deadzone.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Granate und Molotow: Rechtsklick wirft. */
public class ThrowableWeaponItem extends Item {
	private final ProjectileEntity.ProjectileCreator<? extends ProjectileEntity> creator;

	public ThrowableWeaponItem(ProjectileEntity.ProjectileCreator<? extends ProjectileEntity> creator, Item.Settings settings) {
		super(settings);
		this.creator = creator;
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 0.8F, 0.5F);
		if (world instanceof ServerWorld serverWorld) {
			ProjectileEntity.spawnWithVelocity(this.creator, serverWorld, stack, user, 0.0F, 1.2F, 1.0F);
		}
		user.getItemCooldownManager().set(stack, 20);
		stack.decrementUnlessCreative(1, user);
		return ActionResult.SUCCESS;
	}
}
