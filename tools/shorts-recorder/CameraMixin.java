package com.kaan.deadzone.mixin.client.shorts;

import com.kaan.deadzone.client.shorts.ShortsDirector;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setPos(double x, double y, double z);

	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Inject(method = "update", at = @At("TAIL"))
	private void deadzone$override(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
		ShortsDirector.Pose p = ShortsDirector.camera;
		if (ShortsDirector.INSTANCE != null && p != null) {
			this.setRotation(p.yaw(), p.pitch());
			this.setPos(p.x(), p.y(), p.z());
		}
	}
}
