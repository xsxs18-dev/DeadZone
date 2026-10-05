package com.kaan.deadzone.client;

import com.kaan.deadzone.DeadZone;
import com.kaan.deadzone.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.EntityRendererFactories;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.model.ZombieEntityModel;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;

public class DeadZoneClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		for (ModEntities.Kind kind : ModEntities.ALL) {
			Identifier texture = DeadZone.id("textures/entity/zombie/" + kind.name() + ".png");
			Identifier eyes = DeadZone.id("textures/entity/zombie/" + kind.name() + "_eyes.png");
			EntityRendererFactories.<ZombieEntity>register(kind.type(), ctx -> new Renderer(ctx, texture, eyes));
		}
		EntityRendererFactories.register(ModEntities.GRENADE, FlyingItemEntityRenderer::new);
		EntityRendererFactories.register(ModEntities.MOLOTOV, FlyingItemEntityRenderer::new);
	}

	/** Vanilla-Zombiemodell mit eigener Textur und leuchtenden Augen, die man nachts von weitem sieht. */
	static class Renderer extends ZombieEntityRenderer {
		private final Identifier texture;

		Renderer(EntityRendererFactory.Context ctx, Identifier texture, Identifier eyes) {
			super(ctx);
			this.texture = texture;
			RenderLayer eyesLayer = RenderLayers.eyes(eyes);
			this.addFeature(new EyesFeatureRenderer<ZombieEntityRenderState, ZombieEntityModel<ZombieEntityRenderState>>(this) {
				@Override
				public RenderLayer getEyesTexture() {
					return eyesLayer;
				}
			});
		}

		@Override
		public Identifier getTexture(ZombieEntityRenderState state) {
			return this.texture;
		}
	}
}
