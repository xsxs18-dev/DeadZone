package com.kaan.deadzone.world;

import com.kaan.deadzone.world.BuildingPiece.Kind;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Direction;

/** Die eigentlichen Gebäude. Koordinaten sind immer absolut, Canvas schneidet auf den aktuellen Chunk zu. */
final class Builders {
	private Builders() {
	}

	// ------------------------------------------------------------------ Paletten

	static final BlockState AIR = Blocks.AIR.getDefaultState();
	static final BlockState CAVE_AIR = Blocks.CAVE_AIR.getDefaultState();
	static final BlockState DIRT = Blocks.DIRT.getDefaultState();
	static final BlockState STONE = Blocks.STONE.getDefaultState();
	static final BlockState COBWEB = Blocks.COBWEB.getDefaultState();
	static final BlockState MOSS_CARPET = Blocks.MOSS_CARPET.getDefaultState();

	static final BlockState[] RUBBLE = {
			Blocks.COBBLESTONE.getDefaultState(), Blocks.ANDESITE.getDefaultState(), Blocks.CRACKED_STONE_BRICKS.getDefaultState(),
			Blocks.MOSSY_COBBLESTONE.getDefaultState(), Blocks.STONE.getDefaultState(), Blocks.GRAY_CONCRETE.getDefaultState()
	};

	static final BlockState[] CAR_COLORS = {
			Blocks.RED_CONCRETE.getDefaultState(), Blocks.BLUE_CONCRETE.getDefaultState(), Blocks.WHITE_CONCRETE.getDefaultState(),
			Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(), Blocks.BLACK_CONCRETE.getDefaultState(), Blocks.YELLOW_CONCRETE.getDefaultState(),
			Blocks.GREEN_CONCRETE.getDefaultState(), Blocks.CYAN_CONCRETE.getDefaultState()
	};

	static void build(Kind kind, Canvas c, Direction facing, int flags) {
		boolean sidewalk = (flags & BuildingPiece.FLAG_SIDEWALK) != 0;
		switch (kind) {
			case ROAD_X -> road(c, true, (flags & BuildingPiece.FLAG_METRO) != 0);
			case ROAD_Z -> road(c, false, (flags & BuildingPiece.FLAG_METRO) != 0);
			case SKYSCRAPER -> lot(c, flags, facing, Style.SKYSCRAPER);
			case MEGA_TOWER -> lot(c, flags, facing, Style.MODERN);
			case APARTMENT -> lot(c, flags, facing, Style.APARTMENT);
			case SHOP -> lot(c, flags, facing, Style.SHOP);
			case HOSPITAL -> lot(c, flags, facing, Style.HOSPITAL);
			case POLICE -> lot(c, flags, facing, Style.POLICE);
			case SUPERMARKET, GAS_STATION, PARKING, PARK, CHECKPOINT, CRATER, FALLEN_TOWER, HIGHWAY_X, SECRET_BUNKER ->
					MegaBuilders.build(kind, c, facing, flags);
			case HOUSE -> house(c, sidewalk, facing);
			case RUBBLE -> rubbleLot(c, sidewalk);
			case WATCHTOWER -> watchtower(c, 12, true, ModStructures.LOOT_TOWER);
			case MIL_TOWER -> watchtower(c, 8, false, ModStructures.LOOT_MILITARY);
			case RADIO_TOWER -> radioTower(c);
			case BUNKER -> bunker(c);
			case MIL_GROUND -> militaryGround(c);
			case BARRACKS -> barracks(c, facing);
			case TENT -> tent(c);
			case HELIPAD -> helipad(c);
			case ARMORY -> armory(c, facing);
			case TANK -> tank(c);
			case TRUCK -> truck(c);
		}
	}

	// ------------------------------------------------------------------ Hilfen

	/** Boden der ganzen Box: Fundament auffüllen und Gelände darüber freiräumen. */
	static void prepareGround(Canvas c, int clearHeight, BlockState fill) {
		clearHeight = Math.max(clearHeight, 32);
		for (int x = c.x0; x <= c.x1; x++) {
			for (int z = c.z0; z <= c.z1; z++) {
				if (!c.columnInClip(x, z)) {
					continue;
				}
				c.foundation(x, c.y0, z, fill);
				c.clear(x, z, c.y0 + 1, c.y0 + clearHeight);
			}
		}
	}

