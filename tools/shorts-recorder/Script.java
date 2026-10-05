package com.kaan.deadzone.client.shorts;

import com.kaan.deadzone.client.shorts.ShortsDirector.Pose;
import com.kaan.deadzone.client.shorts.ShortsDirector.Shot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.kaan.deadzone.client.shorts.ShortsDirector.ease;
import static com.kaan.deadzone.client.shorts.ShortsDirector.lerp;
import static com.kaan.deadzone.client.shorts.ShortsDirector.look;

/** Das Drehbuch des Shorts. Zeiten in Bildern bei 60 FPS. */
final class Script {
	private Script() {
	}

	private static String f(String fmt, Object... args) {
		return String.format(Locale.ROOT, fmt, args);
	}

	private static String[] horde(int x, int y, int z, boolean withBrutes) {
		String[] types = {"walker", "walker", "runner", "walker", "crawler", "runner", "leaper", "walker", "bloater",
				"toxic", "walker", "runner", "spitter", "crawler", "leaper", "walker"};
		List<String> out = new ArrayList<>();
		for (int i = 0; i < types.length; i++) {
			int dx = (i % 5) - 2;
			int dz = -(i / 5) * 2 - (i % 2);
			out.add(f("summon deadzone:%s %d %d %d {PersistenceRequired:1b}", types[i], x + dx, y, z + dz));
		}
		if (withBrutes) {
			out.add(f("summon deadzone:brute %d %d %d {PersistenceRequired:1b}", x - 1, y, z - 9));
			out.add(f("summon deadzone:brute %d %d %d {PersistenceRequired:1b}", x + 2, y, z - 11));
		}
		return out.toArray(new String[0]);
	}

