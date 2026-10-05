package com.kaan.deadzone.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blutrote, langsam pulsierende Vignette über dem Panorama und ein DeadZone-Schriftzug unten links. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	protected TitleScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;renderPanoramaBackground(Lnet/minecraft/client/gui/DrawContext;F)V", shift = At.Shift.AFTER))
	private void deadzone$overlay(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
		float pulse = 0.5F + 0.5F * (float) Math.sin(Util.getMeasuringTimeMs() / 1400.0);
		int topAlpha = (int) (60 + pulse * 30);
		context.fillGradient(0, 0, this.width, this.height / 3, (topAlpha << 24) | 0x200000, 0x00000000);
		context.fillGradient(0, this.height / 2, this.width, this.height, 0x00000000, 0xB0100000);
		context.drawTextWithShadow(this.textRenderer, Text.translatable("menu.deadzone.tagline"), 2, this.height - 20, 0xFFD04030);
	}
}
