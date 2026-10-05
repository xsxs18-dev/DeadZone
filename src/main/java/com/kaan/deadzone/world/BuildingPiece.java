package com.kaan.deadzone.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * Ein prozedural gebautes Gebäude. Alle Zufallsentscheidungen hängen nur von Seed und Position ab,
 * dadurch sieht das Gebäude gleich aus, egal in welcher Reihenfolge die Chunks generiert werden.
 */
public class BuildingPiece extends StructurePiece {
	public enum Kind {
		ROAD_X, ROAD_Z, SKYSCRAPER, APARTMENT, SHOP, HOUSE, RUBBLE,
		WATCHTOWER, RADIO_TOWER, BUNKER,
		MIL_GROUND, MIL_TOWER, BARRACKS, TENT, HELIPAD, ARMORY, TANK, TRUCK,
		MEGA_TOWER, HOSPITAL, POLICE, SUPERMARKET, GAS_STATION, PARKING, PARK, CHECKPOINT, CRATER, FALLEN_TOWER, HIGHWAY_X, SECRET_BUNKER
	}

	public static final int FLAG_SIDEWALK = 1;
	public static final int FLAG_METRO = 2;

	/** Zerstörungsgrad in die Flags packen (siehe Builders.INTACT ... LEANING). */
	public static int state(int state) {
		return (state & 0xF) << 4;
	}

	private final Kind kind;
	private final long seed;
	private final Direction facing;
	private final int flags;

	public BuildingPiece(Kind kind, BlockBox box, long seed, Direction facing, int flags) {
		super(ModStructures.BUILDING, 0, box);
		this.kind = kind;
		this.seed = seed;
		this.facing = facing;
		this.flags = flags;
	}

	public BuildingPiece(NbtCompound nbt) {
		super(ModStructures.BUILDING, nbt);
		Kind k;
		try {
			k = Kind.valueOf(nbt.getString("Kind", "RUBBLE"));
		} catch (IllegalArgumentException e) {
			k = Kind.RUBBLE;
		}
		this.kind = k;
		this.seed = nbt.getLong("Seed", 0L);
		this.facing = Direction.fromHorizontalQuarterTurns(nbt.getInt("Facing", 0));
		this.flags = nbt.getInt("Flags", 0);
	}

	@Override
	protected void writeNbt(StructureContext context, NbtCompound nbt) {
		nbt.putString("Kind", this.kind.name());
		nbt.putLong("Seed", this.seed);
		nbt.putInt("Facing", this.facing.getHorizontalQuarterTurns());
		nbt.putInt("Flags", this.flags);
	}

	@Override
	public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random,
						 BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
		Canvas c = new Canvas(world, chunkBox, this.boundingBox, this.seed);
		Builders.build(this.kind, c, this.facing, this.flags);
	}
}
