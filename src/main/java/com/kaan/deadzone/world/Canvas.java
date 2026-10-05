package com.kaan.deadzone.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.StructureWorldAccess;

/** Schreibt Blöcke in absoluten Koordinaten, aber nur innerhalb des gerade generierten Chunks. */
final class Canvas {
	final StructureWorldAccess world;
	final BlockBox clip;
	final BlockBox box;
	final long seed;
	final int x0, y0, z0, x1, y1, z1;
	private final BlockPos.Mutable pos = new BlockPos.Mutable();

	Canvas(StructureWorldAccess world, BlockBox clip, BlockBox box, long seed) {
		this.world = world;
		this.clip = clip;
		this.box = box;
		this.seed = seed;
		this.x0 = box.getMinX();
		this.y0 = box.getMinY();
		this.z0 = box.getMinZ();
		this.x1 = box.getMaxX();
		this.y1 = box.getMaxY();
		this.z1 = box.getMaxZ();
	}

	boolean inClip(int x, int y, int z) {
		return this.clip.contains(this.pos.set(x, y, z));
	}

	boolean columnInClip(int x, int z) {
		return x >= this.clip.getMinX() && x <= this.clip.getMaxX() && z >= this.clip.getMinZ() && z <= this.clip.getMaxZ();
	}

	void set(int x, int y, int z, BlockState state) {
		if (!this.clip.contains(this.pos.set(x, y, z))) {
			return;
		}
		this.world.setBlockState(this.pos, state, Block.NOTIFY_LISTENERS);
		Block b = state.getBlock();
		if (b instanceof PaneBlock || b instanceof FenceBlock || b instanceof WallBlock || b instanceof StairsBlock) {
			this.world.getChunk(this.pos).markBlockForPostProcessing(this.pos);
		}
	}

	void set(int x, int y, int z, Block block) {
		this.set(x, y, z, block.getDefaultState());
	}

	BlockState get(int x, int y, int z) {
		return this.world.getBlockState(this.pos.set(x, y, z));
	}

	void fill(int ax, int ay, int az, int bx, int by, int bz, BlockState state) {
		for (int x = Math.min(ax, bx); x <= Math.max(ax, bx); x++) {
			for (int z = Math.min(az, bz); z <= Math.max(az, bz); z++) {
				if (!this.columnInClip(x, z)) {
					continue;
				}
				for (int y = Math.min(ay, by); y <= Math.max(ay, by); y++) {
					this.set(x, y, z, state);
				}
			}
		}
	}

	void fill(int ax, int ay, int az, int bx, int by, int bz, Block block) {
		this.fill(ax, ay, az, bx, by, bz, block.getDefaultState());
	}

	/** Füllt unter (x, top, z) bis zu 12 Blöcke auf, damit Gebäude nicht über Löchern schweben. */
	void foundation(int x, int top, int z, BlockState state) {
		if (!this.columnInClip(x, z)) {
			return;
		}
		for (int y = top - 1; y > top - 13; y--) {
			BlockState s = this.get(x, y, z);
			if (s.isAir() || s.isReplaceable() || !s.getFluidState().isEmpty() || s.isIn(net.minecraft.registry.tag.BlockTags.LEAVES) || s.isIn(net.minecraft.registry.tag.BlockTags.LOGS)) {
				this.set(x, y, z, state);
			} else {
				return;
			}
		}
	}

	void clear(int x, int z, int from, int to) {
		if (!this.columnInClip(x, z)) {
			return;
		}
		for (int y = from; y <= to; y++) {
			if (!this.get(x, y, z).isAir()) {
				this.set(x, y, z, Blocks.AIR.getDefaultState());
			}
		}
	}

