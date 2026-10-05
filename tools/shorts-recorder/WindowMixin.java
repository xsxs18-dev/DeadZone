package com.kaan.deadzone.mixin.client.shorts;

import com.kaan.deadzone.client.shorts.ShortsDirector;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Window.class)
public abstract class WindowMixin implements ShortsDirector.WindowAccess {
	@Shadow
	private int framebufferWidth;
	@Shadow
	private int framebufferHeight;

	@Override
	public int deadzone$getFbWidth() {
		return this.framebufferWidth;
	}

	@Override
	public int deadzone$getFbHeight() {
		return this.framebufferHeight;
	}

	@Override
	public void deadzone$setFbSize(int w, int h) {
		this.framebufferWidth = w;
		this.framebufferHeight = h;
	}
}
