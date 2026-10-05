package com.kaan.deadzone.world;

import com.kaan.deadzone.world.BuildingPiece.Kind;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Random;

/** Legt fest, welche Gebäude wo stehen. Gebaut werden sie danach in {@link Builders}. */
final class Layouts {
	private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	private Layouts() {
	}

	static void build(RuinStructure.Layout layout, StructurePiecesCollector out, BlockPos origin, long seed) {
		Random r = new Random(seed);
		switch (layout) {
			case CITY -> city(out, origin, r);
			case HOUSE -> house(out, origin, r);
			case BUNKER -> bunker(out, origin, r);
			case TOWER -> tower(out, origin, r);
			case MILITARY -> military(out, origin, r);
			case METROPOLIS -> metropolis(out, origin, r);
			case SECRET_BUNKER -> add(out, r, Kind.SECRET_BUNKER, origin.getX() - 22, origin.getY() - 40, origin.getZ() - 22, 45, 48, 45, Direction.NORTH, 0);
		}
	}

	private static void add(StructurePiecesCollector out, Random r, Kind kind, int x, int y, int z, int sx, int sy, int sz, Direction facing, int flags) {
		out.addPiece(new BuildingPiece(kind, new BlockBox(x, y, z, x + sx - 1, y + sy - 1, z + sz - 1), r.nextLong(), facing, flags));
	}

	private static Direction randomDir(Random r) {
		return HORIZONTAL[r.nextInt(4)];
	}

	// ------------------------------------------------------------------ Stadt