	void chest(int x, int y, int z, Direction facing, RegistryKey<LootTable> loot) {
		if (!this.inClip(x, y, z)) {
			return;
		}
		this.set(x, y, z, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, facing));
		if (this.world.getBlockEntity(this.pos.set(x, y, z)) instanceof LootableContainerBlockEntity container) {
			container.setLootTable(loot, this.hash(x, y, z, 77));
		}
	}

	void barrel(int x, int y, int z, RegistryKey<LootTable> loot) {
		if (!this.inClip(x, y, z)) {
			return;
		}
		this.set(x, y, z, Blocks.BARREL.getDefaultState());
		if (this.world.getBlockEntity(this.pos.set(x, y, z)) instanceof LootableContainerBlockEntity container) {
			container.setLootTable(loot, this.hash(x, y, z, 78));
		}
	}

	/** Spawnt einen dauerhaften Mob, aber nur in dem Chunk, der gerade generiert wird (sonst doppelt). */
	void spawnMob(EntityType<? extends MobEntity> type, double x, double y, double z) {
		BlockPos p = BlockPos.ofFloored(x, y, z);
		if (!this.clip.contains(p)) {
			return;
		}
		MobEntity mob = type.create(this.world.toServerWorld(), SpawnReason.STRUCTURE);
		if (mob == null) {
			return;
		}
		mob.refreshPositionAndAngles(x, y, z, (float) (this.rnd(p.getX(), p.getY(), p.getZ(), 90) * 360), 0);
		mob.initialize(this.world, this.world.getLocalDifficulty(p), SpawnReason.STRUCTURE, null);
		mob.setPersistent();
		this.world.spawnEntityAndPassengers(mob);
	}

	void armorStand(int x, int y, int z, ItemStack head, ItemStack chest) {
		if (!this.inClip(x, y, z)) {
			return;
		}
		ArmorStandEntity stand = new ArmorStandEntity(this.world.toServerWorld(), x + 0.5, y, z + 0.5);
		stand.setYaw((float) (this.rnd(x, y, z, 91) * 360));
		stand.equipStack(EquipmentSlot.HEAD, head);
		stand.equipStack(EquipmentSlot.CHEST, chest);
		this.world.spawnEntity(stand);
	}

	// ------------------------------------------------------------ deterministischer Zufall

	long hash(int x, int y, int z, int salt) {
		long h = this.seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L) ^ (salt * 0x27D4EB2F165667C5L);
		h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
		h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
		return h ^ (h >>> 31);
	}

	/** Zufallszahl 0..1, nur abhängig von Position, Salt und Seed. */
	float rnd(int x, int y, int z, int salt) {
		return (this.hash(x, y, z, salt) >>> 40) / (float) (1 << 24);
	}

	/** Weiches 2D-Rauschen 0..1 (für eingestürzte Dächer usw.). */
	float noise(int x, int z, int scale, int salt) {
		float fx = (float) x / scale;
		float fz = (float) z / scale;
		int ix = MathHelper.floor(fx);
		int iz = MathHelper.floor(fz);
		float tx = fx - ix;
		float tz = fz - iz;
		tx = tx * tx * (3 - 2 * tx);
		tz = tz * tz * (3 - 2 * tz);
		float a = this.rnd(ix, 0, iz, salt);
		float b = this.rnd(ix + 1, 0, iz, salt);
		float c = this.rnd(ix, 0, iz + 1, salt);
		float d = this.rnd(ix + 1, 0, iz + 1, salt);
		return MathHelper.lerp(tz, MathHelper.lerp(tx, a, b), MathHelper.lerp(tx, c, d));
	}

	/** Zufall, der für das ganze Gebäude gleich ist. */
	float global(int salt) {
		return this.rnd(this.x0, this.y0, this.z0, salt);
	}

	int globalInt(int salt, int bound) {
		return Math.min(bound - 1, (int) (this.global(salt) * bound));
	}

	BlockState pick(int x, int y, int z, int salt, BlockState... states) {
		return states[Math.min(states.length - 1, (int) (this.rnd(x, y, z, salt) * states.length))];
	}
}
