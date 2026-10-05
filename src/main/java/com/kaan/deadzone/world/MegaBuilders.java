package com.kaan.deadzone.world;

import com.kaan.deadzone.registry.ModEntities;
import com.kaan.deadzone.registry.ModItems;
import com.kaan.deadzone.world.BuildingPiece.Kind;
import com.kaan.deadzone.world.Builders.Rect;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

import static com.kaan.deadzone.world.Builders.AIR;
import static com.kaan.deadzone.world.Builders.CAVE_AIR;
import static com.kaan.deadzone.world.Builders.COBWEB;
import static com.kaan.deadzone.world.Builders.DIRT;
import static com.kaan.deadzone.world.Builders.RUBBLE;
import static com.kaan.deadzone.world.Builders.STONE;

/** Spektakuläre Teile der Mega-Städte und der Geheimbunker. */
final class MegaBuilders {
	private MegaBuilders() {
	}

	private static BlockState s(net.minecraft.block.Block b) {
		return b.getDefaultState();
	}

	static void build(Kind kind, Canvas c, Direction facing, int flags) {
		switch (kind) {
			case SUPERMARKET -> supermarket(c, facing);
			case GAS_STATION -> gasStation(c, facing);
			case PARKING -> parking(c);
			case PARK -> park(c);
			case CHECKPOINT -> checkpoint(c);
			case CRATER -> crater(c);
			case FALLEN_TOWER -> fallenTower(c, facing.getAxis() == Direction.Axis.X);
			case HIGHWAY_X -> highway(c);
			case SECRET_BUNKER -> secretBunker(c);
			default -> {
			}
		}
	}

	private static Rect lotWithSidewalk(Canvas c) {
		Rect lot = Builders.fullRect(c);
		Builders.prepareGround(c, lot, c.y1 - c.y0, STONE);
		Builders.sidewalk(c, lot, 2);
		return lot.inset(2);
	}

	// ------------------------------------------------------------------ Straßen-Details

	/** Straßenlaternen, Bombenkrater und Barrikaden für Mega-Städte. */
	static void metroRoad(Canvas c, boolean alongX) {
		int length = alongX ? c.x1 - c.x0 + 1 : c.z1 - c.z0 + 1;
		for (int a = 6; a < length - 3; a += 13) {
			int across = (a / 13) % 2 == 0 ? 0 : 6;
			int px = alongX ? c.x0 + a : c.x0 + across;
			int pz = alongX ? c.z0 + across : c.z0 + a;
			boolean broken = c.rnd(px, 0, pz, 300) < 0.3F;
			int h = broken ? 2 : 5;
			for (int y = 1; y <= h; y++) {
				c.set(px, c.y0 + y, pz, s(Blocks.POLISHED_BLACKSTONE_WALL));
			}
			if (!broken) {
				int inward = across == 0 ? 1 : -1;
				int ax = alongX ? px : px + inward;
				int az = alongX ? pz + inward : pz;
				c.set(ax, c.y0 + 6, az, s(Blocks.POLISHED_BLACKSTONE_SLAB));
				c.set(px, c.y0 + 6, pz, s(Blocks.POLISHED_BLACKSTONE_SLAB));
				c.set(ax, c.y0 + 5, az, s(Blocks.REDSTONE_LAMP));
			}
		}
		for (int a = 10; a < length - 8; a += 24) {
			int cx = alongX ? c.x0 + a : c.x0 + 3;
			int cz = alongX ? c.z0 + 3 : c.z0 + a;
			float r = c.rnd(cx, 0, cz, 301);
			if (r < 0.18F) {
				roadCrater(c, cx, cz, 3);
			} else if (r < 0.26F) {
				for (int i = 0; i < 7; i++) {
					int bx = alongX ? cx : c.x0 + i;
					int bz = alongX ? c.z0 + i : cz;
					if (i != 3 && i != 4) {
						c.set(bx, c.y0 + 1, bz, c.rnd(bx, 1, bz, 302) < 0.5F ? s(Blocks.PACKED_MUD) : s(Blocks.BARREL));
						if (c.rnd(bx, 2, bz, 303) < 0.4F) {
							c.set(bx, c.y0 + 2, bz, COBWEB);
						}
					}
				}
			}
		}
	}