	private static void city(StructurePiecesCollector out, BlockPos o, Random r) {
		int n = 3 + r.nextInt(2);
		int lot = 20;
		int road = 7;
		int extent = n * lot + (n + 1) * road;
		int sx = o.getX() - extent / 2;
		int sz = o.getZ() - extent / 2;
		int y = o.getY();

		for (int i = 0; i <= n; i++) {
			int off = i * (lot + road);
			add(out, r, Kind.ROAD_X, sx, y, sz + off, extent, 7, road, Direction.NORTH, 0);
			add(out, r, Kind.ROAD_Z, sx + off, y, sz, road, 7, extent, Direction.NORTH, 0);
		}

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				int lx = sx + road + i * (lot + road);
				int lz = sz + road + j * (lot + road);
				boolean center = Math.abs(i * 2 - (n - 1)) <= 1 && Math.abs(j * 2 - (n - 1)) <= 1;
				float roll = r.nextFloat();
				Kind kind;
				int height;
				if (roll < (center ? 0.55F : 0.2F)) {
					kind = Kind.SKYSCRAPER;
					height = (6 + r.nextInt(6)) * 4 + 3;
				} else if (roll < 0.55F) {
					kind = Kind.APARTMENT;
					height = (3 + r.nextInt(3)) * 4 + 3;
				} else if (roll < 0.78F) {
					kind = Kind.SHOP;
					height = (1 + r.nextInt(2)) * 4 + 3;
				} else if (roll < 0.9F) {
					kind = Kind.HOUSE;
					height = 16;
				} else {
					kind = Kind.RUBBLE;
					height = 10;
				}
				add(out, r, kind, lx, y, lz, lot, height, lot, randomDir(r), BuildingPiece.FLAG_SIDEWALK | BuildingPiece.state(randomState(r, false)));
			}
		}
	}

	/** Zerstörungsgrad: hohe Gebäude stürzen öfter ein, können auch schief stehen. */
	private static int randomState(Random r, boolean canLean) {
		float f = r.nextFloat();
		if (f < 0.15F) {
			return Builders.INTACT;
		} else if (f < 0.45F) {
			return Builders.DAMAGED;
		} else if (f < 0.7F) {
			return Builders.HALF;
		} else if (f < 0.85F || !canLean) {
			return Builders.RUIN;
		}
		return Builders.LEANING;
	}

	// ------------------------------------------------------------------ Mega-Stadt

	private static void metropolis(StructurePiecesCollector out, BlockPos o, Random r) {
		int n = 7;
		int lot = 22;
		int road = 7;
		int step = lot + road;
		int extent = n * lot + (n + 1) * road;
		int sx = o.getX() - extent / 2;
		int sz = o.getZ() - extent / 2;
		int y = o.getY();
		int center = n / 2;
		int metro = BuildingPiece.FLAG_METRO;

		for (int i = 0; i <= n; i++) {
			int off = i * step;
			add(out, r, Kind.ROAD_X, sx, y, sz + off, extent, 7, road, Direction.NORTH, metro);
			add(out, r, Kind.ROAD_Z, sx + off, y, sz, road, 7, extent, Direction.NORTH, metro);
		}
		int highwayRow = r.nextBoolean() ? 2 : 5;
		add(out, r, Kind.HIGHWAY_X, sx, y, sz + highwayRow * step, extent, 19, road, Direction.NORTH, 0);

		// Umgestürzte Hochhäuser liegen auf zwei Straßen; das Grundstück daneben ist nur noch ein Stumpf
		boolean[][] stump = new boolean[n][n];
		for (int k = 0; k < 2; k++) {
			int col = k == 0 ? 2 : 5;
			int row = 1 + r.nextInt(n - 2);
			int x = sx + col * step;
			int z = sz + road + row * step - 3;
			add(out, r, Kind.FALLEN_TOWER, x, y, z, road, 10, lot + 6, Direction.NORTH, 0);
			stump[col - 1][row] = true;
		}

		java.util.List<Kind> specials = new java.util.ArrayList<>(java.util.List.of(
				Kind.HOSPITAL, Kind.POLICE, Kind.SUPERMARKET, Kind.GAS_STATION, Kind.PARKING, Kind.PARK, Kind.CHECKPOINT, Kind.PARKING, Kind.PARK));
		java.util.Collections.shuffle(specials, r);

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				int lx = sx + road + i * step;
				int lz = sz + road + j * step;
				int ring = Math.max(Math.abs(i - center), Math.abs(j - center));
				Direction facing = randomDir(r);
				Kind kind;
				int height;
				int state;
				if (stump[i][j]) {
					kind = Kind.SKYSCRAPER;
					height = 11 * 4 + 3;
					state = Builders.RUIN;
				} else if (ring == 0) {
					kind = r.nextFloat() < 0.7F ? Kind.CRATER : Kind.MEGA_TOWER;
					height = kind == Kind.CRATER ? 12 : (18 + r.nextInt(4)) * 4 + 3;
					state = kind == Kind.CRATER ? 0 : Builders.HALF;
				} else if (ring == 1) {
					kind = r.nextFloat() < 0.6F ? Kind.MEGA_TOWER : Kind.SKYSCRAPER;
					height = (kind == Kind.MEGA_TOWER ? 13 + r.nextInt(9) : 9 + r.nextInt(5)) * 4 + 3;
					state = randomState(r, true);
				} else if (ring == 2 && !specials.isEmpty() && r.nextFloat() < 0.35F) {
					kind = specials.remove(specials.size() - 1);
					height = specialHeight(kind, r);
					state = Builders.DAMAGED;
				} else if (ring == 2) {
					float f = r.nextFloat();
					kind = f < 0.4F ? Kind.SKYSCRAPER : f < 0.85F ? Kind.APARTMENT : Kind.SHOP;
					height = (kind == Kind.SKYSCRAPER ? 7 + r.nextInt(5) : kind == Kind.APARTMENT ? 4 + r.nextInt(3) : 1 + r.nextInt(2)) * 4 + 3;
					state = randomState(r, kind == Kind.SKYSCRAPER);
				} else if (!specials.isEmpty() && r.nextFloat() < 0.3F) {
					kind = specials.remove(specials.size() - 1);
					height = specialHeight(kind, r);
					state = Builders.DAMAGED;
				} else {
					float f = r.nextFloat();
					kind = f < 0.35F ? Kind.APARTMENT : f < 0.65F ? Kind.SHOP : f < 0.85F ? Kind.HOUSE : Kind.RUBBLE;
					height = switch (kind) {
						case APARTMENT -> (3 + r.nextInt(3)) * 4 + 3;
						case SHOP -> (1 + r.nextInt(2)) * 4 + 3;
						case HOUSE -> 16;
						default -> 10;
					};
					state = randomState(r, false);
				}
				int bx = lx;
				int bz = lz;
				int wx = lot;
				int wz = lot;
				if (state == Builders.LEANING) {
					switch (facing) {
						case NORTH -> {
							bz -= Builders.LEAN_EXT;
							wz += Builders.LEAN_EXT;
						}
						case SOUTH -> wz += Builders.LEAN_EXT;
						case WEST -> {
							bx -= Builders.LEAN_EXT;
							wx += Builders.LEAN_EXT;
						}
						default -> wx += Builders.LEAN_EXT;
					}
				}
				add(out, r, kind, bx, y, bz, wx, height, wz, facing, BuildingPiece.FLAG_SIDEWALK | BuildingPiece.state(state));
			}
		}
	}

	private static int specialHeight(Kind kind, Random r) {
		return switch (kind) {
			case HOSPITAL -> (4 + r.nextInt(2)) * 4 + 3;
			case POLICE -> 3 * 4 + 3;
			case PARKING -> 4 * 4 + 3;
			case SUPERMARKET, GAS_STATION -> 10;
			default -> 8;
		};
	}

	// ------------------------------------------------------------------ Einzelnes Haus

	private static void house(StructurePiecesCollector out, BlockPos o, Random r) {
		add(out, r, Kind.HOUSE, o.getX() - 8, o.getY(), o.getZ() - 8, 17, 16, 17, randomDir(r), 0);
	}

	// ------------------------------------------------------------------ Bunker

	private static void bunker(StructurePiecesCollector out, BlockPos o, Random r) {
		add(out, r, Kind.BUNKER, o.getX() - 3, o.getY() - 16, o.getZ() - 10, 27, 21, 21, Direction.EAST, 0);
	}

	// ------------------------------------------------------------------ Turm

	private static void tower(StructurePiecesCollector out, BlockPos o, Random r) {
		if (r.nextBoolean()) {
			add(out, r, Kind.WATCHTOWER, o.getX() - 4, o.getY(), o.getZ() - 4, 9, 18, 9, randomDir(r), 0);
		} else {
			add(out, r, Kind.RADIO_TOWER, o.getX() - 4, o.getY(), o.getZ() - 4, 9, 34, 9, randomDir(r), 0);
		}
	}

	// ------------------------------------------------------------------ Militärbasis

	private static void military(StructurePiecesCollector out, BlockPos o, Random r) {
		int size = 56;
		int x = o.getX() - size / 2;
		int z = o.getZ() - size / 2;
		int y = o.getY();
		add(out, r, Kind.MIL_GROUND, x, y, z, size, 5, size, Direction.SOUTH, 0);

		add(out, r, Kind.MIL_TOWER, x + 1, y, z + 1, 7, 13, 7, Direction.SOUTH, 0);
		add(out, r, Kind.MIL_TOWER, x + size - 8, y, z + 1, 7, 13, 7, Direction.SOUTH, 0);
		add(out, r, Kind.MIL_TOWER, x + 1, y, z + size - 8, 7, 13, 7, Direction.NORTH, 0);
		add(out, r, Kind.MIL_TOWER, x + size - 8, y, z + size - 8, 7, 13, 7, Direction.NORTH, 0);

		add(out, r, Kind.BARRACKS, x + 10, y, z + 9, 9, 7, 15, Direction.EAST, 0);
		add(out, r, Kind.BARRACKS, x + 21, y, z + 9, 9, 7, 15, Direction.EAST, 0);
		add(out, r, Kind.ARMORY, x + 36, y, z + 10, 9, 7, 9, Direction.SOUTH, 0);

		add(out, r, Kind.HELIPAD, x + 34, y, z + 30, 15, 6, 15, Direction.NORTH, 0);
		add(out, r, Kind.TENT, x + 9, y, z + 30, 6, 6, 8, Direction.SOUTH, 0);
		add(out, r, Kind.TENT, x + 17, y, z + 30, 6, 6, 8, Direction.SOUTH, 0);
		add(out, r, Kind.TENT, x + 9, y, z + 41, 6, 6, 8, Direction.SOUTH, 0);
		add(out, r, Kind.TANK, x + 25, y, z + 31, 5, 6, 9, Direction.SOUTH, 0);
		add(out, r, Kind.TRUCK, x + 19, y, z + 42, 4, 5, 8, Direction.SOUTH, 0);
	}
}
