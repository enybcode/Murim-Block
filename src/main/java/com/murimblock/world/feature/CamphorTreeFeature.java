package com.murimblock.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.murimblock.world.block.MurimBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * The camphor of the southern villages (docs/biomes/camphor_hills.md): a bare, stout trunk two blocks thick, flared at
 * its foot, thick roots crawling out over the ground all round and sinking into it; high up it splits into four to six
 * great limbs that climb outwards, each holding two tiers of glossy leaves, so the crown is a broad, rounded, uneven dome
 * lifted well clear of the ground. The ancient one, the fengshui tree of a village, stands on a trunk three blocks thick
 * with more and longer roots and limbs and an immense crown.
 */
public class CamphorTreeFeature extends Feature<CamphorTreeFeature.Config> {
    public record Config(boolean ancient) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("ancient", false).forGetter(Config::ancient)).apply(i, Config::new));
    }

    public CamphorTreeFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        BlockPos base = context.origin();
        if (!level.getBlockState(base.below()).is(BlockTags.DIRT)) return false;
        return grow(level, context.random(), base, context.config().ancient());
    }

    /** Grows a camphor whose trunk starts at base (false if there is no room for its trunk). */
    public static boolean grow(WorldGenLevel level, RandomSource random, BlockPos base, boolean ancient) {
        // Massive trunks: two blocks thick, three for the ancient one.
        int thick = ancient ? 3 : 2;
        int trunkHeight = ancient ? 8 + random.nextInt(3) : 6 + random.nextInt(3);
        for (int y = 0; y <= trunkHeight; y++) for (int dx = 0; dx < thick; dx++) for (int dz = 0; dz < thick; dz++) {
            if (!free(level, base.offset(dx, y, dz))) return false;
        }
        BlockState log = MurimBlocks.CAMPHOR.log().get().defaultBlockState();
        BlockState leaves = MurimBlocks.CAMPHOR.leaves().get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

        // The trunk, and the soil carried under its foot (on a slope it would float).
        for (int dx = 0; dx < thick; dx++) for (int dz = 0; dz < thick; dz++) {
            for (int y = 0; y < trunkHeight; y++) level.setBlock(base.offset(dx, y, dz), log, 3);
            for (int y = 1; y <= 3 && !level.getBlockState(base.offset(dx, -y, dz)).isSolid(); y++) level.setBlock(base.offset(dx, -y, dz), log, 3);
        }
        double cx = base.getX() + thick / 2.0, cz = base.getZ() + thick / 2.0;
        // The foot flares: a ring of wood round the trunk at the ground (two rings high for the ancient one).
        for (int y = 0; y < (ancient ? 2 : 1); y++) for (int dx = -1; dx <= thick; dx++) for (int dz = -1; dz <= thick; dz++) {
            boolean ring = dx < 0 || dz < 0 || dx >= thick || dz >= thick;
            boolean corner = (dx < 0 || dx >= thick) && (dz < 0 || dz >= thick);
            if (!ring || (corner && (y > 0 || random.nextBoolean()))) continue;
            BlockPos at = base.offset(dx, y, dz);
            if (free(level, at) && !free(level, at.below())) level.setBlock(at, log, 3);
        }
        // The roots: thick ones crawling out over the ground all round, high near the trunk, lower further out, then sinking
        // into the soil, winding a little as they go, following every rise and dip of the ground.
        int roots = ancient ? 7 + random.nextInt(4) : 5 + random.nextInt(3);
        double rootAngle0 = random.nextDouble() * Math.PI * 2;
        for (int n = 0; n < roots; n++) {
            double a = rootAngle0 + n * Math.PI * 2 / roots + (random.nextDouble() - 0.5) * 0.5;
            int len = ancient ? 8 + random.nextInt(6) : 5 + random.nextInt(4);
            double x = cx + Math.cos(a) * (thick / 2.0 + 0.5), z = cz + Math.sin(a) * (thick / 2.0 + 0.5);
            int bx = Mth.floor(x), bz = Mth.floor(z), groundY = base.getY();
            for (int k = 0; k < len; k++) {
                if (k % 2 == 1) a += (random.nextDouble() - 0.5) * 0.5; // it winds
                x += Math.cos(a);
                z += Math.sin(a);
                int nx = Mth.floor(x), nz = Mth.floor(z);
                // One block at a time on one axis, so the root stays one piece.
                while (bx != nx || bz != nz) {
                    Direction.Axis axis;
                    if (bx != nx && (bz == nz || random.nextBoolean())) { bx += Integer.signum(nx - bx); axis = Direction.Axis.X; }
                    else { bz += Integer.signum(nz - bz); axis = Direction.Axis.Z; }
                    // The ground here: down while there is nothing under it, up over a step.
                    BlockPos at = new BlockPos(bx, groundY, bz);
                    for (int i = 0; i < 6 && free(level, at.below()); i++) at = at.below();
                    for (int i = 0; i < 2 && !free(level, at); i++) at = at.above();
                    if (!free(level, at)) break;
                    groundY = at.getY();
                    BlockState root = log.setValue(RotatedPillarBlock.AXIS, axis);
                    double t = (double) k / len;
                    int height = t < 0.3 ? (ancient ? 3 : 2) : t < 0.7 ? 1 : 0;
                    if (height == 0) {
                        // Sinking: the root runs in the soil, its back showing.
                        if (level.getBlockState(at.below()).is(BlockTags.DIRT)) level.setBlock(at.below(), root, 3);
                    } else {
                        for (int h = 0; h < height && free(level, at.above(h)); h++) level.setBlock(at.above(h), h == 0 ? root : log, 3);
                    }
                }
            }
        }

        // The limbs: from the top of the trunk, outwards and up, each a few logs, ending in a cloud of leaves.
        int top = base.getY() + trunkHeight - 1;
        int limbs = ancient ? 6 + random.nextInt(3) : 4 + random.nextInt(3);
        double angle0 = random.nextDouble() * Math.PI * 2;
        for (int l = 0; l < limbs; l++) {
            double a = angle0 + l * Math.PI * 2 / limbs + (random.nextDouble() - 0.5) * 0.6;
            int len = ancient ? 7 + random.nextInt(4) : 4 + random.nextInt(3);
            double rise = 0.75 + random.nextDouble() * 0.4;
            // A continuous limb: from the top of the trunk to its end, one block at a time along the axis furthest from
            // done, each log touching the last by a face (no floating steps), lying along the way it goes.
            int x = Mth.floor(cx), y = top - random.nextInt(2), z = Mth.floor(cz);
            int tx = Mth.floor(cx + Math.cos(a) * len), ty = top + (int) Math.round(len * rise), tz = Mth.floor(cz + Math.sin(a) * len);
            BlockPos end = null;
            for (int guard = 0; guard < 32 && (x != tx || y != ty || z != tz); guard++) {
                int ddx = tx - x, ddy = ty - y, ddz = tz - z;
                Direction.Axis axis;
                if (Math.abs(ddy) >= Math.abs(ddx) && Math.abs(ddy) >= Math.abs(ddz)) { y += Integer.signum(ddy); axis = Direction.Axis.Y; }
                else if (Math.abs(ddx) >= Math.abs(ddz)) { x += Integer.signum(ddx); axis = Direction.Axis.X; }
                else { z += Integer.signum(ddz); axis = Direction.Axis.Z; }
                BlockPos at = new BlockPos(x, y, z);
                if (!free(level, at) && !level.getBlockState(at).is(log.getBlock())) break;
                level.setBlock(at, log.setValue(RotatedPillarBlock.AXIS, axis), 3);
                // Thick at its root: the first blocks of a limb are doubled underneath.
                if (guard < (ancient ? 4 : 2) && free(level, at.below())) level.setBlock(at.below(), log.setValue(RotatedPillarBlock.AXIS, axis), 3);
                end = at;
            }
            if (end != null) {
                int r = ancient ? 5 + random.nextInt(2) : 4 + random.nextInt(2);
                cloud(level, random, end, r, leaves);
                cloud(level, random, end.above(r / 2 + 1), r - 1, leaves); // a second tier: a rounded crown, not a flat one
            }
        }
        // A cloud over the top of the trunk, closing the dome.
        cloud(level, random, BlockPos.containing(cx, top + (ancient ? 7 : 5), cz), ancient ? 7 : 5, leaves);
        cloud(level, random, BlockPos.containing(cx, top + (ancient ? 10 : 8), cz), ancient ? 5 : 3, leaves);
        return true;
    }

    /** A rounded, ragged cloud of leaves: about r wide, r * 0.7 high, thinning at its rim, its underside flat. */
    private static void cloud(WorldGenLevel level, RandomSource random, BlockPos centre, int r, BlockState leaves) {
        int h = Math.max(2, Mth.ceil(r * 0.7));
        for (int dy = -1; dy <= h; dy++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            double ny = dy < 0 ? 0.9 : (double) dy / h;
            double d = (dx * dx + dz * dz) / (double) (r * r) + ny * ny;
            if (d > 1.0) continue;
            if (d > 0.7 && random.nextFloat() < 0.45) continue; // ragged rim, a few holes
            BlockPos at = centre.offset(dx, dy, dz);
            if (level.isEmptyBlock(at) || level.getBlockState(at).is(BlockTags.REPLACEABLE_BY_TREES)) level.setBlock(at, leaves, 2);
        }
    }

    private static boolean free(WorldGenLevel level, BlockPos at) {
        BlockState s = level.getBlockState(at);
        return s.isAir() || s.is(BlockTags.REPLACEABLE_BY_TREES) || s.is(BlockTags.LEAVES);
    }
}
