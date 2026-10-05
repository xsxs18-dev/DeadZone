package com.kaan.deadzone.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.SpreadType;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Optional;

/** Eine Ruinen-Struktur. Das Feld "layout" wählt, was gebaut wird: Stadt, Haus, Bunker, Turm oder Militärbasis. */
public class RuinStructure extends Structure {
	public enum Layout implements StringIdentifiable {
		CITY("city", 58, 14),
		HOUSE("house", 8, 7),
		BUNKER("bunker", 4, 6),
		TOWER("tower", 5, 9),
		MILITARY("military", 32, 12),
		METROPOLIS("metropolis", 100, 28),
		SECRET_BUNKER("secret_bunker", 4, 8);

		public static final Codec<Layout> CODEC = StringIdentifiable.createCodec(Layout::values);

		private final String id;
		final int radius;
		final int maxSlope;

		Layout(String id, int radius, int maxSlope) {
			this.id = id;
			this.radius = radius;
			this.maxSlope = maxSlope;
		}

		@Override
		public String asString() {
			return this.id;
		}
	}

	public static final MapCodec<RuinStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			configCodecBuilder(i),
			Layout.CODEC.fieldOf("layout").forGetter(s -> s.layout)
	).apply(i, RuinStructure::new));

	private final Layout layout;

	public RuinStructure(Structure.Config config, Layout layout) {
		super(config);
		this.layout = layout;
	}

	// Muss zu den structure_set-JSONs passen (tools/gen_data.py)
	private static final RandomSpreadStructurePlacement METROPOLIS_GRID = new RandomSpreadStructurePlacement(36, 16, SpreadType.LINEAR, 902211357);
	private static final RandomSpreadStructurePlacement CITY_GRID = new RandomSpreadStructurePlacement(24, 10, SpreadType.LINEAR, 581630241);

	/** true, wenn in der Nähe eine (mögliche) Stadt oder Mega-Stadt startet. Kleine Ruinen weichen dann aus. */
	private static boolean nearCity(long seed, ChunkPos chunk) {
		return near(METROPOLIS_GRID, 36, 9, seed, chunk) || near(CITY_GRID, 24, 5, seed, chunk);
	}

	private static boolean near(RandomSpreadStructurePlacement grid, int spacing, int chunks, long seed, ChunkPos chunk) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				ChunkPos start = grid.getStartChunk(seed, chunk.x + dx * spacing, chunk.z + dz * spacing);
				if (Math.abs(start.x - chunk.x) <= chunks && Math.abs(start.z - chunk.z) <= chunks) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected Optional<StructurePosition> getStructurePosition(Context context) {
		ChunkPos chunk = context.chunkPos();
		if (this.layout != Layout.CITY && this.layout != Layout.METROPOLIS && nearCity(context.seed(), chunk)) {
			return Optional.empty();
		}
		int cx = chunk.getCenterX();
		int cz = chunk.getCenterZ();
		ChunkGenerator gen = context.chunkGenerator();
		int r = this.layout.radius;
		int[][] samples = {{0, 0}, {r, r}, {-r, r}, {r, -r}, {-r, -r}, {r, 0}, {-r, 0}, {0, r}, {0, -r}};
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		int sum = 0;
		for (int[] s : samples) {
			int h = gen.getHeightInGround(cx + s[0], cz + s[1], Heightmap.Type.OCEAN_FLOOR_WG, context.world(), context.noiseConfig());
			min = Math.min(min, h);
			max = Math.max(max, h);
			sum += h;
		}
		if (min < gen.getSeaLevel() || max - min > this.layout.maxSlope) {
			return Optional.empty();
		}
		int ground = Math.round((float) sum / samples.length);
		BlockPos origin = new BlockPos(cx, ground, cz);
		long seed = context.random().nextLong();
		return Optional.of(new StructurePosition(origin, collector -> Layouts.build(this.layout, collector, origin, seed)));
	}

	@Override
	public StructureType<?> getType() {
		return ModStructures.RUIN;
	}
}