	static void build(List<Shot> shots) {
		// 1. Hook: Drohnenflug auf die Skyline zu
		Shot hook = new Shot("hook", 240);
		hook.setupTicks = 600;
		hook.fov = 80;
		hook.setup = (d, c) -> d.cmd(c, "gamemode spectator @a", "time set 12350", "effect clear @a",
				f("tp @p %d %d %d", d.ox - 165, d.y0 + 80, d.oz - 120));
		hook.ready = (d, c) -> d.measureCity(c);
		hook.cam = (d, fr, t) -> {
			double u = ease(t / 4.0);
			return look(lerp(u, d.ox - 165, d.ox - 118), lerp(u, d.y0 + 82, d.y0 + 44), lerp(u, d.oz - 120, d.oz - 84),
					lerp(u, d.ox, d.ox + 10), lerp(u, d.y0 + 35, d.y0 + 22), lerp(u, d.oz, d.oz + 10));
		};
		shots.add(hook);

		// 2. Tief durch die Straßenschlucht
		Shot street = new Shot("street", 360);
		street.setupTicks = 160;
		street.fov = 85;
		street.setup = (d, c) -> d.cmd(c, f("tp @p %.1f %d %d", d.sx + 90.5, d.y0 + 5, d.sz + 8));
		street.cam = (d, fr, t) -> {
			double u = t / 6.0;
			double x = d.sx + 90.5 + Math.sin(t * 0.8) * 0.6;
			double z = lerp(u, d.sz + 8, d.sz + 74);
			double y = d.y0 + 7.4 + 1.2 * ease(u);
			return new Pose(x, y, z, (float) (Math.sin(t * 0.7) * 4.0), (float) lerp(u, 9, 5));
		};
		shots.add(street);

		// 3. Kreisflug um den Bombenkrater
		Shot crater = new Shot("crater", 240);
		crater.setupTicks = 60;
		crater.fov = 80;
		crater.cam = (d, fr, t) -> {
			double a = Math.toRadians(lerp(ease(t / 4.0), 215, 305));
			double cx = d.ox + 0.5;
			double cz = d.oz + 0.5;
			return look(cx + Math.cos(a) * 15, d.y0 + 30, cz + Math.sin(a) * 15, cx, d.y0 - 4, cz);
		};
		crater.setup = (d, c) -> d.cmd(c, f("tp @p %d %d %d", d.ox - 12, d.y0 + 30, d.oz - 9));
		shots.add(crater);

		// 4. Auf der Hochstraße bis zur Abbruchkante
		Shot highway = new Shot("highway", 240);
		highway.setupTicks = 80;
		highway.fov = 80;
		highway.setup = (d, c) -> d.cmd(c, f("tp @p %d %d %d", d.sx + 4, d.y0 + 14, d.sz + d.highwayRow * 29 + 3));
		highway.cam = (d, fr, t) -> {
			double u = ease(t / 4.0);
			double end = Math.max(d.sx + 30, d.gapX - 12);
			double zc = d.sz + d.highwayRow * 29 + 3.5;
			return new Pose(lerp(u, d.sx + 4, end), d.y0 + 15.6, zc + Math.sin(t) * 0.3, -90F, (float) lerp(u, 6, 22));
		};
		shots.add(highway);

		// 5. Blutmond: die Horde kommt
		Shot hordeShot = new Shot("horde", 360);
		hordeShot.setupTicks = 30;
		hordeShot.fov = 75;
		hordeShot.setup = (d, c) -> {
			int px = d.sx + 119;
			int pz = d.sz + 170;
			d.cmd(c, "kill @e[type=!minecraft:player]", "gamemode survival @a", "clear @a",
					"effect give @a resistance infinite 4 true", "effect give @a regeneration infinite 4 true",
					"effect give @a saturation infinite 0 true", "time set 12900",
					f("tp @p %d.5 %d %d.5 180 0", px, d.y0 + 1, pz));
			d.cmd(c, horde(px, d.y0 + 1, d.sz + 146, false));
		};
		hordeShot.frame = (d, c, fr, t) -> {
			if (fr == 50) {
				d.cmd(c, f("summon minecraft:lightning_bolt %d %d %d", d.sx + 116, d.y0 + 1, d.sz + 142));
			}
		};
		hordeShot.cam = (d, fr, t) -> {
			double px = d.sx + 119.5;
			double pz = d.sz + 170.5;
			double u = ease(t / 6.0);
			double shake = Math.sin(t * 9.0) * 0.02;
			return look(px + 1.0, d.y0 + 1.9 + shake, pz - 4.5 + 2.0 * u, px - 0.5, d.y0 + 1.6, pz - 22);
		};
		shots.add(hordeShot);

		// 6. Feuergefecht in der Ich-Perspektive
		Shot fight = new Shot("fight", 420);
		fight.setupTicks = 30;
		fight.fov = 75;
		fight.hud = true;
		fight.setup = (d, c) -> {
			int px = d.sx + 119;
			int pz = d.sz + 170;
			d.cmd(c, "kill @e[type=!minecraft:player]", "clear @a",
					"item replace entity @p hotbar.0 with deadzone:minigun", "item replace entity @p hotbar.1 with deadzone:rpg",
					"item replace entity @p hotbar.2 with deadzone:molotov 4", "give @p deadzone:ammo_556 640",
					"give @p deadzone:explosive_round 8", f("tp @p %d.5 %d %d.5 180 0", px, d.y0 + 1, pz));
			d.cmd(c, horde(px, d.y0 + 1, d.sz + 148, true));
			d.cmd(c, "effect give @a fire_resistance infinite 0 true", "time set 12900");
		};
		fight.frame = (d, c, fr, t) -> {
			if (fr < 70) {
				// Molotow weit in die anrückende Horde
				if (fr == 0) {
					c.player.getInventory().setSelectedSlot(2);
				}
				d.aim(c, d.nearestZombie(c, false), 0.5, 0.2F);
				c.player.setPitch(c.player.getPitch() - 8);
				c.player.lastPitch = c.player.getPitch();
				if (fr == 40) {
					c.interactionManager.interactItem(c.player, Hand.MAIN_HAND);
				}
			} else if (fr < 320) {
				if (fr == 70) {
					c.player.getInventory().setSelectedSlot(0);
				}
				LivingEntity target = d.nearestZombieExcept(c, true);
				d.aim(c, target, 0.8, 0.22F);
				if (fr >= 85 && target != null) {
					c.interactionManager.interactItem(c.player, Hand.MAIN_HAND);
				}
			} else {
				if (fr == 320) {
					c.player.getInventory().setSelectedSlot(1);
				}
				LivingEntity brute = d.nearestZombie(c, true);
				d.aim(c, brute != null ? brute : d.nearestZombie(c, false), 0.5, 0.2F);
				if (fr == 355) {
					c.interactionManager.interactItem(c.player, Hand.MAIN_HAND);
				}
			}
		};
		shots.add(fight);

		// 7a. Die unscheinbare Hütte
		Shot shed = new Shot("shed", 90);
		shed.setupTicks = 450;
		shed.fov = 80;
		shed.setup = (d, c) -> d.cmd(c, "kill @e[type=!minecraft:player]", "clear @a", "effect clear @a", "gamemode spectator @a",
				"time set 12300", f("tp @p %d 120 %d", d.bunkerX - 8, d.bunkerZ - 8));
		shed.ready = (d, c) -> d.measureBunker(c);
		shed.cam = (d, fr, t) -> {
			double u = ease(t / 1.5);
			double bx = d.bunkerX + 0.5;
			double bz = d.bunkerZ + 0.5;
			int s = d.bunkerSurface;
			return look(lerp(u, bx - 10, bx - 6.5), lerp(u, s + 6, s + 4.2), lerp(u, bz - 10, bz - 6.5), bx, s + 1.0, bz);
		};
		shots.add(shed);

		// 7b. Falltür auf, Sturz in den Schacht
		Shot shaft = new Shot("shaft", 120);
		shaft.setupTicks = 2;
		shaft.fov = 85;
		shaft.setup = (d, c) -> d.cmd(c, "effect give @a night_vision infinite 0 true");
		shaft.frame = (d, c, fr, t) -> {
			if (fr == 30) {
				int s = d.bunkerSurface;
				d.cmd(c, f("setblock %d %d %d minecraft:air", d.bunkerX, s + 1, d.bunkerZ),
						f("setblock %d %d %d minecraft:spruce_trapdoor[open=true,half=top,facing=north]", d.bunkerX, s, d.bunkerZ),
						f("playsound minecraft:block.wooden_trapdoor.open block @a %d %d %d 1 0.8", d.bunkerX, s, d.bunkerZ));
			}
		};
		shaft.cam = (d, fr, t) -> {
			double bx = d.bunkerX + 0.5;
			double bz = d.bunkerZ + 0.5;
			int s = d.bunkerSurface;
			int floor = s - 36;
			double y;
			if (fr < 30) {
				y = lerp(fr / 30.0, s + 2.7, s + 2.3);
			} else {
				y = lerp(ease((fr - 30) / 90.0), s + 2.3, floor + 2.5);
			}
			float pitch = fr < 95 ? 89F : (float) lerp(ease((fr - 95) / 25.0), 89, 12);
			return new Pose(bx, y, bz, 0F, pitch);
		};
		shots.add(shaft);

		// 7c. Durch den Gang zum Boss
		Shot boss = new Shot("boss", 180);
		boss.setupTicks = 40;
		boss.fov = 80;
		boss.setup = (d, c) -> {
			int floor = d.bunkerSurface - 36;
			d.cmd(c, "effect clear @a", "kill @e[type=deadzone:patient_zero]",
					f("summon deadzone:patient_zero %d.5 %d %d.5 {NoAI:1b,Rotation:[180f,0f],PersistenceRequired:1b}", d.bunkerX, floor + 1, d.bunkerZ + 14));
		};
		boss.frame = (d, c, fr, t) -> {
			if (fr == 105) {
				int floor = d.bunkerSurface - 36;
				d.cmd(c, f("playsound minecraft:entity.warden.roar hostile @a %d %d %d 4 0.7", d.bunkerX, floor + 3, d.bunkerZ + 12),
						f("particle minecraft:large_smoke %d.5 %d %d.5 0.8 0.8 0.8 0.03 60", d.bunkerX, floor + 3, d.bunkerZ + 14));
			}
		};
		boss.cam = (d, fr, t) -> {
			double bx = d.bunkerX + 0.5;
			double bz = d.bunkerZ + 0.5;
			int floor = d.bunkerSurface - 36;
			double z;
			double y;
			if (fr < 100) {
				double u = ease(fr / 100.0);
				z = lerp(u, bz, bz + 9.5);
				y = floor + 2.6;
			} else {
				double u = ease((fr - 100) / 80.0);
				z = lerp(u, bz + 9.5, bz + 9.0);
				y = lerp(u, floor + 2.6, floor + 7.5);
			}
			return look(bx, y, z, bx, floor + 2.6, bz + 14);
		};
		shots.add(boss);

		// 8. Outro über der Stadt
		Shot outro = new Shot("outro", 180);
		outro.setupTicks = 450;
		outro.fov = 80;
		outro.setup = (d, c) -> d.cmd(c, "kill @e[type=!minecraft:player]", "effect clear @a", "gamemode spectator @a", "time set 12600",
				f("tp @p %d %d %d", d.ox - 80, d.y0 + 75, d.oz - 80));
		outro.cam = (d, fr, t) -> {
			double u = ease(t / 3.0);
			double a = Math.toRadians(lerp(u, 222, 238));
			double r = lerp(u, 125, 112);
			return look(d.ox + Math.cos(a) * r, lerp(u, d.y0 + 70, d.y0 + 80), d.oz + Math.sin(a) * r, d.ox, d.y0 + 25, d.oz);
		};
		shots.add(outro);
	}
}
