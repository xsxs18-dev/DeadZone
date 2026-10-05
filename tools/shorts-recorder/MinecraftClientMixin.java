package com.kaan.deadzone.mixin.client.shorts;

import com.kaan.deadzone.client.shorts.ShortsDirector;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void deadzone$frameStart(boolean tick, CallbackInfo ci) {
		if (ShortsDirector.INSTANCE != null) {
			ShortsDirector.INSTANCE.onFrameStart((MinecraftClient) (Object) this);
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;blitToScreen()V"))
	private void deadzone$frameEnd(boolean tick, CallbackInfo ci) {
		if (ShortsDirector.INSTANCE != null) {
			ShortsDirector.INSTANCE.onFrameEnd((MinecraftClient) (Object) this);
		}
	}
}