	private static void roadCrater(Canvas c, int cx, int cz, int radius) {
		for (int x = cx - radius; x <= cx + radius; x++) {
			for (int z = cz - radius; z <= cz + radius; z++) {
				if (!c.box.contains(new net.minecraft.util.math.Vec3i(x, c.y0, z))) {
					continue;
				}
				double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz)) / radius;
				if (d > 1.0) {
					continue;
				}
				int depth = (int) Math.round((1 - d) * 3);
				for (int y = c.y0 - depth + 1; y <= c.y0 + 2; y++) {
					c.set(x, y, z, AIR);
				}
				c.set(x, c.y0 - depth, z, c.pick(x, 0, z, 304, s(Blocks.BLACKSTONE), s(Blocks.BASALT), s(Blocks.COAL_BLOCK), s(Blocks.MAGMA_BLOCK), s(Blocks.COBBLED_DEEPSLATE)));
			}
		}
		if (c.rnd(cx, 1, cz, 305) < 0.5F) {
			Builders.fire(c, cx, c.y0 - 2, cz);
		}
	}

	// ------------------------------------------------------------------ Fassaden

	/** Großes rotes Kreuz an der Krankenhausfassade. */
	static void redCross(Canvas c, Rect fp, Direction facing, int y) {
		int mid = facing.getAxis() == Direction.Axis.X ? (fp.z0() + fp.z1()) / 2 : (fp.x0() + fp.x1()) / 2;
		int plane = switch (facing) {
			case NORTH -> fp.z0();
			case SOUTH -> fp.z1();
			case WEST -> fp.x0();
			default -> fp.x1();
		};
		for (int a = -2; a <= 2; a++) {
			for (int dy = 0; dy <= 4; dy++) {
				boolean cross = Math.abs(a) <= 0 || dy == 2;
				if (!cross || y + dy <= c.y0) {
					continue;
				}
				if (facing.getAxis() == Direction.Axis.X) {
					c.set(plane, y + dy, mid + a, s(Blocks.RED_CONCRETE));
				} else {
					c.set(mid + a, y + dy, plane, s(Blocks.RED_CONCRETE));
				}
			}
		}
	}

	// ------------------------------------------------------------------ Supermarkt

	private static void supermarket(Canvas c, Direction facing) {
		Rect in = lotWithSidewalk(c);
		Rect fp = in.inset(1);
		int y0 = c.y0;
		int h = 6;
		for (int x = fp.x0(); x <= fp.x1(); x++) {
			for (int z = fp.z0(); z <= fp.z1(); z++) {
				c.foundation(x, y0, z, STONE);
				c.set(x, y0, z, c.rnd(x, 0, z, 400) < 0.1F ? s(Blocks.CRACKED_STONE_BRICKS) : s(Blocks.SMOOTH_QUARTZ));
				boolean edge = x == fp.x0() || x == fp.x1() || z == fp.z0() || z == fp.z1();
				boolean front = switch (facing) {
					case NORTH -> z == fp.z0();
					case SOUTH -> z == fp.z1();
					case WEST -> x == fp.x0();
					default -> x == fp.x1();
				};
				for (int y = y0 + 1; y < y0 + h; y++) {
					BlockState st = AIR;
					if (edge) {
						if (front && y <= y0 + 3) {
							st = c.rnd(x, y, z, 401) < 0.55F ? AIR : s(Blocks.GLASS_PANE);
						} else if (front && y == y0 + 4) {
							st = (x + z) % 2 == 0 ? s(Blocks.RED_CONCRETE) : s(Blocks.YELLOW_CONCRETE);
						} else {
							st = s(Blocks.WHITE_CONCRETE);
						}
					}
					c.set(x, y, z, st);
				}
				if (c.noise(x, z, 4, 402) < 0.72F) {
					c.set(x, y0 + h, z, s(Blocks.SMOOTH_STONE_SLAB));
				}
			}
		}
		boolean shelvesAlongX = facing.getAxis() == Direction.Axis.Z;
		for (int x = fp.x0() + 2; x <= fp.x1() - 2; x++) {
			for (int z = fp.z0() + 3; z <= fp.z1() - 3; z++) {
				int row = shelvesAlongX ? z - fp.z0() : x - fp.x0();
				int along = shelvesAlongX ? x - fp.x0() : z - fp.z0();
				if (row % 3 != 0 || along % 9 == 4) {
					continue;
				}
				if (c.rnd(x, 1, z, 403) < 0.12F) {
					c.barrel(x, y0 + 1, z, ModStructures.LOOT_SUPERMARKET);
				} else if (c.rnd(x, 1, z, 404) < 0.9F) {
					c.set(x, y0 + 1, z, s(Blocks.SPRUCE_SLAB).with(SlabBlock.TYPE, SlabType.TOP));
					c.set(x, y0 + 2, z, s(Blocks.SPRUCE_SLAB).with(SlabBlock.TYPE, SlabType.TOP));
				}
			}
		}
		Builders.rubblePile(c, fp.x0() + 3, fp.z0() + 3, 3, 3, fp, 405);
	}

	// ------------------------------------------------------------------ Tankstelle

	private static void gasStation(Canvas c, Direction facing) {
		Rect in = lotWithSidewalk(c);
		int y0 = c.y0;
		for (int x = in.x0(); x <= in.x1(); x++) {
			for (int z = in.z0(); z <= in.z1(); z++) {
				float r = c.rnd(x, 0, z, 410);
				c.set(x, y0, z, r < 0.12F ? s(Blocks.BLACKSTONE) : r < 0.2F ? s(Blocks.COAL_BLOCK) : s(Blocks.SMOOTH_STONE));
			}
		}
		int cx = (in.x0() + in.x1()) / 2;
		int cz = (in.z0() + in.z1()) / 2;
		// Dach auf vier Säulen
		for (int x = cx - 5; x <= cx + 5; x++) {
			for (int z = cz - 4; z <= cz + 1; z++) {
				boolean post = (x == cx - 5 || x == cx + 5) && (z == cz - 4 || z == cz + 1);
				if (post) {
					for (int y = y0 + 1; y <= y0 + 4; y++) {
						c.set(x, y, z, s(Blocks.QUARTZ_PILLAR));
					}
				}
				boolean rim = x == cx - 5 || x == cx + 5 || z == cz - 4 || z == cz + 1;
				if (c.noise(x, z, 3, 411) < 0.8F) {
					c.set(x, y0 + 5, z, rim ? s(Blocks.RED_CONCRETE) : s(Blocks.WHITE_CONCRETE));
				}
			}
		}
		for (int i = -1; i <= 1; i += 2) {
			int px = cx + i * 2;
			c.set(px, y0 + 1, cz - 1, s(Blocks.POLISHED_BLACKSTONE));
			c.set(px, y0 + 2, cz - 1, s(Blocks.RED_CONCRETE));
			c.set(px, y0 + 3, cz - 1, s(Blocks.WHITE_CONCRETE));
		}
		Builders.car(c, cx - 1, y0 + 1, cz - 4, true, 412);
		Builders.fire(c, cx + 4, y0 + 1, cz - 2);
		// Kleiner Laden hinten
		int sz0 = cz + 3;
		for (int x = cx - 4; x <= cx + 4; x++) {
			for (int z = sz0; z <= Math.min(sz0 + 5, in.z1()); z++) {
				boolean edge = x == cx - 4 || x == cx + 4 || z == sz0 || z == Math.min(sz0 + 5, in.z1());
				for (int y = y0 + 1; y <= y0 + 3; y++) {
					c.set(x, y, z, edge ? (z == sz0 && y <= y0 + 2 && Math.abs(x - cx) <= 2 ? s(Blocks.GLASS_PANE) : s(Blocks.WHITE_TERRACOTTA)) : AIR);
				}
				c.set(x, y0 + 4, z, s(Blocks.SMOOTH_STONE_SLAB));
			}
		}
		c.set(cx, y0 + 1, sz0, AIR);
		c.set(cx, y0 + 2, sz0, AIR);
		c.chest(cx + 2, y0 + 1, sz0 + 2, Direction.NORTH, ModStructures.LOOT_GAS);
		c.barrel(cx - 2, y0 + 1, sz0 + 2, ModStructures.LOOT_SUPERMARKET);
	}

	// ------------------------------------------------------------------ Parkhaus

	private static void parking(Canvas c) {
		Rect in = lotWithSidewalk(c);
		Rect fp = in.inset(1);
		int levels = Math.max(2, (c.y1 - c.y0 - 2) / 4);
		for (int lv = 0; lv < levels; lv++) {
			int fy = c.y0 + lv * 4;
			for (int x = fp.x0(); x <= fp.x1(); x++) {
				for (int z = fp.z0(); z <= fp.z1(); z++) {
					if (lv == 0) {
						c.foundation(x, fy, z, STONE);
					}
					boolean collapsed = lv > 0 && c.noise(x, z, 4, 420 + lv) > 0.78F;
					boolean edge = x == fp.x0() || x == fp.x1() || z == fp.z0() || z == fp.z1();
					boolean pillar = (x - fp.x0()) % 5 == 0 && (z - fp.z0()) % 5 == 0;
					if (!collapsed) {
						c.set(x, fy, z, (x - fp.x0()) % 4 == 0 ? s(Blocks.WHITE_CONCRETE) : s(Blocks.GRAY_CONCRETE));
						if (edge && lv > 0) {
							c.set(x, fy + 1, z, s(Blocks.SMOOTH_STONE_SLAB));
						}
					} else if (lv > 0 && c.rnd(x, 0, z, 421) < 0.5F) {
						c.set(x, c.y0 + 1, z, RUBBLE[(int) (c.rnd(x, lv, z, 422) * RUBBLE.length) % RUBBLE.length]);
					}
					if (pillar && lv < levels - 1) {
						for (int y = fy + 1; y < fy + 4; y++) {
							c.set(x, y, z, s(Blocks.GRAY_CONCRETE));
						}
					}
				}
			}
			for (int x = fp.x0() + 2; x + 4 < fp.x1(); x += 5) {
				if (c.rnd(x, fy, fp.z0(), 423) < 0.6F && c.get(x, fy, fp.z0() + 2).isOpaque()) {
					Builders.car(c, x, fy + 1, fp.z0() + 1, false, 424 + lv);
				}
			}
		}
		for (int y = c.y0 + 1; y <= c.y0 + (levels - 1) * 4; y++) {
			c.set(fp.x0() + 1, y, fp.z1() - 1, Builders.ladder(Direction.SOUTH));
			c.set(fp.x0() + 1, y, fp.z1(), s(Blocks.GRAY_CONCRETE));
		}
		c.chest(fp.x1() - 1, c.y0 + (levels - 1) * 4 + 1, fp.z1() - 1, Direction.WEST, ModStructures.LOOT_CITY);
	}

	// ------------------------------------------------------------------ Park mit Überlebenden-Lager

	private static void park(Canvas c) {
		Rect in = lotWithSidewalk(c);
		Builders.overgrownGround(c, in.x0(), in.z0(), in.x1(), in.z1());
		int cx = (in.x0() + in.x1()) / 2;
		int cz = (in.z0() + in.z1()) / 2;
		for (int x = cx - 3; x <= cx + 3; x++) {
			for (int z = cz - 3; z <= cz + 3; z++) {
				int d = Math.max(Math.abs(x - cx), Math.abs(z - cz));
				if (d == 3) {
					c.set(x, c.y0 + 1, z, c.rnd(x, 1, z, 430) < 0.3F ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.STONE_BRICKS));
				} else {
					c.set(x, c.y0, z, s(Blocks.MOSSY_COBBLESTONE));
				}
			}
		}
		c.set(cx, c.y0 + 1, cz, s(Blocks.STONE_BRICK_WALL));
		c.set(cx, c.y0 + 2, cz, s(Blocks.STONE_BRICK_WALL));
		c.set(cx, c.y0 + 3, cz, s(Blocks.CHISELED_STONE_BRICKS));
		for (int i = 0; i < 3; i++) {
			int tx = in.x0() + 2 + c.globalInt(431 + i, in.x1() - in.x0() - 4);
			int tz = in.z0() + 2 + c.globalInt(434 + i, in.z1() - in.z0() - 4);
			if (Math.abs(tx - cx) < 5 && Math.abs(tz - cz) < 5) {
				continue;
			}
			for (int y = 1; y <= 5; y++) {
				c.set(tx, c.y0 + y, tz, s(Blocks.DARK_OAK_LOG));
			}
			c.set(tx + 1, c.y0 + 4, tz, s(Blocks.DARK_OAK_LOG).with(net.minecraft.block.PillarBlock.AXIS, Direction.Axis.X));
			c.set(tx, c.y0 + 5, tz - 1, s(Blocks.DARK_OAK_LOG).with(net.minecraft.block.PillarBlock.AXIS, Direction.Axis.Z));
		}
		// Lager von Überlebenden
		int lx = in.x0() + 2;
		int lz = in.z1() - 4;
		for (int z = lz; z <= lz + 2; z++) {
			c.set(lx, c.y0 + 1, z, s(Blocks.WHITE_WOOL));
			c.set(lx + 1, c.y0 + 2, z, s(Blocks.WHITE_WOOL));
			c.set(lx + 2, c.y0 + 1, z, s(Blocks.WHITE_WOOL));
		}
		c.set(lx + 4, c.y0 + 1, lz + 1, s(Blocks.CAMPFIRE).with(net.minecraft.block.CampfireBlock.LIT, false));
		c.chest(lx + 1, c.y0 + 1, lz + 1, Direction.EAST, ModStructures.LOOT_HOUSE);
		c.set(lx + 5, c.y0 + 1, lz, s(Blocks.OAK_STAIRS).with(StairsBlock.FACING, Direction.WEST));
	}

	// ------------------------------------------------------------------ Militär-Checkpoint

	private static void checkpoint(Canvas c) {
		Rect in = lotWithSidewalk(c);
		for (int x = in.x0(); x <= in.x1(); x++) {
			for (int z = in.z0(); z <= in.z1(); z++) {
				c.set(x, c.y0, z, c.rnd(x, 0, z, 440) < 0.6F ? s(Blocks.PACKED_MUD) : s(Blocks.COARSE_DIRT));
				boolean ring = x == in.x0() || x == in.x1() || z == in.z0() || z == in.z1();
				boolean gate = Math.abs(x - (in.x0() + in.x1()) / 2) <= 1;
				if (ring && !gate) {
					c.set(x, c.y0 + 1, z, s(Blocks.PACKED_MUD));
					c.set(x, c.y0 + 2, z, c.rnd(x, 2, z, 441) < 0.5F ? s(Blocks.PACKED_MUD) : COBWEB);
				}
			}
		}
		int cx = (in.x0() + in.x1()) / 2;
		int cz = (in.z0() + in.z1()) / 2;
		// Zelte
		for (int t = 0; t < 2; t++) {
			int tx = in.x0() + 2 + t * 6;
			for (int z = in.z0() + 2; z <= in.z0() + 6; z++) {
				c.set(tx, c.y0 + 1, z, s(Blocks.GREEN_WOOL));
				c.set(tx + 1, c.y0 + 2, z, s(Blocks.GREEN_WOOL));
				c.set(tx + 2, c.y0 + 1, z, s(Blocks.GREEN_WOOL));
			}
			c.chest(tx + 1, c.y0 + 1, in.z0() + 4, Direction.SOUTH, ModStructures.LOOT_MILITARY);
		}
		// Panzer
		for (int z = cz; z <= cz + 6; z++) {
			for (int x = cx + 2; x <= cx + 6; x++) {
				boolean side = x == cx + 2 || x == cx + 6;
				c.set(x, c.y0 + 1, z, side ? s(Blocks.GRAY_CONCRETE) : s(Blocks.GREEN_CONCRETE));
				c.set(x, c.y0 + 2, z, s(Blocks.GREEN_CONCRETE));
			}
		}
		for (int x = cx + 3; x <= cx + 5; x++) {
			for (int z = cz + 2; z <= cz + 4; z++) {
				c.set(x, c.y0 + 3, z, s(Blocks.GREEN_TERRACOTTA));
			}
		}
		for (int z = cz - 3; z < cz + 2; z++) {
			c.set(cx + 4, c.y0 + 3, z, s(Blocks.POLISHED_BLACKSTONE_WALL));
		}
		// Gefallene Soldaten (Rüstungsständer)
		c.armorStand(in.x0() + 3, c.y0 + 1, cz + 3, new ItemStack(ModItems.MILITARY_HELMET), new ItemStack(ModItems.MILITARY_VEST));
		c.armorStand(in.x1() - 3, c.y0 + 1, in.z1() - 3, new ItemStack(ModItems.GAS_MASK), new ItemStack(ModItems.MILITARY_VEST));
		// Flutlicht
		for (int y = 1; y <= 4; y++) {
			c.set(in.x1() - 1, c.y0 + y, in.z0() + 1, s(Blocks.IRON_BARS));
		}
		c.set(in.x1() - 1, c.y0 + 5, in.z0() + 1, s(Blocks.REDSTONE_LAMP));
	}

	// ------------------------------------------------------------------ Riesiger Krater im Zentrum

	private static void crater(Canvas c) {
		Rect lot = Builders.fullRect(c);
		Builders.prepareGround(c, lot, c.y1 - c.y0, STONE);
		int cx = (c.x0 + c.x1) / 2;
		int cz = (c.z0 + c.z1) / 2;
		double radius = Math.min(c.x1 - c.x0, c.z1 - c.z0) / 2.0 - 0.5;
		for (int x = c.x0; x <= c.x1; x++) {
			for (int z = c.z0; z <= c.z1; z++) {
				double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz)) / radius;
				float r = c.rnd(x, 0, z, 450);
				if (d > 1.0) {
					c.set(x, c.y0, z, r < 0.5F ? s(Blocks.BLACKSTONE) : r < 0.75F ? s(Blocks.COAL_BLOCK) : s(Blocks.CRACKED_STONE_BRICKS));
					continue;
				}
				int depth = (int) Math.round(Math.sqrt(1 - d * d) * 7);
				for (int y = c.y0 - depth + 1; y <= c.y0; y++) {
					c.set(x, y, z, AIR);
				}
				c.set(x, c.y0 - depth, z, c.pick(x, 1, z, 451, s(Blocks.BLACKSTONE), s(Blocks.BASALT), s(Blocks.MAGMA_BLOCK), s(Blocks.COAL_BLOCK), s(Blocks.COBBLED_DEEPSLATE)));
				if (d > 0.85 && r < 0.5F) {
					c.set(x, c.y0 + 1, z, RUBBLE[(int) (r * 997) % RUBBLE.length]);
				}
			}
		}
		// Abgestürzter Hubschrauber in der Mitte
		int hy = c.y0 - 6;
		for (int z = cz - 3; z <= cz + 3; z++) {
			for (int x = cx - 1; x <= cx + 1; x++) {
				c.set(x, hy + 1, z, s(Blocks.GREEN_CONCRETE));
				c.set(x, hy + 2, z, z < cz - 1 ? s(Blocks.TINTED_GLASS) : s(Blocks.GREEN_CONCRETE));
			}
		}
		for (int z = cz + 4; z <= cz + 8; z++) {
			c.set(cx, hy + 2 + (z - cz - 4) / 2, z, s(Blocks.GREEN_CONCRETE));
		}
		for (int i = -5; i <= 5; i++) {
			c.set(cx + i, hy + 3, cz + i / 3, s(Blocks.IRON_BARS));
		}
		c.chest(cx, hy + 1, cz + 1, Direction.SOUTH, ModStructures.LOOT_MILITARY);
		Builders.fire(c, cx + 3, hy + 2, cz - 2);
		Builders.fire(c, cx - 4, hy + 3, cz + 3);
		Builders.fire(c, cx + 1, hy + 4, cz + 6);
	}

	// ------------------------------------------------------------------ Umgestürztes Hochhaus

	/** Ein Hochhaus-Stück, das quer über der Straße liegt. */
	private static void fallenTower(Canvas c, boolean alongX) {
		int len = alongX ? c.x1 - c.x0 + 1 : c.z1 - c.z0 + 1;
		int width = alongX ? c.z1 - c.z0 + 1 : c.x1 - c.x0 + 1;
		BlockState frame = c.global(500) < 0.5F ? s(Blocks.GRAY_CONCRETE) : s(Blocks.LIGHT_GRAY_CONCRETE);
		BlockState glass = c.global(501) < 0.5F ? s(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE) : s(Blocks.GLASS_PANE);
		int h = Math.min(width, 7);
		for (int a = 0; a < len; a++) {
			// zerklüftete Enden
			int cutStart = (int) (c.rnd(a, 0, 0, 502) * 3);
			for (int w = 0; w < width; w++) {
				int x = alongX ? c.x0 + a : c.x0 + w;
				int z = alongX ? c.z0 + w : c.z0 + a;
				if (!c.columnInClip(x, z)) {
					continue;
				}
				int endDist = Math.min(a, len - 1 - a);
				if (endDist < cutStart + (int) (c.rnd(x, 1, z, 503) * 3)) {
					if (c.rnd(x, 2, z, 504) < 0.6F) {
						c.set(x, c.y0 + 1, z, RUBBLE[(int) (c.rnd(x, 3, z, 505) * RUBBLE.length) % RUBBLE.length]);
					}
					continue;
				}
				for (int y = 1; y <= h; y++) {
					boolean sideW = w == 0 || w == width - 1;
					boolean sideY = y == 1 || y == h;
					boolean slice = a % 4 == 0;
					BlockState st;
					if ((sideW && sideY) || slice && (sideW || sideY)) {
						st = frame;
					} else if (sideW || y == h) {
						st = c.rnd(x, y, z, 506) < 0.45F ? AIR : glass;
					} else if (y == 1) {
						st = frame;
					} else if (slice) {
						st = c.rnd(x, y, z, 507) < 0.5F ? s(Blocks.SMOOTH_STONE) : AIR;
					} else {
						st = c.rnd(x, y, z, 508) < 0.04F ? RUBBLE[0] : AIR;
					}
					c.set(x, c.y0 + y, z, st);
				}
			}
		}
		for (int i = 0; i < 4; i++) {
			int a = c.globalInt(510 + i, len);
			int x = alongX ? c.x0 + a : (c.global(520 + i) < 0.5F ? c.x0 - 1 : c.x1 + 1);
			int z = alongX ? (c.global(520 + i) < 0.5F ? c.z0 - 1 : c.z1 + 1) : c.z0 + a;
			Builders.rubblePile(c, x, z, 2, 3, Builders.fullRect(c), 530 + i);
		}
		Builders.fire(c, alongX ? c.x0 + len / 2 : c.x0 + width / 2, c.y0 + h + 1, alongX ? c.z0 + width / 2 : c.z0 + len / 2);
	}

	// ------------------------------------------------------------------ Eingestürzte Hochstraße

	private static void highway(Canvas c) {
		int deck = c.y0 + 12;
		int len = c.x1 - c.x0 + 1;
		int gap1 = len / 4 + c.globalInt(600, len / 6);
		int gap2 = len * 3 / 5 + c.globalInt(601, len / 6);
		int gapLen = 11 + c.globalInt(602, 4);
		for (int x = c.x0; x <= c.x1; x++) {
			int a = x - c.x0;
			boolean inGap1 = a >= gap1 && a < gap1 + gapLen;
			boolean inGap2 = a >= gap2 && a < gap2 + gapLen;
			for (int z = c.z0; z <= c.z1; z++) {
				if (!c.columnInClip(x, z)) {
					continue;
				}
				int across = z - c.z0;
				// Pfeiler
				if (a % 16 == 8 && across >= 2 && across <= 4 && !inGap1 && !inGap2) {
					for (int y = c.y0 + 1; y < deck - 1; y++) {
						c.set(x, y, z, s(Blocks.GRAY_CONCRETE));
					}
					c.set(x, deck - 1, z, s(Blocks.SMOOTH_STONE));
				}
				if (inGap1 || inGap2) {
					// heruntergestürztes Fahrbahnstück: schräg von der Kante bis zur Straße
					int k = inGap1 ? a - gap1 : a - gap2;
					int y = deck - 1 - (int) ((float) k / gapLen * 11);
					if (inGap2) {
						y = c.y0 + 1 + (int) ((float) k / gapLen * 10);
					}
					if (c.rnd(x, y, z, 603) < 0.85F && y > c.y0) {
						c.set(x, y, z, s(Blocks.SMOOTH_STONE));
						c.set(x, y + 1, z, c.rnd(x, y, z, 604) < 0.3F ? s(Blocks.CRACKED_STONE_BRICKS) : s(Blocks.BLACK_CONCRETE));
					}
					if (c.rnd(x, 0, z, 605) < 0.4F) {
						c.set(x, c.y0 + 1, z, RUBBLE[(int) (c.rnd(x, 1, z, 606) * RUBBLE.length) % RUBBLE.length]);
					}
					continue;
				}
				int edgeDist = Math.min(Math.abs(a - gap1), Math.min(Math.abs(a - (gap1 + gapLen - 1)), Math.min(Math.abs(a - gap2), Math.abs(a - (gap2 + gapLen - 1)))));
				if (edgeDist <= 1 && c.rnd(x, 1, z, 607) < 0.4F) {
					continue;
				}
				c.set(x, deck - 1, z, s(Blocks.SMOOTH_STONE));
				BlockState top;
				if (across == 3 && (a / 3) % 2 == 0) {
					top = s(Blocks.YELLOW_TERRACOTTA);
				} else {
					top = c.rnd(x, 2, z, 608) < 0.15F ? s(Blocks.POLISHED_BLACKSTONE) : s(Blocks.BLACK_CONCRETE);
				}
				c.set(x, deck, z, top);
				if ((across == 0 || across == 6) && c.rnd(x, 3, z, 609) < 0.9F) {
					c.set(x, deck + 1, z, s(Blocks.ANDESITE_WALL));
				} else {
					c.set(x, deck + 1, z, AIR);
					c.set(x, deck + 2, z, AIR);
				}
			}
		}
		for (int a = 4; a + 5 < len; a += 14) {
			if (c.rnd(a, 0, 0, 610) < 0.5F) {
				int x = c.x0 + a;
				boolean blocked = (a + 4 >= gap1 && a <= gap1 + gapLen) || (a + 4 >= gap2 && a <= gap2 + gapLen);
				if (!blocked) {
					Builders.car(c, x, deck + 1, c.z0 + (c.rnd(a, 1, 0, 611) < 0.5F ? 1 : 4), true, 612);
				}
			}
		}
		// ein Auto hängt über der Abbruchkante
		Builders.car(c, c.x0 + gap1 - 3, deck + 1, c.z0 + 2, true, 613);
		Builders.fire(c, c.x0 + gap1 + gapLen / 2, c.y0 + 2, c.z0 + 3);
	}

	// ------------------------------------------------------------------ Geheimbunker

	/**
	 * Oben nur eine halb verfallene Hütte. Unter dem Moos-Teppich liegt eine Falltür,
	 * dahinter ein 35 Blöcke tiefer Schacht in eine geheime Forschungsanlage mit Boss.
	 */
	private static void secretBunker(Canvas c) {
		int surface = c.y1 - 7;
		int floor = c.y0 + 4;
		int bx = c.x0;
		int bz = c.z0;
		BlockState[] wall = {s(Blocks.POLISHED_DEEPSLATE), s(Blocks.DEEPSLATE_TILES), s(Blocks.CRACKED_DEEPSLATE_TILES), s(Blocks.GRAY_CONCRETE)};
		BlockState light = s(Blocks.SEA_LANTERN);

		// Räume {x0, z0, x1, z1, Höhe}
		int[][] rooms = {
				{18, 18, 26, 26, 6},   // Zentrale Halle
				{12, 3, 32, 15, 7},    // Kommandozentrale (Norden)
				{29, 12, 42, 32, 7},   // Labor (Osten)
				{12, 29, 32, 43, 11},  // Sicherheitsverwahrung mit Boss (Süden)
				{2, 12, 15, 32, 6},    // Waffentresor + Schlafraum (Westen)
				{21, 14, 23, 18, 5},   // Gang Nord
				{26, 21, 29, 23, 5},   // Gang Ost
				{21, 26, 23, 29, 5},   // Gang Süd
				{15, 21, 18, 23, 5}    // Gang West
		};
		for (int[] r : rooms) {
			for (int x = bx + r[0]; x <= bx + r[2]; x++) {
				for (int z = bz + r[1]; z <= bz + r[3]; z++) {
					for (int y = floor; y <= floor + r[4]; y++) {
						c.set(x, y, z, wall[(int) (c.rnd(x, y, z, 700) * wall.length) % wall.length]);
					}
				}
			}
		}
		for (int[] r : rooms) {
			for (int x = bx + r[0] + 1; x <= bx + r[2] - 1; x++) {
				for (int z = bz + r[1] + 1; z <= bz + r[3] - 1; z++) {
					c.set(x, floor, z, (x + z) % 2 == 0 ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.SMOOTH_STONE));
					for (int y = floor + 1; y < floor + r[4]; y++) {
						c.set(x, y, z, CAVE_AIR);
					}
					if ((x - bx) % 4 == 0 && (z - bz) % 4 == 0 && r[4] >= 6 && c.rnd(x, 0, z, 701) < 0.8F) {
						c.set(x, floor + r[4], z, light);
					}
				}
			}
		}
		// Durchbrüche zwischen Gängen und Räumen
		int[][] doors = {{22, 18}, {22, 15}, {26, 22}, {29, 22}, {22, 26}, {22, 29}, {18, 22}, {15, 22}};
		for (int[] d : doors) {
			for (int y = floor + 1; y <= floor + 3; y++) {
				for (int o = -1; o <= 1; o++) {
					boolean xWall = d[0] == 18 || d[0] == 15 || d[0] == 26 || d[0] == 29;
					int x = bx + d[0] + (xWall ? 0 : o);
					int z = bz + d[1] + (xWall ? o : 0);
					c.set(x, y, z, CAVE_AIR);
				}
			}
		}
		// Panzertür zum Tresor (Eisenblöcke als Rahmen)
		for (int y = floor + 1; y <= floor + 4; y++) {
			c.set(bx + 15, y, bz + 20, s(Blocks.IRON_BLOCK));
			c.set(bx + 15, y, bz + 24, s(Blocks.IRON_BLOCK));
		}

		// Schacht mit Leiter von der Hütte bis zur Halle
		int sx = bx + 22;
		int sz = bz + 22;
		for (int y = floor + 1; y <= surface; y++) {
			if (y > floor + 5) {
				for (int x = sx - 1; x <= sx + 1; x++) {
					for (int z = sz - 1; z <= sz + 1; z++) {
						c.set(x, y, z, (x == sx && z == sz) ? CAVE_AIR : s(Blocks.GRAY_CONCRETE));
					}
				}
			}
			c.set(sx, y, sz, Builders.ladder(Direction.SOUTH));
			c.set(sx, y, sz - 1, s(Blocks.GRAY_CONCRETE));
		}

		// Getarnte Hütte oben
		for (int x = sx - 3; x <= sx + 3; x++) {
			for (int z = sz - 3; z <= sz + 3; z++) {
				c.foundation(x, surface, z, DIRT);
				c.set(x, surface, z, x == sx && z == sz ? s(Blocks.SPRUCE_TRAPDOOR).with(TrapdoorBlock.HALF, BlockHalf.TOP).with(TrapdoorBlock.FACING, Direction.NORTH)
						: s(Blocks.SPRUCE_PLANKS));
				c.clear(x, z, surface + 1, surface + 6);
				boolean edge = Math.abs(x - sx) == 3 || Math.abs(z - sz) == 3;
				boolean corner = Math.abs(x - sx) == 3 && Math.abs(z - sz) == 3;
				int wallTop = surface + 3 - (x > sx ? 2 : 0);
				for (int y = surface + 1; y <= wallTop; y++) {
					if (corner) {
						c.set(x, y, z, s(Blocks.SPRUCE_LOG));
					} else if (edge && c.rnd(x, y, z, 702) < 0.85F) {
						c.set(x, y, z, s(Blocks.SPRUCE_PLANKS));
					}
				}
				if (x <= sx && c.rnd(x, 4, z, 703) < 0.8F) {
					c.set(x, surface + 4, z, s(Blocks.SPRUCE_SLAB));
				}
			}
		}
		c.set(sx, surface + 1, sz, Builders.MOSS_CARPET);
		c.set(sx, surface + 1, sz + 3, AIR);
		c.set(sx, surface + 2, sz + 3, AIR);
		c.set(sx - 2, surface + 1, sz - 2, s(Blocks.CRAFTING_TABLE));
		c.set(sx - 2, surface + 1, sz + 2, s(Blocks.BARREL));
		c.set(sx + 2, surface + 1, sz - 2, Builders.COBWEB);

		// Kommandozentrale: Monitorwand, Kartentisch, Konsolen
		for (int x = bx + 14; x <= bx + 30; x++) {
			for (int y = floor + 2; y <= floor + 5; y++) {
				boolean screen = (x - bx) % 3 != 1;
				c.set(x, y, bz + 4, screen ? s(Blocks.BLACK_STAINED_GLASS) : s(Blocks.POLISHED_BLACKSTONE));
				c.set(x, y, bz + 3, screen && c.rnd(x, y, 0, 704) < 0.6F ? s(Blocks.REDSTONE_LAMP).with(net.minecraft.block.RedstoneLampBlock.LIT, true) : s(Blocks.REDSTONE_BLOCK));
			}
			if ((x - bx) % 4 != 0) {
				c.set(x, floor + 1, bz + 6, s(Blocks.POLISHED_BLACKSTONE_SLAB).with(SlabBlock.TYPE, SlabType.TOP));
				c.set(x, floor + 1, bz + 8, s(Blocks.DARK_OAK_STAIRS).with(StairsBlock.FACING, Direction.SOUTH));
			}
		}
		c.set(bx + 22, floor + 1, bz + 11, s(Blocks.CARTOGRAPHY_TABLE));
		c.set(bx + 21, floor + 1, bz + 11, s(Blocks.CARTOGRAPHY_TABLE));
		c.chest(bx + 31, floor + 1, bz + 14, Direction.WEST, ModStructures.LOOT_SECRET);

		// Labor: Glasröhren mit lebenden Versuchsobjekten
		int[][] tubes = {{32, 15}, {35, 15}, {38, 15}, {32, 29}, {35, 29}, {38, 29}};
		for (int i = 0; i < tubes.length; i++) {
			int tx = bx + tubes[i][0];
			int tz = bz + tubes[i][1];
			for (int y = floor + 1; y <= floor + 4; y++) {
				for (int x = tx - 1; x <= tx + 1; x++) {
					for (int z = tz - 1; z <= tz + 1; z++) {
						boolean core = x == tx && z == tz;
						if (y == floor + 1 || y == floor + 4) {
							c.set(x, y, z, s(Blocks.IRON_BLOCK));
						} else if (!core) {
							c.set(x, y, z, i == 4 && c.rnd(x, y, z, 705) < 0.6F ? AIR : s(Blocks.LIGHT_BLUE_STAINED_GLASS));
						} else {
							c.set(x, y, z, CAVE_AIR);
						}
					}
				}
			}
			c.set(tx, floor + 1, tz, s(Blocks.SEA_LANTERN));
			if (i != 4) {
				var type = switch (i % 3) {
					case 0 -> ModEntities.WALKER;
					case 1 -> ModEntities.TOXIC;
					default -> ModEntities.SPITTER;
				};
				c.spawnMob(type, tx + 0.5, floor + 2, tz + 0.5);
			}
		}
		for (int x = bx + 31; x <= bx + 39; x += 2) {
			c.set(x, floor + 1, bz + 22, s(Blocks.BREWING_STAND));
			c.set(x + 1, floor + 1, bz + 22, s(Blocks.WHITE_CONCRETE));
		}
		c.set(bx + 40, floor + 1, bz + 20, s(Blocks.CAULDRON));
		c.chest(bx + 41, floor + 1, bz + 22, Direction.WEST, ModStructures.LOOT_SECRET_LAB);
		c.chest(bx + 41, floor + 1, bz + 24, Direction.WEST, ModStructures.LOOT_SECRET_LAB);

		// Verwahrung: aufgebrochener Käfig, Kratzspuren, Boss
		int ccx = bx + 22;
		int ccz = bz + 36;
		for (int x = ccx - 4; x <= ccx + 4; x++) {
			for (int z = ccz - 4; z <= ccz + 4; z++) {
				boolean ring = Math.abs(x - ccx) == 4 || Math.abs(z - ccz) == 4;
				if (!ring) {
					continue;
				}
				boolean bent = z == ccz - 4 && Math.abs(x - ccx) <= 1;
				for (int y = floor + 1; y <= floor + 7; y++) {
					if (!bent || y > floor + 5) {
						c.set(x, y, z, s(Blocks.IRON_BARS));
					}
				}
			}
		}
		for (int i = 0; i < 40; i++) {
			int x = bx + 13 + c.globalInt(720 + i, 19);
			int z = bz + 30 + c.globalInt(780 + i, 13);
			if (c.get(x, floor + 1, z).isAir()) {
				c.set(x, floor, z, c.global(840 + i) < 0.5F ? s(Blocks.REDSTONE_BLOCK) : s(Blocks.NETHER_WART_BLOCK));
			}
		}
		c.spawnMob(ModEntities.PATIENT_ZERO, ccx + 0.5, floor + 1, ccz + 0.5);

		// Tresor und Schlafraum
		for (int z = bz + 14; z <= bz + 30; z += 3) {
			c.chest(bx + 3, floor + 1, z, Direction.EAST, ModStructures.LOOT_SECRET);
			c.set(bx + 3, floor + 2, z, s(Blocks.IRON_BARS));
		}
		for (int z = bz + 14; z <= bz + 30; z += 2) {
			c.barrel(bx + 9, floor + 1, z, ModStructures.LOOT_MILITARY);
		}
		for (int z = bz + 15; z <= bz + 29; z += 4) {
			Builders.bed(c, bx + 13, floor + 1, z, Direction.WEST, s(Blocks.GREEN_BED));
		}
		c.armorStand(bx + 6, floor + 1, bz + 28, new ItemStack(ModItems.GAS_MASK), new ItemStack(ModItems.MILITARY_VEST));

		// Spinnweben, kaputte Lampen
		for (int[] r : rooms) {
			for (int x = bx + r[0] + 1; x <= bx + r[2] - 1; x++) {
				for (int z = bz + r[1] + 1; z <= bz + r[3] - 1; z++) {
					int y = floor + r[4] - 1;
					if (c.rnd(x, y, z, 706) < 0.05F && c.get(x, y, z).isAir()) {
						c.set(x, y, z, COBWEB);
					}
				}
			}
		}
		c.set(sx + 2, floor + 4, sz + 2, s(Blocks.LANTERN).with(LanternBlock.HANGING, true));
	}
}