	static BlockState ladder(Direction facing) {
		return Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, facing);
	}

	static void bed(Canvas c, int x, int y, int z, Direction headDir, BlockState bed) {
		c.set(x, y, z, bed.with(BedBlock.FACING, headDir).with(BedBlock.PART, BedPart.FOOT));
		c.set(x + headDir.getOffsetX(), y, z + headDir.getOffsetZ(), bed.with(BedBlock.FACING, headDir).with(BedBlock.PART, BedPart.HEAD));
	}

	static void door(Canvas c, int x, int y, int z, Direction facing, BlockState door) {
		c.set(x, y, z, door.with(DoorBlock.FACING, facing).with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		c.set(x, y + 1, z, door.with(DoorBlock.FACING, facing).with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	static void hangingLantern(Canvas c, int x, int y, int z) {
		c.set(x, y, z, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
	}

	/** Ein Autowrack, 2 breit und 4 lang. */
	static void car(Canvas c, int x, int y, int z, boolean alongX, int salt) {
		boolean burnt = c.rnd(x, y, z, salt) < 0.3F;
		BlockState body = burnt ? Blocks.BLACKSTONE.getDefaultState() : CAR_COLORS[(int) (c.rnd(x, y, z, salt + 1) * CAR_COLORS.length) % CAR_COLORS.length];
		BlockState glass = burnt ? AIR : Blocks.TINTED_GLASS.getDefaultState();
		BlockState wheel = Blocks.BLACK_CONCRETE.getDefaultState();
		for (int along = 0; along < 4; along++) {
			for (int across = 0; across < 2; across++) {
				int bx = alongX ? x + along : x + across;
				int bz = alongX ? z + across : z + along;
				boolean end = along == 0 || along == 3;
				c.set(bx, y, bz, end ? wheel : body);
				if (along == 1 || along == 2) {
					c.set(bx, y + 1, bz, along == 1 ? glass : body);
				} else {
					c.set(bx, y + 1, bz, Blocks.SMOOTH_STONE_SLAB.getDefaultState());
				}
			}
		}
		if (burnt && c.rnd(x, y, z, salt + 2) < 0.5F) {
			c.set(x, y + 2, z, Blocks.CAMPFIRE.getDefaultState().with(net.minecraft.block.CampfireBlock.LIT, false));
		}
	}

	private static void sidewalkRing(Canvas c, int inset) {
		for (int x = c.x0; x <= c.x1; x++) {
			for (int z = c.z0; z <= c.z1; z++) {
				if (!c.columnInClip(x, z)) {
					continue;
				}
				boolean ring = x < c.x0 + inset || x > c.x1 - inset || z < c.z0 + inset || z > c.z1 - inset;
				if (ring) {
					float r = c.rnd(x, c.y0, z, 11);
					BlockState s = r < 0.1F ? Blocks.CRACKED_STONE_BRICKS.getDefaultState()
							: r < 0.17F ? Blocks.MOSS_BLOCK.getDefaultState()
							: r < 0.6F ? Blocks.SMOOTH_STONE.getDefaultState() : Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
					c.set(x, c.y0, z, s);
					if (r > 0.97F) {
						c.set(x, c.y0 + 1, z, MOSS_CARPET);
					}
				}
			}
		}
	}

	static void overgrownGround(Canvas c, int ax, int az, int bx, int bz) {
		for (int x = ax; x <= bx; x++) {
			for (int z = az; z <= bz; z++) {
				if (!c.columnInClip(x, z)) {
					continue;
				}
				float r = c.rnd(x, c.y0, z, 12);
				c.set(x, c.y0, z, r < 0.25F ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
				if (r > 0.6F) {
					c.set(x, c.y0 + 1, z, r > 0.95F ? Blocks.TALL_GRASS.getDefaultState() : Blocks.SHORT_GRASS.getDefaultState());
					if (r > 0.95F) {
						c.set(x, c.y0 + 2, z, Blocks.TALL_GRASS.getDefaultState().with(net.minecraft.block.TallPlantBlock.HALF, DoubleBlockHalf.UPPER));
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------ Straßen

	private static void road(Canvas c, boolean alongX, boolean metro) {
		// Hoch genug freiräumen, damit keine Hügel über der Straße hängen bleiben
		prepareGround(c, metro ? 48 : 28, STONE);
		for (int x = c.x0; x <= c.x1; x++) {
			for (int z = c.z0; z <= c.z1; z++) {
				if (!c.columnInClip(x, z)) {
					continue;
				}
				int across = alongX ? z - c.z0 : x - c.x0;
				int along = alongX ? x - c.x0 : z - c.z0;
				float r = c.rnd(x, c.y0, z, 1);
				BlockState s;
				if (across == 0 || across == 6) {
					s = Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
				} else if (across == 3 && (along / 3) % 2 == 0 && r > 0.25F) {
					s = Blocks.YELLOW_TERRACOTTA.getDefaultState();
				} else if (r < 0.06F) {
					s = Blocks.COBBLED_DEEPSLATE.getDefaultState();
				} else if (r < 0.1F) {
					s = Blocks.ANDESITE.getDefaultState();
				} else if (r < 0.2F) {
					s = Blocks.POLISHED_BLACKSTONE.getDefaultState();
				} else {
					s = Blocks.BLACK_CONCRETE.getDefaultState();
				}
				c.set(x, c.y0, z, s);
				float d = c.rnd(x, c.y0 + 1, z, 2);
				if (d < 0.02F) {
					c.set(x, c.y0 + 1, z, MOSS_CARPET);
				} else if (d < 0.025F) {
					c.set(x, c.y0 + 1, z, RUBBLE[(int) (d * 1000) % RUBBLE.length]);
				}
			}
		}
		// Autowracks: alle 12 Blöcke eine Chance
		int length = alongX ? c.x1 - c.x0 + 1 : c.z1 - c.z0 + 1;
		for (int seg = 0; seg + 6 < length; seg += 12) {
			int sx = alongX ? c.x0 + seg + 3 : c.x0;
			int sz = alongX ? c.z0 : c.z0 + seg + 3;
			float r = c.rnd(sx, c.y0, sz, 3);
			if (r < 0.35F) {
				int lane = r < 0.17F ? 1 : 4;
				if (alongX) {
					car(c, sx, c.y0 + 1, c.z0 + lane, true, 40);
				} else {
					car(c, c.x0 + lane, c.y0 + 1, sz, false, 40);
				}
			}
		}
	}

	// ------------------------------------------------------------------ Mehrstöckige Gebäude

	enum Style {
		SKYSCRAPER, MODERN, APARTMENT, SHOP, HOSPITAL, POLICE
	}

	/** Zerstörungsgrad, steckt in den Bits 4-7 der Flags. */
	static final int INTACT = 0, DAMAGED = 1, HALF = 2, RUIN = 3, LEANING = 4;
	static final int LEAN_EXT = 10;

	record Rect(int x0, int z0, int x1, int z1) {
		boolean contains(int x, int z) {
			return x >= this.x0 && x <= this.x1 && z >= this.z0 && z <= this.z1;
		}

		Rect inset(int n) {
			return new Rect(this.x0 + n, this.z0 + n, this.x1 - n, this.z1 - n);
		}
	}

	static Rect fullRect(Canvas c) {
		return new Rect(c.x0, c.z0, c.x1, c.z1);
	}

	/** Grundstück ohne die Überhang-Zone eines schiefen Hochhauses. */
	static Rect lotRect(Canvas c, Direction facing, int state) {
		Rect r = fullRect(c);
		if (state != LEANING) {
			return r;
		}
		return switch (facing) {
			case NORTH -> new Rect(r.x0, r.z0 + LEAN_EXT, r.x1, r.z1);
			case SOUTH -> new Rect(r.x0, r.z0, r.x1, r.z1 - LEAN_EXT);
			case WEST -> new Rect(r.x0 + LEAN_EXT, r.z0, r.x1, r.z1);
			default -> new Rect(r.x0, r.z0, r.x1 - LEAN_EXT, r.z1);
		};
	}

	static void prepareGround(Canvas c, Rect r, int clearHeight, BlockState fill) {
		// mindestens 32 Blöcke freiräumen, sonst bleiben über niedrigen Gebäuden Erdüberhänge hängen
		clearHeight = Math.max(clearHeight, 32);
		for (int x = r.x0; x <= r.x1; x++) {
			for (int z = r.z0; z <= r.z1; z++) {
				if (!c.columnInClip(x, z)) {
					continue;
				}
				c.foundation(x, c.y0, z, fill);
				c.clear(x, z, c.y0 + 1, c.y0 + clearHeight);
			}
		}
	}

	static void sidewalk(Canvas c, Rect lot, int inset) {
		for (int x = lot.x0; x <= lot.x1; x++) {
			for (int z = lot.z0; z <= lot.z1; z++) {
				if (!c.columnInClip(x, z) || lot.inset(inset).contains(x, z)) {
					continue;
				}
				float r = c.rnd(x, c.y0, z, 11);
				BlockState s = r < 0.1F ? Blocks.CRACKED_STONE_BRICKS.getDefaultState()
						: r < 0.17F ? Blocks.MOSS_BLOCK.getDefaultState()
						: r < 0.6F ? Blocks.SMOOTH_STONE.getDefaultState() : Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
				c.set(x, c.y0, z, s);
				if (r > 0.97F) {
					c.set(x, c.y0 + 1, z, MOSS_CARPET);
				}
			}
		}
	}

	/** Schutthaufen: Kegel aus Trümmern, bleibt innerhalb von {@code area}. */
	static void rubblePile(Canvas c, int px, int pz, int radius, int height, Rect area, int salt) {
		for (int x = px - radius; x <= px + radius; x++) {
			for (int z = pz - radius; z <= pz + radius; z++) {
				if (!area.contains(x, z) || !c.columnInClip(x, z)) {
					continue;
				}
				double d = Math.sqrt((x - px) * (x - px) + (z - pz) * (z - pz)) / radius;
				int h = (int) Math.round((1 - d) * height + (c.rnd(x, 0, z, salt) - 0.5F) * 2);
				for (int y = 1; y <= h; y++) {
					if (c.get(x, c.y0 + y, z).isAir() && c.rnd(x, y, z, salt + 1) < 0.9F) {
						c.set(x, c.y0 + y, z, RUBBLE[(int) (c.rnd(x, y, z, salt + 2) * RUBBLE.length) % RUBBLE.length]);
					}
				}
			}
		}
	}

	/** Brennende Stelle: Lagerfeuer erzeugt eine weithin sichtbare Rauchsäule. */
	static void fire(Canvas c, int x, int y, int z) {
		c.set(x, y, z, Blocks.CAMPFIRE.getDefaultState().with(net.minecraft.block.CampfireBlock.LIT, true)
				.with(net.minecraft.block.CampfireBlock.SIGNAL_FIRE, true));
		c.set(x, y - 1, z, Blocks.HAY_BLOCK.getDefaultState());
	}

	static void lot(Canvas c, int flags, Direction facing, Style style) {
		boolean sidewalk = (flags & BuildingPiece.FLAG_SIDEWALK) != 0;
		int state = (flags >> 4) & 0xF;
		int floors = (c.y1 - c.y0 - 3) / 4;
		Rect lot = lotRect(c, facing, state);
		prepareGround(c, lot, c.y1 - c.y0, STONE);
		int inset = 2;
		if (sidewalk) {
			sidewalk(c, lot, inset);
		}
		int span = lot.x1 - lot.x0 + 1 - inset * 2;
		int depth = lot.z1 - lot.z0 + 1 - inset * 2;
		int w = switch (style) {
			case SKYSCRAPER, MODERN -> span - 2 - c.globalInt(20, 4);
			case HOSPITAL, POLICE -> span - 2;
			default -> 11 + c.globalInt(20, 6);
		};
		int d = switch (style) {
			case SKYSCRAPER, MODERN -> depth - 2 - c.globalInt(21, 4);
			case HOSPITAL, POLICE -> depth - 4;
			default -> 10 + c.globalInt(21, 6);
		};
		w = Math.max(7, Math.min(w, span));
		d = Math.max(7, Math.min(d, depth));
		int fx0 = lot.x0 + inset + (span - w) / 2;
		int fz0 = lot.z0 + inset + (depth - d) / 2;
		Rect fp = new Rect(fx0, fz0, fx0 + w - 1, fz0 + d - 1);
		if (sidewalk) {
			for (int x = lot.x0 + inset; x <= lot.x1 - inset; x++) {
				for (int z = lot.z0 + inset; z <= lot.z1 - inset; z++) {
					if (!fp.contains(x, z) && c.columnInClip(x, z)) {
						c.set(x, c.y0, z, c.rnd(x, 0, z, 13) < 0.5F ? Blocks.SMOOTH_STONE.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
					}
				}
			}
		}
		building(c, fp, lot, floors, facing, style, state);
	}

	static void building(Canvas c, Rect fp, Rect lot, int floors, Direction facing, Style style, int state) {
		int fx0 = fp.x0, fz0 = fp.z0, fx1 = fp.x1, fz1 = fp.z1;
		int y0 = c.y0;
		int height = floors * 4;
		BlockState frame;
		BlockState fill;
		BlockState floor;
		BlockState window = Blocks.GLASS_PANE.getDefaultState();
		int palette = c.globalInt(30, 4);
		RegistryKey<LootTable> loot = ModStructures.LOOT_CITY;
		switch (style) {
			case SKYSCRAPER -> {
				frame = palette % 2 == 0 ? Blocks.GRAY_CONCRETE.getDefaultState() : Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
				fill = palette < 2 ? Blocks.CYAN_TERRACOTTA.getDefaultState() : Blocks.LIGHT_GRAY_TERRACOTTA.getDefaultState();
				floor = Blocks.SMOOTH_STONE.getDefaultState();
				if (palette == 3) {
					window = Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState();
				}
			}
			case MODERN -> {
				frame = switch (palette) {
					case 0 -> Blocks.WHITE_CONCRETE.getDefaultState();
					case 1 -> Blocks.BLACK_CONCRETE.getDefaultState();
					case 2 -> Blocks.QUARTZ_BLOCK.getDefaultState();
					default -> Blocks.GRAY_CONCRETE.getDefaultState();
				};
				fill = Blocks.BLACK_CONCRETE.getDefaultState();
				floor = Blocks.POLISHED_ANDESITE.getDefaultState();
				window = switch (palette) {
					case 0 -> Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState();
					case 1 -> Blocks.GRAY_STAINED_GLASS_PANE.getDefaultState();
					case 2 -> Blocks.CYAN_STAINED_GLASS_PANE.getDefaultState();
					default -> Blocks.BLUE_STAINED_GLASS_PANE.getDefaultState();
				};
			}
			case APARTMENT -> {
				frame = palette == 0 ? Blocks.STONE_BRICKS.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
				fill = switch (palette) {
					case 0, 1 -> Blocks.BRICKS.getDefaultState();
					case 2 -> Blocks.WHITE_TERRACOTTA.getDefaultState();
					default -> Blocks.ORANGE_TERRACOTTA.getDefaultState();
				};
				floor = Blocks.SPRUCE_PLANKS.getDefaultState();
			}
			case HOSPITAL -> {
				frame = Blocks.WHITE_CONCRETE.getDefaultState();
				fill = Blocks.WHITE_TERRACOTTA.getDefaultState();
				floor = Blocks.SMOOTH_QUARTZ.getDefaultState();
				window = Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState();
				loot = ModStructures.LOOT_HOSPITAL;
			}
			case POLICE -> {
				frame = Blocks.BLUE_CONCRETE.getDefaultState();
				fill = Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
				floor = Blocks.POLISHED_ANDESITE.getDefaultState();
				loot = ModStructures.LOOT_POLICE;
			}
			default -> {
				frame = Blocks.POLISHED_ANDESITE.getDefaultState();
				fill = palette < 2 ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.BRICKS.getDefaultState();
				floor = Blocks.POLISHED_ANDESITE.getDefaultState();
			}
		}
		BlockState signColor = CAR_COLORS[c.globalInt(31, CAR_COLORS.length)];
		boolean tall = style == Style.SKYSCRAPER || style == Style.MODERN;
		int pillarStep = style == Style.MODERN ? 3 : 4;

		// Schiefer Turm: ab leanStart wandert jede Etage Richtung Straße
		int leanStart = y0 + 8 + c.globalInt(44, 8);
		int leanDx = facing.getOffsetX();
		int leanDz = facing.getOffsetZ();
		int leanRate = Math.max(3, (height - 8) / LEAN_EXT + 1);
		// Halb zerstört: schräge Abrisskante entlang einer Achse
		boolean halfAlongX = c.global(45) < 0.5F;
		boolean halfFlip = c.global(46) < 0.5F;
		float halfLow = 0.15F + c.global(47) * 0.25F;
		// Ruine: nur 1-3 Stockwerke stehen noch
		int ruinTop = y0 + 4 * (1 + c.globalInt(48, 3));

		float collapse = switch (state) {
			case INTACT -> 0.12F;
			case DAMAGED -> tall ? 0.5F + c.global(32) * 0.5F : c.global(32) * 0.6F;
			default -> 0.3F;
		};
		int maxDrop = tall ? 16 : 6;
		boolean hasHole = state == DAMAGED && c.global(33) < 0.6F && floors >= 3;
		int hx = fx0 + c.globalInt(34, fx1 - fx0 + 1);
		int hz = c.global(35) < 0.5F ? fz0 : fz1;
		int hy = y0 + 4 + c.globalInt(36, Math.max(1, height - 4));
		int hr = 3 + c.globalInt(37, tall ? 5 : 3);

		int ladderX = fx0 + 1;
		int ladderZ = fz0 + 2;
		int doorU = facing.getAxis() == Direction.Axis.X ? (fz0 + fz1) / 2 : (fx0 + fx1) / 2;

		for (int x = fx0; x <= fx1; x++) {
			for (int z = fz0; z <= fz1; z++) {
				c.foundation(x, y0, z, STONE);
				boolean edgeX = x == fx0 || x == fx1;
				boolean edgeZ = z == fz0 || z == fz1;
				boolean edge = edgeX || edgeZ;
				int u = edgeX ? z - fz0 : x - fx0;
				boolean pillar = (edgeX && edgeZ) || (edge && u % pillarStep == 0) || (x == fx0 && z == ladderZ);
				float n = c.noise(x, z, 5, 38);
				int drop = (int) (Math.max(0, n - (1 - collapse)) / Math.max(0.01F, collapse) * maxDrop);
				int top = y0 + height - drop;
				if (state == HALF) {
					float t = halfAlongX ? (float) (x - fx0) / Math.max(1, fx1 - fx0) : (float) (z - fz0) / Math.max(1, fz1 - fz0);
					if (halfFlip) {
						t = 1 - t;
					}
					top = Math.min(top, y0 + (int) (height * (halfLow + (1 - halfLow) * t)) + (int) ((n - 0.5F) * 6));
				} else if (state == RUIN) {
					top = Math.min(top, ruinTop + (int) ((n - 0.5F) * 6));
				}

				boolean isDoorSide = switch (facing) {
					case NORTH -> z == fz0;
					case SOUTH -> z == fz1;
					case WEST -> x == fx0;
					default -> x == fx1;
				};
				int doorAlong = facing.getAxis() == Direction.Axis.X ? z : x;

				for (int y = y0; y <= top; y++) {
					int ly = y - y0;
					int dx = x - hx;
					int dy = y - hy;
					int dz = z - hz;
					int off = state == LEANING && y > leanStart ? (y - leanStart) / leanRate : 0;
					int px = x + off * leanDx;
					int pz = z + off * leanDz;
					if (!c.columnInClip(px, pz)) {
						continue;
					}
					if (hasHole && y > y0 && dx * dx + dy * dy * 2 + dz * dz < hr * hr) {
						c.set(px, y, pz, AIR);
						continue;
					}
					boolean floorLevel = ly % 4 == 0;
					BlockState s;
					if (floorLevel) {
						if (edge) {
							s = frame;
						} else if (x == ladderX && z == ladderZ && y > y0 && off == 0) {
							s = ladder(Direction.EAST);
						} else {
							s = (y > y0 && c.rnd(x, y, z, 39) < (state == INTACT ? 0.01F : 0.06F)) ? AIR : floor;
						}
					} else if (edge) {
						int fl = ly % 4;
						if (pillar) {
							s = frame;
						} else if (isDoorSide && ly < 4 && Math.abs(doorAlong - doorU) <= 1) {
							s = AIR;
						} else {
							boolean isWindow = switch (style) {
								case SKYSCRAPER -> fl == 1 || fl == 2;
								case MODERN -> true;
								case APARTMENT, HOSPITAL, POLICE -> fl == 2 && u % 2 == 1;
								case SHOP -> ly < 4 ? fl >= 1 : (fl == 2 && u % 3 != 0);
							};
							float broken = state == INTACT ? 0.15F : 0.45F;
							if (style == Style.SHOP && ly == 3 && isDoorSide) {
								s = signColor;
							} else if (isWindow) {
								s = c.rnd(x, y, z, 40) < broken ? AIR : window;
							} else {
								s = c.rnd(x, y, z, 41) < 0.08F ? Blocks.CRACKED_STONE_BRICKS.getDefaultState() : fill;
							}
						}
					} else if (x == ladderX && z == ladderZ && off == 0) {
						s = ladder(Direction.EAST);
					} else {
						s = AIR;
					}
					c.set(px, y, pz, s);
				}
				if (!c.columnInClip(x, z)) {
					continue;
				}
				// Bruchkante oben zerklüften, offene Etagen bei halben Gebäuden
				if (top < y0 + height && edge && c.rnd(x, top, z, 42) < 0.45F && state != LEANING) {
					c.set(x, top + 1, z, RUBBLE[(int) (c.rnd(x, top, z, 43) * RUBBLE.length) % RUBBLE.length]);
				}
				// Flachdach mit Brüstung, wo das Dach noch steht
				if (top == y0 + height && edge && !tall && state != LEANING) {
					c.set(x, top + 1, z, frame);
				}
			}
		}

		// Trümmer neben eingestürzten Gebäuden
		if (state == HALF || state == RUIN || (state == DAMAGED && tall)) {
			int piles = state == RUIN ? 6 : 3;
			for (int i = 0; i < piles; i++) {
				int side = c.globalInt(60 + i, 4);
				int along = c.globalInt(70 + i, Math.max(1, (side < 2 ? fx1 - fx0 : fz1 - fz0)));
				int px = side == 0 ? fx0 + along : side == 1 ? fx0 + along : side == 2 ? fx0 - 1 : fx1 + 1;
				int pz = side == 0 ? fz0 - 1 : side == 1 ? fz1 + 1 : fz0 + along;
				int radius = state == RUIN ? 5 : 3 + c.globalInt(80 + i, 2);
				rubblePile(c, px, pz, radius, radius + 1, lot, 90 + i);
			}
			if (state == RUIN) {
				rubblePile(c, (fx0 + fx1) / 2, (fz0 + fz1) / 2, Math.max(4, (fx1 - fx0) / 2), 7, lot, 99);
			}
			if (c.global(49) < 0.45F) {
				int fxp = fx0 - 1 + c.globalInt(50, fx1 - fx0 + 2);
				int fzp = c.global(51) < 0.5F ? fz0 - 2 : fz1 + 2;
				if (lot.contains(fxp, fzp)) {
					fire(c, fxp, y0 + 1, fzp);
				}
			}
		}

		// Dachaufbauten auf intakten Hochhäusern
		if (tall && (state == INTACT || state == DAMAGED)) {
			roofTop(c, fp, y0 + height, style);
		}

		// Einrichtung pro Stockwerk (bei schiefen Türmen nur unterhalb der Neigung)
		for (int f = 0; f < floors; f++) {
			int fy = y0 + f * 4;
			if (state == LEANING && fy + 4 > leanStart) {
				break;
			}
			for (int x = fx0 + 1; x < fx1; x++) {
				for (int z = fz0 + 1; z < fz1; z++) {
					if (!c.columnInClip(x, z) || (x == ladderX && z == ladderZ)) {
						continue;
					}
					if (!c.get(x, fy, z).isOpaque() || !c.get(x, fy + 1, z).isAir()) {
						continue;
					}
					furnish(c, x, fy, z, style, fx0, fz0, fx1, fz1);
				}
			}
			int cx = fx1 - 1;
			int cz = fz1 - 1;
			if (c.rnd(cx, fy, cz, 50) < (style == Style.SHOP ? 0.9F : 0.55F) && c.inClip(cx, fy + 1, cz) && c.get(cx, fy, cz).isOpaque()) {
				c.chest(cx, fy + 1, cz, Direction.WEST, loot);
			}
		}

		// Fassaden-Schmuck
		if (style == Style.HOSPITAL) {
			MegaBuilders.redCross(c, fp, facing, y0 + height - 6);
		}
	}

	/** Wassertank, Antenne oder Helipad auf dem Dach. */
	private static void roofTop(Canvas c, Rect fp, int roofY, Style style) {
		int cx = (fp.x0 + fp.x1) / 2;
		int cz = (fp.z0 + fp.z1) / 2;
		int kind = c.globalInt(52, 3);
		if (kind == 0) {
			for (int y = roofY + 1; y <= roofY + 9; y++) {
				c.set(cx, y, cz, y % 3 == 0 ? Blocks.RED_CONCRETE.getDefaultState() : Blocks.IRON_BARS.getDefaultState());
			}
			c.set(cx, roofY + 10, cz, Blocks.LIGHTNING_ROD.getDefaultState());
		} else if (kind == 1) {
			for (int x = cx - 1; x <= cx + 1; x++) {
				for (int z = cz - 1; z <= cz + 1; z++) {
					c.set(x, roofY + 1, z, Blocks.OAK_FENCE.getDefaultState());
					c.set(x, roofY + 2, z, Blocks.SPRUCE_PLANKS.getDefaultState());
					c.set(x, roofY + 3, z, Blocks.SPRUCE_PLANKS.getDefaultState());
					c.set(x, roofY + 4, z, Blocks.SPRUCE_SLAB.getDefaultState());
				}
			}
		} else if (style == Style.MODERN) {
			for (int x = cx - 3; x <= cx + 3; x++) {
				for (int z = cz - 3; z <= cz + 3; z++) {
					boolean h = (Math.abs(x - cx) == 2 && Math.abs(z - cz) <= 2) || (z == cz && Math.abs(x - cx) <= 2);
					if (fp.contains(x, z)) {
						c.set(x, roofY, z, h ? Blocks.YELLOW_CONCRETE.getDefaultState() : Blocks.GRAY_CONCRETE.getDefaultState());
					}
				}
			}
		}
	}

	private static void furnish(Canvas c, int x, int fy, int z, Style style, int fx0, int fz0, int fx1, int fz1) {
		float r = c.rnd(x, fy, z, 60);
		boolean nearWall = x == fx0 + 1 || x == fx1 - 1 || z == fz0 + 1 || z == fz1 - 1;
		boolean corner = (x == fx0 + 1 || x == fx1 - 1) && (z == fz0 + 1 || z == fz1 - 1);
		if (corner && r < 0.5F) {
			c.set(x, fy + 3, z, COBWEB);
		}
		switch (style) {
			case SKYSCRAPER, MODERN, POLICE -> {
				if (r < 0.05F && !nearWall) {
					c.set(x, fy + 1, z, Blocks.SPRUCE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
				} else if (r < 0.07F && nearWall) {
					c.set(x, fy + 1, z, Blocks.BOOKSHELF.getDefaultState());
				} else if (r < 0.08F) {
					c.set(x, fy + 1, z, Blocks.POLISHED_ANDESITE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.fromHorizontalQuarterTurns((int) (r * 1000) % 4)));
				} else if (r < 0.1F) {
					c.set(x, fy + 1, z, RUBBLE[(int) (r * 997) % RUBBLE.length]);
				} else if (r < 0.105F) {
					c.barrel(x, fy + 1, z, style == Style.POLICE ? ModStructures.LOOT_POLICE : ModStructures.LOOT_CITY);
				}
			}
			case HOSPITAL -> {
				if (r < 0.03F && nearWall) {
					Direction dir = x == fx0 + 1 ? Direction.WEST : x == fx1 - 1 ? Direction.EAST : z == fz0 + 1 ? Direction.NORTH : Direction.SOUTH;
					int bx = x - dir.getOffsetX();
					int bz = z - dir.getOffsetZ();
					if (c.get(bx, fy + 1, bz).isAir() && c.get(bx, fy, bz).isOpaque()) {
						bed(c, bx, fy + 1, bz, dir, Blocks.WHITE_BED.getDefaultState());
					}
				} else if (r < 0.045F) {
					c.set(x, fy + 1, z, c.pick(x, fy, z, 62, Blocks.BREWING_STAND.getDefaultState(), Blocks.CAULDRON.getDefaultState(),
							Blocks.WHITE_CONCRETE.getDefaultState()));
				} else if (r < 0.05F) {
					c.barrel(x, fy + 1, z, ModStructures.LOOT_HOSPITAL);
				} else if (r < 0.06F) {
					c.set(x, fy + 1, z, Blocks.RED_CARPET.getDefaultState());
				}
			}
			case APARTMENT -> {
				if (r < 0.025F && nearWall) {
					Direction dir = x == fx0 + 1 ? Direction.WEST : x == fx1 - 1 ? Direction.EAST : z == fz0 + 1 ? Direction.NORTH : Direction.SOUTH;
					int bx = x - dir.getOffsetX();
					int bz = z - dir.getOffsetZ();
					if (c.get(bx, fy + 1, bz).isAir() && c.get(bx, fy, bz).isOpaque()) {
						bed(c, bx, fy + 1, bz, dir, (r < 0.012F ? Blocks.RED_BED : Blocks.LIGHT_GRAY_BED).getDefaultState());
					}
				} else if (r < 0.04F && nearWall) {
					c.set(x, fy + 1, z, c.pick(x, fy, z, 61, Blocks.CRAFTING_TABLE.getDefaultState(), Blocks.FURNACE.getDefaultState(),
							Blocks.CAULDRON.getDefaultState(), Blocks.BOOKSHELF.getDefaultState(), Blocks.SMOKER.getDefaultState()));
				} else if (r < 0.06F) {
					c.set(x, fy + 1, z, Blocks.BROWN_CARPET.getDefaultState());
				} else if (r < 0.075F) {
					c.set(x, fy + 1, z, RUBBLE[(int) (r * 997) % RUBBLE.length]);
				}
			}
			case SHOP -> {
				int lx = x - fx0;
				if (fy == c.y0 && lx % 3 == 0 && !nearWall && r < 0.85F) {
					c.set(x, fy + 1, z, r < 0.08F ? Blocks.BARREL.getDefaultState() : Blocks.SPRUCE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
					if (r < 0.5F) {
						c.set(x, fy + 2, z, Blocks.SPRUCE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
					}
				} else if (r < 0.03F) {
					c.barrel(x, fy + 1, z, ModStructures.LOOT_CITY);
				} else if (r < 0.06F) {
					c.set(x, fy + 1, z, RUBBLE[(int) (r * 997) % RUBBLE.length]);
				}
			}
		}
	}

	// ------------------------------------------------------------------ Haus

	private static void house(Canvas c, boolean sidewalk, Direction facing) {
		prepareGround(c, c.y1 - c.y0, DIRT);
		int inset = sidewalk ? 2 : 0;
		if (sidewalk) {
			sidewalkRing(c, inset);
		}
		overgrownGround(c, c.x0 + inset, c.z0 + inset, c.x1 - inset, c.z1 - inset);

		int cx = (c.x0 + c.x1) / 2;
		int cz = (c.z0 + c.z1) / 2;
		int hx0 = cx - 4;
		int hx1 = cx + 4;
		int hz0 = cz - 5;
		int hz1 = cz + 5;
		int y0 = c.y0;

		int palette = c.globalInt(70, 4);
		BlockState wall = switch (palette) {
			case 0 -> Blocks.WHITE_CONCRETE.getDefaultState();
			case 1 -> Blocks.OAK_PLANKS.getDefaultState();
			case 2 -> Blocks.BRICKS.getDefaultState();
			default -> Blocks.STRIPPED_BIRCH_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y);
		};
		BlockState post = Blocks.DARK_OAK_LOG.getDefaultState();
		BlockState roof = palette == 2 ? Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState() : Blocks.DARK_OAK_STAIRS.getDefaultState();
		BlockState gable = palette == 2 ? Blocks.BRICKS.getDefaultState() : Blocks.DARK_OAK_PLANKS.getDefaultState();
		int ladderX = hx0 + 1;
		int ladderZ = hz1 - 1;

		for (int x = hx0; x <= hx1; x++) {
			for (int z = hz0; z <= hz1; z++) {
				if (!c.columnInClip(x, z)) {
					continue;
				}
				c.foundation(x, y0, z, Blocks.COBBLESTONE.getDefaultState());
				boolean edgeX = x == hx0 || x == hx1;
				boolean edgeZ = z == hz0 || z == hz1;
				boolean edge = edgeX || edgeZ;
				int u = edgeX ? z - hz0 : x - hx0;
				c.set(x, y0, z, Blocks.OAK_PLANKS.getDefaultState());
				for (int y = y0 + 1; y <= y0 + 7; y++) {
					int ly = y - y0;
					BlockState s = AIR;
					if (ly == 4 && !edge) {
						s = (x == ladderX && z == ladderZ) ? ladder(Direction.EAST) : (c.rnd(x, y, z, 71) < 0.08F ? AIR : Blocks.SPRUCE_PLANKS.getDefaultState());
					} else if (edgeX && edgeZ) {
						s = post;
					} else if (edge) {
						boolean window = (ly == 2 || ly == 6) && u % 3 == 1;
						s = window ? (c.rnd(x, y, z, 72) < 0.35F ? AIR : Blocks.GLASS_PANE.getDefaultState()) : wall;
						if (ly == 4) {
							s = post;
						}
					} else if (x == ladderX && z == ladderZ && ly < 4) {
						s = ladder(Direction.EAST);
					}
					if (edge && c.rnd(x, y, z, 73) < 0.04F && ly > 1) {
						s = AIR;
					}
					c.set(x, y, z, s);
				}
			}
		}
		// Tür
		int dx = switch (facing) {
			case EAST -> hx1;
			case WEST -> hx0;
			default -> cx;
		};
		int dz = switch (facing) {
			case NORTH -> hz0;
			case SOUTH -> hz1;
			default -> cz;
		};
		if (c.global(74) < 0.6F) {
			door(c, dx, y0 + 1, dz, facing, Blocks.OAK_DOOR.getDefaultState());
		} else {
			c.set(dx, y0 + 1, dz, AIR);
			c.set(dx, y0 + 2, dz, AIR);
		}

		// Satteldach entlang Z, mit Löchern
		for (int i = 0; i <= 5; i++) {
			int y = y0 + 8 + i;
			int left = hx0 - 1 + i;
			int right = hx1 + 1 - i;
			for (int z = hz0 - 1; z <= hz1 + 1; z++) {
				boolean hole = c.rnd(left, y, z, 75) < 0.12F;
				if (left < right) {
					if (!hole) {
						c.set(left, y, z, roof.with(StairsBlock.FACING, Direction.EAST));
					}
					if (c.rnd(right, y, z, 76) >= 0.12F) {
						c.set(right, y, z, roof.with(StairsBlock.FACING, Direction.WEST));
					}
				} else if (left == right && !hole) {
					c.set(left, y, z, Blocks.DARK_OAK_SLAB.getDefaultState());
				}
			}
			for (int x = left + 1; x < right; x++) {
				c.set(x, y, hz0, gable);
				c.set(x, y, hz1, gable);
			}
		}

		// Einrichtung
		c.set(hx1 - 1, y0 + 1, hz0 + 1, Blocks.CRAFTING_TABLE.getDefaultState());
		c.set(hx1 - 1, y0 + 1, hz0 + 2, Blocks.FURNACE.getDefaultState());
		c.set(hx1 - 1, y0 + 1, hz0 + 3, Blocks.CAULDRON.getDefaultState());
		c.chest(hx1 - 1, y0 + 1, hz0 + 5, Direction.WEST, ModStructures.LOOT_HOUSE);
		c.set(hx0 + 3, y0 + 1, hz0 + 3, Blocks.OAK_FENCE.getDefaultState());
		c.set(hx0 + 3, y0 + 2, hz0 + 3, Blocks.OAK_PRESSURE_PLATE.getDefaultState());
		bed(c, hx1 - 2, y0 + 5, hz1 - 2, Direction.EAST, Blocks.WHITE_BED.getDefaultState());
		bed(c, hx1 - 2, y0 + 5, hz1 - 4, Direction.EAST, Blocks.BLUE_BED.getDefaultState());
		if (c.global(77) < 0.6F) {
			c.chest(hx0 + 1, y0 + 5, hz0 + 1, Direction.EAST, ModStructures.LOOT_HOUSE);
		}
		c.set(hx0 + 2, y0 + 5, hz0 + 1, Blocks.BOOKSHELF.getDefaultState());
		c.set(hx0 + 1, y0 + 3, hz0 + 1, COBWEB);
		c.set(hx1 - 1, y0 + 7, hz1 - 1, COBWEB);

		// Autowrack in der Einfahrt
		if (c.global(78) < 0.5F) {
			int carX = facing == Direction.EAST ? hx1 + 2 : facing == Direction.WEST ? hx0 - 3 : hx1 + 2;
			car(c, Math.min(carX, c.x1 - 2), y0 + 1, hz0, false, 79);
		}
		// Kaputter Gartenzaun (nur außerhalb der Stadt)
		if (!sidewalk) {
			for (int x = c.x0; x <= c.x1; x++) {
				for (int z = c.z0; z <= c.z1; z++) {
					boolean ring = x == c.x0 || x == c.x1 || z == c.z0 || z == c.z1;
					if (ring && c.rnd(x, y0, z, 80) < 0.55F) {
						c.set(x, y0 + 1, z, Blocks.OAK_FENCE.getDefaultState());
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------ Schutt-Grundstück

	private static void rubbleLot(Canvas c, boolean sidewalk) {
		prepareGround(c, c.y1 - c.y0, DIRT);
		int inset = sidewalk ? 2 : 0;
		if (sidewalk) {
			sidewalkRing(c, inset);
		}
		overgrownGround(c, c.x0 + inset, c.z0 + inset, c.x1 - inset, c.z1 - inset);
		for (int p = 0; p < 4; p++) {
			int px = c.x0 + 4 + c.globalInt(90 + p, c.x1 - c.x0 - 7);
			int pz = c.z0 + 4 + c.globalInt(95 + p, c.z1 - c.z0 - 7);
			int radius = 2 + c.globalInt(100 + p, 3);
			for (int x = px - radius; x <= px + radius; x++) {
				for (int z = pz - radius; z <= pz + radius; z++) {
					int h = radius - (int) Math.sqrt((x - px) * (x - px) + (z - pz) * (z - pz));
					for (int y = 1; y <= h; y++) {
						if (c.rnd(x, y, z, 101) < 0.85F) {
							c.set(x, c.y0 + y, z, RUBBLE[(int) (c.rnd(x, y, z, 102) * RUBBLE.length) % RUBBLE.length]);
						}
					}
				}
			}
		}
		// Toter Baum
		int tx = c.x0 + 5 + c.globalInt(110, 8);
		int tz = c.z0 + 5 + c.globalInt(111, 8);
		int th = 4 + c.globalInt(112, 3);
		for (int y = 1; y <= th; y++) {
			c.set(tx, c.y0 + y, tz, Blocks.DARK_OAK_LOG.getDefaultState());
		}
		c.set(tx + 1, c.y0 + th - 1, tz, Blocks.DARK_OAK_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.X));
		c.set(tx - 1, c.y0 + th, tz, Blocks.DARK_OAK_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.X));
		c.set(tx, c.y0 + th - 2, tz + 1, Blocks.DARK_OAK_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Z));
		car(c, c.x1 - 6, c.y0 + 1, c.z0 + 4, false, 113);
	}

	// ------------------------------------------------------------------ Wachturm

	private static void watchtower(Canvas c, int platformHeight, boolean roofed, RegistryKey<LootTable> loot) {
		prepareGround(c, c.y1 - c.y0, DIRT);
		int cx = (c.x0 + c.x1) / 2;
		int cz = (c.z0 + c.z1) / 2;
		int y0 = c.y0;
		int py = y0 + platformHeight;
		BlockState log = Blocks.SPRUCE_LOG.getDefaultState();
		BlockState planks = Blocks.SPRUCE_PLANKS.getDefaultState();
		BlockState fence = Blocks.SPRUCE_FENCE.getDefaultState();
		int[][] posts = {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}};
		for (int[] p : posts) {
			c.foundation(cx + p[0], y0, cz + p[1], Blocks.COBBLESTONE.getDefaultState());
			c.set(cx + p[0], y0, cz + p[1], Blocks.COBBLESTONE.getDefaultState());
			for (int y = y0 + 1; y <= py + (roofed ? 3 : 0); y++) {
				c.set(cx + p[0], y, cz + p[1], log);
			}
		}
		// Querstreben
		for (int y = y0 + 3; y < py; y += 4) {
			for (int i = -1; i <= 1; i++) {
				if (c.rnd(cx + i, y, cz, 120) < 0.8F) {
					c.set(cx + i, y, cz - 2, fence);
					c.set(cx + i, y, cz + 2, fence);
					c.set(cx - 2, y, cz + i, fence);
					c.set(cx + 2, y, cz + i, fence);
				}
			}
		}
		// Leiter am Pfosten
		for (int y = y0 + 1; y <= py; y++) {
			c.set(cx - 1, y, cz - 2, ladder(Direction.EAST));
		}
		// Plattform
		for (int x = cx - 3; x <= cx + 3; x++) {
			for (int z = cz - 3; z <= cz + 3; z++) {
				if (x == cx - 1 && z == cz - 2) {
					continue;
				}
				boolean post = Math.abs(x - cx) == 2 && Math.abs(z - cz) == 2;
				if (!post) {
					c.set(x, py, z, c.rnd(x, py, z, 121) < 0.07F ? AIR : planks);
				}
				boolean rim = Math.abs(x - cx) == 3 || Math.abs(z - cz) == 3;
				if (rim && c.rnd(x, py + 1, z, 122) < 0.8F) {
					c.set(x, py + 1, z, fence);
				}
			}
		}
		c.chest(cx + 1, py + 1, cz + 1, Direction.NORTH, loot);
		if (roofed) {
			for (int x = cx - 3; x <= cx + 3; x++) {
				for (int z = cz - 3; z <= cz + 3; z++) {
					if (c.rnd(x, py + 4, z, 123) < 0.85F) {
						c.set(x, py + 4, z, Blocks.SPRUCE_SLAB.getDefaultState());
					}
				}
			}
		}
		// Sandsäcke um den Fuß
		for (int x = cx - 4; x <= cx + 4; x++) {
			for (int z = cz - 4; z <= cz + 4; z++) {
				boolean ring = Math.abs(x - cx) == 4 || Math.abs(z - cz) == 4;
				if (ring && c.rnd(x, y0, z, 124) < 0.5F) {
					c.set(x, y0 + 1, z, Blocks.PACKED_MUD.getDefaultState());
				}
			}
		}
	}

	// ------------------------------------------------------------------ Funkturm

	private static void radioTower(Canvas c) {
		prepareGround(c, c.y1 - c.y0, DIRT);
		int cx = c.x0 + 4;
		int cz = c.z0 + 4;
		int y0 = c.y0;
		int top = c.y1 - 3;
		for (int x = cx - 1; x <= cx + 1; x++) {
			for (int z = cz - 1; z <= cz + 1; z++) {
				c.foundation(x, y0, z, STONE);
				c.set(x, y0, z, Blocks.SMOOTH_STONE.getDefaultState());
			}
		}
		for (int y = y0 + 1; y <= top; y++) {
			boolean red = ((y - y0) / 5) % 2 == 0;
			c.set(cx, y, cz, red ? Blocks.RED_CONCRETE.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState());
			c.set(cx + 1, y, cz, ladder(Direction.EAST));
			int[][] corners = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
			for (int[] k : corners) {
				if (c.rnd(cx + k[0], y, cz + k[1], 130) < 0.92F) {
					c.set(cx + k[0], y, cz + k[1], Blocks.IRON_BARS.getDefaultState());
				}
			}
			if ((y - y0) % 6 == 0) {
				c.set(cx, y, cz - 1, Blocks.IRON_BARS.getDefaultState());
				c.set(cx - 1, y, cz, Blocks.IRON_BARS.getDefaultState());
				c.set(cx, y, cz + 1, Blocks.IRON_BARS.getDefaultState());
			}
		}
		for (int x = cx - 1; x <= cx + 1; x++) {
			for (int z = cz - 1; z <= cz + 1; z++) {
				c.set(x, top + 1, z, Blocks.SMOOTH_STONE_SLAB.getDefaultState());
			}
		}
		c.set(cx + 1, top + 1, cz, AIR);
		c.chest(cx - 1, top + 2, cz - 1, Direction.SOUTH, ModStructures.LOOT_TOWER);
		c.set(cx, top + 2, cz, Blocks.LIGHTNING_ROD.getDefaultState());
		c.set(cx, top + 2, cz + 1, Blocks.RED_CONCRETE.getDefaultState());

		// Kleine Funkhütte
		int sx0 = c.x0 + 6;
		int sz0 = c.z0;
		for (int x = sx0; x <= sx0 + 2; x++) {
			for (int z = sz0; z <= sz0 + 3; z++) {
				c.foundation(x, y0, z, STONE);
				c.set(x, y0, z, Blocks.SMOOTH_STONE.getDefaultState());
				boolean edge = x == sx0 || x == sx0 + 2 || z == sz0 || z == sz0 + 3;
				for (int y = y0 + 1; y <= y0 + 3; y++) {
					c.set(x, y, z, edge ? Blocks.LIGHT_GRAY_CONCRETE.getDefaultState() : AIR);
				}
				c.set(x, y0 + 4, z, Blocks.SMOOTH_STONE_SLAB.getDefaultState());
			}
		}
		c.set(sx0 + 1, y0 + 1, sz0 + 3, AIR);
		c.set(sx0 + 1, y0 + 2, sz0 + 3, AIR);
		c.chest(sx0 + 1, y0 + 1, sz0 + 1, Direction.SOUTH, ModStructures.LOOT_TOWER);
	}

	// ------------------------------------------------------------------ Bunker

	private static void bunker(Canvas c) {
		int y0 = c.y0;
		int surface = c.y1 - 4;
		BlockState[] walls = {
				Blocks.GRAY_CONCRETE.getDefaultState(), Blocks.POLISHED_ANDESITE.getDefaultState(),
				Blocks.CRACKED_STONE_BRICKS.getDefaultState(), Blocks.LIGHT_GRAY_CONCRETE.getDefaultState()
		};
		int bx = c.x0;
		int bz = c.z0;

		// Räume: {x0, z0, x1, z1} relativ
		int[][] rooms = {
				{0, 6, 6, 14},     // Schacht-Halle
				{6, 8, 26, 12},    // Hauptgang
				{7, 1, 14, 8},     // Lager
				{16, 1, 23, 8},    // Schlafraum
				{7, 12, 14, 19},   // Labor
				{16, 12, 23, 19}   // Waffenkammer
		};
		for (int[] room : rooms) {
			for (int x = bx + room[0]; x <= bx + room[2]; x++) {
				for (int z = bz + room[1]; z <= bz + room[3]; z++) {
					for (int y = y0; y <= y0 + 5; y++) {
						c.set(x, y, z, walls[(int) (c.rnd(x, y, z, 140) * walls.length) % walls.length]);
					}
				}
			}
		}
		for (int[] room : rooms) {
			for (int x = bx + room[0] + 1; x <= bx + room[2] - 1; x++) {
				for (int z = bz + room[1] + 1; z <= bz + room[3] - 1; z++) {
					c.set(x, y0, z, c.rnd(x, y0, z, 141) < 0.15F ? Blocks.CRACKED_DEEPSLATE_TILES.getDefaultState() : Blocks.POLISHED_DEEPSLATE.getDefaultState());
					for (int y = y0 + 1; y <= y0 + 4; y++) {
						c.set(x, y, z, CAVE_AIR);
					}
				}
			}
		}
		// Durchgänge Halle -> Gang und Gang -> Räume
		for (int z = bz + 9; z <= bz + 11; z++) {
			for (int y = y0 + 1; y <= y0 + 3; y++) {
				c.set(bx + 6, y, z, CAVE_AIR);
			}
		}
		int[] doorX = {bx + 10, bx + 19};
		for (int dx : doorX) {
			for (int x = dx; x <= dx + 1; x++) {
				for (int y = y0 + 1; y <= y0 + 3; y++) {
					c.set(x, y, bz + 8, CAVE_AIR);
					c.set(x, y, bz + 12, CAVE_AIR);
				}
			}
		}
		// Licht: hängende Laternen, manche kaputt
		for (int x = bx + 8; x <= bx + 24; x += 4) {
			if (c.rnd(x, y0, bz, 142) < 0.6F) {
				hangingLantern(c, x, y0 + 4, bz + 10);
			}
		}
		hangingLantern(c, bx + 3, y0 + 4, bz + 8);

		// Schacht nach oben mit Leiter
		int sx = bx + 3;
		int sz = bz + 10;
		for (int y = y0 + 1; y <= surface; y++) {
			for (int x = sx - 1; x <= sx + 1; x++) {
				for (int z = sz - 1; z <= sz + 1; z++) {
					if (y > y0 + 4) {
						c.set(x, y, z, (x == sx && z == sz) ? CAVE_AIR : Blocks.GRAY_CONCRETE.getDefaultState());
					}
				}
			}
			c.set(sx, y, sz, ladder(Direction.SOUTH));
			c.set(sx, y, sz - 1, Blocks.GRAY_CONCRETE.getDefaultState());
		}
		// Eingangshütte oben
		for (int x = sx - 2; x <= sx + 2; x++) {
			for (int z = sz - 2; z <= sz + 2; z++) {
				c.foundation(x, surface, z, STONE);
				if (!(x == sx && z == sz)) {
					c.set(x, surface, z, Blocks.GRAY_CONCRETE.getDefaultState());
				}
				boolean edge = Math.abs(x - sx) == 2 || Math.abs(z - sz) == 2;
				for (int y = surface + 1; y <= surface + 3; y++) {
					c.set(x, y, z, edge ? Blocks.LIGHT_GRAY_CONCRETE.getDefaultState() : AIR);
				}
				c.set(x, surface + 4, z, Blocks.SMOOTH_STONE_SLAB.getDefaultState());
			}
		}
		c.set(sx + 2, surface + 1, sz, AIR);
		c.set(sx + 2, surface + 2, sz, AIR);
		c.set(sx, surface + 1, sz, Blocks.IRON_TRAPDOOR.getDefaultState()
				.with(net.minecraft.block.TrapdoorBlock.OPEN, true)
				.with(net.minecraft.block.TrapdoorBlock.FACING, Direction.SOUTH));

		// Lager
		c.chest(bx + 8, y0 + 1, bz + 2, Direction.SOUTH, ModStructures.LOOT_BUNKER);
		c.chest(bx + 10, y0 + 1, bz + 2, Direction.SOUTH, ModStructures.LOOT_BUNKER);
		c.barrel(bx + 13, y0 + 1, bz + 2, ModStructures.LOOT_BUNKER);
		c.set(bx + 13, y0 + 1, bz + 3, Blocks.BARREL.getDefaultState());
		c.set(bx + 12, y0 + 1, bz + 2, Blocks.BARREL.getDefaultState());
		// Schlafraum (Stockbetten)
		for (int i = 0; i < 3; i++) {
			bed(c, bx + 17 + i * 2, y0 + 1, bz + 2, Direction.NORTH, Blocks.GREEN_BED.getDefaultState());
			c.set(bx + 17 + i * 2, y0 + 2, bz + 2, Blocks.SPRUCE_SLAB.getDefaultState());
		}
		c.chest(bx + 22, y0 + 1, bz + 6, Direction.WEST, ModStructures.LOOT_BUNKER);
		// Labor
		c.set(bx + 8, y0 + 1, bz + 18, Blocks.BREWING_STAND.getDefaultState());
		c.set(bx + 9, y0 + 1, bz + 18, Blocks.CAULDRON.getDefaultState());
		c.set(bx + 10, y0 + 1, bz + 18, Blocks.WHITE_CONCRETE.getDefaultState());
		c.set(bx + 11, y0 + 1, bz + 18, Blocks.BREWING_STAND.getDefaultState());
		c.chest(bx + 13, y0 + 1, bz + 17, Direction.WEST, ModStructures.LOOT_BUNKER);
		// Waffenkammer
		for (int x = bx + 17; x <= bx + 22; x++) {
			c.set(x, y0 + 1, bz + 15, Blocks.IRON_BARS.getDefaultState());
			c.set(x, y0 + 2, bz + 15, Blocks.IRON_BARS.getDefaultState());
		}
		c.set(bx + 20, y0 + 1, bz + 15, CAVE_AIR);
		c.set(bx + 20, y0 + 2, bz + 15, CAVE_AIR);
		c.chest(bx + 18, y0 + 1, bz + 18, Direction.NORTH, ModStructures.LOOT_MILITARY);
		c.chest(bx + 21, y0 + 1, bz + 18, Direction.NORTH, ModStructures.LOOT_MILITARY);
		// Spinnweben
		for (int[] room : rooms) {
			for (int x = bx + room[0] + 1; x <= bx + room[2] - 1; x++) {
				for (int z = bz + room[1] + 1; z <= bz + room[3] - 1; z++) {
					if (c.rnd(x, y0 + 4, z, 143) < 0.06F && c.get(x, y0 + 4, z).isAir()) {
						c.set(x, y0 + 4, z, COBWEB);
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------ Militär

	private static void militaryGround(Canvas c) {
		prepareGround(c, 4, DIRT);
		int gateMid = (c.x0 + c.x1) / 2;
		for (int x = c.x0; x <= c.x1; x++) {
			for (int z = c.z0; z <= c.z1; z++) {
				if (!c.columnInClip(x, z)) {
					continue;
				}
				float r = c.rnd(x, c.y0, z, 150);
				// Kein Gras oder Erde: sonst wachsen später Bäume mitten in der Basis
				c.set(x, c.y0, z, r < 0.5F ? Blocks.PACKED_MUD.getDefaultState() : r < 0.75F ? Blocks.ANDESITE.getDefaultState()
						: r < 0.9F ? Blocks.STONE.getDefaultState() : Blocks.COBBLESTONE.getDefaultState());
				boolean ring = x == c.x0 || x == c.x1 || z == c.z0 || z == c.z1;
				if (!ring) {
					continue;
				}
				boolean gate = z == c.z1 && Math.abs(x - gateMid) <= 2;
				boolean breach = c.noise(x, z, 3, 151) > 0.82F;
				if (gate || breach) {
					continue;
				}
				c.set(x, c.y0 + 1, z, c.rnd(x, 1, z, 152) < 0.3F ? Blocks.CRACKED_STONE_BRICKS.getDefaultState() : Blocks.STONE_BRICKS.getDefaultState());
				c.set(x, c.y0 + 2, z, c.rnd(x, 2, z, 153) < 0.3F ? Blocks.MOSSY_STONE_BRICKS.getDefaultState() : Blocks.STONE_BRICKS.getDefaultState());
				if (c.rnd(x, 3, z, 154) < 0.85F) {
					c.set(x, c.y0 + 3, z, Blocks.IRON_BARS.getDefaultState());
				}
			}
		}
		// Sandsack-Stellung am Tor
		for (int x = gateMid - 5; x <= gateMid + 5; x++) {
			if (Math.abs(x - gateMid) > 2) {
				c.set(x, c.y0 + 1, c.z1 - 3, Blocks.PACKED_MUD.getDefaultState());
			}
		}
		for (int i = -1; i <= 1; i++) {
			c.set(gateMid + i, c.y0 + 1, c.z1 - 6, Blocks.PACKED_MUD.getDefaultState());
		}
	}

	private static void barracks(Canvas c, Direction facing) {
		prepareGround(c, 6, DIRT);
		for (int x = c.x0; x <= c.x1; x++) {
			for (int z = c.z0; z <= c.z1; z++) {
				if (!c.columnInClip(x, z)) {
					continue;
				}
				boolean edge = x == c.x0 || x == c.x1 || z == c.z0 || z == c.z1;
				c.set(x, c.y0, z, Blocks.SMOOTH_STONE.getDefaultState());
				for (int y = c.y0 + 1; y <= c.y0 + 4; y++) {
					BlockState s = AIR;
					if (edge) {
						int u = (x == c.x0 || x == c.x1) ? z - c.z0 : x - c.x0;
						boolean window = (y == c.y0 + 2 || y == c.y0 + 3) && u % 3 == 1;
						s = window ? (c.rnd(x, y, z, 160) < 0.4F ? AIR : Blocks.GLASS_PANE.getDefaultState())
								: c.rnd(x, y, z, 161) < 0.5F ? Blocks.GREEN_TERRACOTTA.getDefaultState() : Blocks.GREEN_CONCRETE.getDefaultState();
					}
					c.set(x, y, z, s);
				}
				if (c.noise(x, z, 3, 162) < 0.75F) {
					c.set(x, c.y0 + 5, z, Blocks.SMOOTH_STONE_SLAB.getDefaultState());
				}
			}
		}
		int mz = (c.z0 + c.z1) / 2;
		int doorX = facing == Direction.WEST ? c.x0 : c.x1;
		c.set(doorX, c.y0 + 1, mz, AIR);
		c.set(doorX, c.y0 + 2, mz, AIR);
		for (int z = c.z0 + 1; z <= c.z1 - 2; z += 2) {
			if (Math.abs(z - mz) > 1) {
				bed(c, c.x0 + 1, c.y0 + 1, z, Direction.WEST, Blocks.GREEN_BED.getDefaultState());
			}
		}
		c.chest(c.x1 - 1, c.y0 + 1, c.z0 + 1, Direction.WEST, ModStructures.LOOT_MILITARY);
		if (c.global(163) < 0.5F) {
			c.chest(c.x1 - 1, c.y0 + 1, c.z1 - 1, Direction.WEST, ModStructures.LOOT_MILITARY);
		}
		c.set(c.x1 - 1, c.y0 + 1, c.z0 + 3, Blocks.CRAFTING_TABLE.getDefaultState());
		c.set(c.x0 + 1, c.y0 + 4, c.z0 + 1, COBWEB);
	}

	private static void tent(Canvas c) {
		prepareGround(c, 5, DIRT);
		BlockState wool = c.global(170) < 0.5F ? Blocks.GREEN_WOOL.getDefaultState() : Blocks.BROWN_WOOL.getDefaultState();
		for (int z = c.z0; z <= c.z1; z++) {
			for (int i = 0; i < 3; i++) {
				if (c.rnd(c.x0 + i, i, z, 171) < 0.92F) {
					c.set(c.x0 + i, c.y0 + 1 + i, z, wool);
				}
				if (c.rnd(c.x1 - i, i, z, 172) < 0.92F) {
					c.set(c.x1 - i, c.y0 + 1 + i, z, wool);
				}
			}
		}
		c.set(c.x0 + 2, c.y0 + 1, c.z0 + 2, Blocks.CRAFTING_TABLE.getDefaultState());
		if (c.global(173) < 0.6F) {
			c.chest(c.x0 + 3, c.y0 + 1, c.z1 - 1, Direction.NORTH, ModStructures.LOOT_MILITARY);
		}
		c.set(c.x0 + 2, c.y0 + 1, c.z1 + 0, Blocks.CAMPFIRE.getDefaultState().with(net.minecraft.block.CampfireBlock.LIT, false));
	}

	private static void helipad(Canvas c) {
		prepareGround(c, 5, STONE);
		int cx = (c.x0 + c.x1) / 2;
		int cz = (c.z0 + c.z1) / 2;
		for (int x = c.x0; x <= c.x1; x++) {
			for (int z = c.z0; z <= c.z1; z++) {
				int dx = x - cx;
				int dz = z - cz;
				double dist = Math.sqrt(dx * dx + dz * dz);
				if (dist > 7.3) {
					continue;
				}
				BlockState s = dist > 6.3 ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.GRAY_CONCRETE.getDefaultState();
				boolean h = (Math.abs(dx) == 3 && Math.abs(dz) <= 3) || (dz == 0 && Math.abs(dx) <= 3);
				if (h) {
					s = Blocks.YELLOW_CONCRETE.getDefaultState();
				}
				if (c.rnd(x, c.y0, z, 180) < 0.06F) {
					s = Blocks.CRACKED_STONE_BRICKS.getDefaultState();
				}
				c.set(x, c.y0, z, s);
			}
		}
		// Wrack eines Hubschraubers
		if (c.global(181) < 0.5F) {
			BlockState hull = Blocks.GREEN_CONCRETE.getDefaultState();
			for (int z = cz - 3; z <= cz + 3; z++) {
				for (int x = cx - 1; x <= cx + 1; x++) {
					c.set(x, c.y0 + 1, z, hull);
					c.set(x, c.y0 + 2, z, z < cz - 1 ? Blocks.TINTED_GLASS.getDefaultState() : hull);
				}
			}
			for (int z = cz + 4; z <= cz + 7; z++) {
				c.set(cx, c.y0 + 2, z, hull);
			}
			for (int i = -4; i <= 4; i++) {
				c.set(cx + i, c.y0 + 3, cz, Blocks.IRON_BARS.getDefaultState());
				c.set(cx, c.y0 + 3, cz + i, Blocks.IRON_BARS.getDefaultState());
			}
			c.chest(cx, c.y0 + 1, cz + 1, Direction.SOUTH, ModStructures.LOOT_MILITARY);
		}
	}

	private static void armory(Canvas c, Direction facing) {
		prepareGround(c, 6, STONE);
		for (int x = c.x0; x <= c.x1; x++) {
			for (int z = c.z0; z <= c.z1; z++) {
				boolean edge = x == c.x0 || x == c.x1 || z == c.z0 || z == c.z1;
				boolean corner = (x == c.x0 || x == c.x1) && (z == c.z0 || z == c.z1);
				c.set(x, c.y0, z, Blocks.POLISHED_DEEPSLATE.getDefaultState());
				for (int y = c.y0 + 1; y <= c.y0 + 4; y++) {
					c.set(x, y, z, corner ? Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState()
							: edge ? (c.rnd(x, y, z, 190) < 0.2F ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState() : Blocks.STONE_BRICKS.getDefaultState())
							: AIR);
				}
				c.set(x, c.y0 + 5, z, Blocks.STONE_BRICK_SLAB.getDefaultState());
			}
		}
		int mx = (c.x0 + c.x1) / 2;
		int doorZ = facing == Direction.NORTH ? c.z0 : c.z1;
		c.set(mx, c.y0 + 1, doorZ, Blocks.IRON_BARS.getDefaultState());
		c.set(mx, c.y0 + 2, doorZ, AIR);
		c.set(mx - 1, c.y0 + 1, doorZ, AIR);
		c.set(mx - 1, c.y0 + 2, doorZ, AIR);
		int backZ = facing == Direction.NORTH ? c.z1 - 1 : c.z0 + 1;
		Direction chestFacing = facing == Direction.NORTH ? Direction.NORTH : Direction.SOUTH;
		c.chest(c.x0 + 2, c.y0 + 1, backZ, chestFacing, ModStructures.LOOT_MILITARY);
		c.chest(mx, c.y0 + 1, backZ, chestFacing, ModStructures.LOOT_MILITARY);
		c.chest(c.x1 - 2, c.y0 + 1, backZ, chestFacing, ModStructures.LOOT_MILITARY);
		for (int z = c.z0 + 2; z <= c.z1 - 2; z++) {
			c.barrel(c.x0 + 1, c.y0 + 1, z, ModStructures.LOOT_MILITARY);
		}
		hangingLantern(c, mx, c.y0 + 4, (c.z0 + c.z1) / 2);
	}

	private static void tank(Canvas c) {
		BlockState hull = Blocks.GREEN_CONCRETE.getDefaultState();
		BlockState track = Blocks.GRAY_CONCRETE.getDefaultState();
		int cx = c.x0 + 2;
		for (int z = c.z0 + 1; z <= c.z1 - 1; z++) {
			for (int x = c.x0; x <= c.x1; x++) {
				boolean side = x == c.x0 || x == c.x1;
				c.set(x, c.y0 + 1, z, side ? track : hull);
				c.set(x, c.y0 + 2, z, side ? Blocks.MOSS_CARPET.getDefaultState() : hull);
			}
		}
		for (int x = cx - 1; x <= cx + 1; x++) {
			for (int z = c.z0 + 3; z <= c.z0 + 5; z++) {
				c.set(x, c.y0 + 3, z, Blocks.GREEN_TERRACOTTA.getDefaultState());
			}
		}
		c.set(cx, c.y0 + 4, c.z0 + 4, Blocks.IRON_TRAPDOOR.getDefaultState());
		for (int z = c.z0 + 6; z <= c.z1; z++) {
			c.set(cx, c.y0 + 3, z, Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState());
		}
	}

	private static void truck(Canvas c) {
		BlockState body = Blocks.GREEN_CONCRETE.getDefaultState();
		BlockState wheel = Blocks.BLACK_CONCRETE.getDefaultState();
		for (int z = c.z0; z <= c.z1; z++) {
			for (int x = c.x0; x <= c.x1; x++) {
				boolean side = x == c.x0 || x == c.x1;
				int lz = z - c.z0;
				boolean wheelRow = lz == 1 || lz == 5 || lz == 6;
				c.set(x, c.y0 + 1, z, side && wheelRow ? wheel : body);
				if (lz <= 2) {
					c.set(x, c.y0 + 2, z, lz == 0 ? Blocks.TINTED_GLASS.getDefaultState() : body);
					c.set(x, c.y0 + 3, z, lz == 0 ? body : Blocks.SMOOTH_STONE_SLAB.getDefaultState());
				} else {
					c.set(x, c.y0 + 2, z, side || z == c.z1 ? Blocks.GREEN_WOOL.getDefaultState() : AIR);
					c.set(x, c.y0 + 3, z, Blocks.GREEN_WOOL.getDefaultState());
				}
			}
		}
		c.chest(c.x0 + 1, c.y0 + 2, c.z1 - 1, Direction.NORTH, ModStructures.LOOT_MILITARY);
	}
}
