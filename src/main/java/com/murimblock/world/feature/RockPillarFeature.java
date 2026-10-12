package com.murimblock.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

/**
 * Freestanding rock peaks, the signature of three Murim biomes, rising from the ground they stand on:
 * <ul>
 * <li>TOWER: Guilin karst tower (fenglin): round, near-vertical, slightly bulging, domed and wooded top, vertical
 * black and moss streaks where water runs down, vines.</li>
 * <li>SPIRE: Huangshan granite spire: jointed into offset blocks, narrowing by steps, a sheared top for a cliff pine.</li>
 * <li>PILLAR: Wulingyuan sandstone pillar (peach valley): squarish, straight sides with horizontal bedding bands,
 * flat green top.</li>
 * </ul>
 * Height and radius are in blocks; the column starts a few blocks under the surface so it never floats.
 */
public class RockPillarFeature extends Feature<RockPillarFeature.Config> {
    public RockPillarFeature() {
        super(Config.CODEC);
    }

    public enum Style implements StringRepresentable {
        TOWER, SPIRE, PILLAR;

        public static final Codec<Style> CODEC = StringRepresentable.fromEnum(Style::values);

        @Override public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record Config(Style style, BlockStateProvider rock, BlockStateProvider accent, BlockStateProvider top,
                         IntProvider radius, IntProvider height) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Style.CODEC.fieldOf("style").forGetter(Config::style),
                BlockStateProvider.CODEC.fieldOf("rock").forGetter(Config::rock),
                BlockStateProvider.CODEC.fieldOf("accent").forGetter(Config::accent),
                BlockStateProvider.CODEC.fieldOf("top").forGetter(Config::top),
                IntProvider.codec(1, 16).fieldOf("radius").forGetter(Config::radius),
                IntProvider.codec(4, 120).fieldOf("height").forGetter(Config::height)
        ).apply(i, Config::new));
    }

    @Override public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Config config = context.config();
        BlockPos origin = context.origin();
        int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        if (ground <= level.getMinBuildHeight() + 8) return false;
        // Not in deep water and not on top of another peak.
        if (level.getBlockState(new BlockPos(origin.getX(), ground - 1, origin.getZ())).is(config.rock().getState(random, origin).getBlock())) return false;
        int radius = config.radius().sample(random);
        int height = config.height().sample(random);
        // Keep peaks apart: no other rock of this kind within a few blocks of where this one would stand,
        // so they rise as separate towers and never melt into walls.
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        net.minecraft.world.level.block.Block rockBlock = config.rock().getState(random, origin).getBlock();
        for (int k = 0; k < 16; k++) {
            double a = k * Math.PI / 8;
            int reach = radius * 2 + 4;
            probe.set(origin.getX() + Math.cos(a) * reach, ground + 8, origin.getZ() + Math.sin(a) * reach);
            if (level.getBlockState(probe).is(rockBlock)) return false;
            probe.set(origin.getX() + Math.cos(a) * (radius + 2), ground + 8, origin.getZ() + Math.sin(a) * (radius + 2));
            if (level.getBlockState(probe).is(rockBlock)) return false;
        }
        int bottom = ground - 4;
        int top = ground + height;
        SimplexNoise noise = new SimplexNoise(new WorldgenRandom(new LegacyRandomSource(random.nextLong())));
        double lean = (random.nextDouble() - 0.5) * 0.08;
        double leanAngle = random.nextDouble() * Math.PI * 2;
        double squash = 0.75 + random.nextDouble() * 0.5;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int[] jointShiftX = new int[height + 8], jointShiftZ = new int[height + 8];
        for (int s = 0; s < jointShiftX.length; s += 6) {
            int jx = random.nextInt(3) - 1, jz = random.nextInt(3) - 1;
            for (int k = s; k < Math.min(s + 6, jointShiftX.length); k++) { jointShiftX[k] = jx; jointShiftZ[k] = jz; }
        }
        for (int y = bottom; y <= top; y++) {
            double t = (double) (y - ground) / height; // 0 at the ground, 1 at the top
            double r = profile(config.style(), t, radius);
            if (r < 0.6) continue;
            double cx = origin.getX() + Math.cos(leanAngle) * lean * (y - ground);
            double cz = origin.getZ() + Math.sin(leanAngle) * lean * (y - ground);
            if (config.style() == Style.SPIRE) {
                int k = Mth.clamp(y - bottom, 0, jointShiftX.length - 1);
                cx += jointShiftX[k];
                cz += jointShiftZ[k];
            }
            int ri = Mth.ceil(r) + 2;
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double x = Math.floor(cx) + dx, z = Math.floor(cz) + dz;
                    double ox = x + 0.5 - cx, oz = (z + 0.5 - cz) / squash;
                    double dist = config.style() == Style.PILLAR
                            ? Math.max(Math.abs(ox), Math.abs(oz)) * 0.8 + Math.hypot(ox, oz) * 0.2 // rounded square
                            : Math.hypot(ox, oz);
                    double wobble = noise.getValue(x * 0.18, y * 0.08, z * 0.18) * (config.style() == Style.TOWER ? 1.4 : 0.8);
                    if (dist > r + wobble) continue;
                    pos.set(x, y, z);
                    BlockState existing = level.getBlockState(pos);
                    if (existing.is(Blocks.BEDROCK)) continue;
                    boolean surface = dist > r + wobble - 1.2;
                    level.setBlock(pos, rockAt(config, random, noise, x, y, z, surface), 2);
                }
            }
        }
        dressTop(level, random, config, origin, top, radius);
        if (config.style() == Style.TOWER) hangVines(level, random, origin, ground, top, radius);
        return true;
    }

    /** Radius at relative height t (0 ground, 1 top). */
    private static double profile(Style style, double t, int radius) {
        return switch (style) {
            // Karst tower: a touch wider at mid height, sheer, then a dome over the top 15 %.
            case TOWER -> t < 0.85 ? radius * (1.0 + 0.08 * Math.sin(t * Math.PI)) : radius * Math.sqrt(Math.max(0, 1 - Math.pow((t - 0.85) / 0.15, 2)));
            // Granite spire: narrows by steps, sharp shoulder near the top.
            case SPIRE -> t < 0.9 ? radius * (1.0 - 0.45 * Math.floor(t * 5) / 5) : radius * 0.55 * (1 - (t - 0.9) / 0.1 * 0.6);
            // Sandstone pillar: straight sides, a slightly undercut foot, flat top.
            case PILLAR -> t < 0.05 ? radius * 0.92 : radius;
        };
    }

    private static BlockState rockAt(Config config, RandomSource random, SimplexNoise noise, double x, int y, double z, boolean surface) {
        switch (config.style()) {
            case TOWER -> {
                // Vertical streaks: same accent for a whole run of height on the outer skin.
                double streak = noise.getValue(x * 0.9, y * 0.04, z * 0.9);
                if (surface && streak > 0.45) return config.accent().getState(random, BlockPos.containing(x, y, z));
            }
            case PILLAR -> {
                // Horizontal bedding: a band every 4 to 6 blocks.
                if (Math.floorMod(y + (int) (noise.getValue(x * 0.05, 0, z * 0.05) * 2), 5) == 0) return config.accent().getState(random, BlockPos.containing(x, y, z));
            }
            case SPIRE -> {
                // Joint lines between blocks of granite.
                if (surface && Math.floorMod(y, 6) == 0 && random.nextFloat() < 0.6f) return config.accent().getState(random, BlockPos.containing(x, y, z));
            }
        }
        return config.rock().getState(random, BlockPos.containing(x, y, z));
    }

    /** Soil and plants on the summit: a wooded cap (tower, pillar) or a bare sheared top (spire). */
    private static void dressTop(WorldGenLevel level, RandomSource random, Config config, BlockPos origin, int top, int radius) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r = radius + 2;
        for (int dx = -r - 2; dx <= r + 2; dx++) {
            for (int dz = -r - 2; dz <= r + 2; dz++) {
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (y < top - (config.style() == Style.SPIRE ? 4 : 10)) continue;
                pos.set(x, y, z);
                BlockState below = level.getBlockState(pos);
                if (!below.is(config.rock().getState(random, pos).getBlock()) && !below.is(config.accent().getState(random, pos).getBlock())) continue;
                if (config.style() == Style.SPIRE && random.nextFloat() < 0.6f) continue;
                level.setBlock(pos, config.top().getState(random, pos), 2);
                BlockPos above = pos.above();
                if (level.isEmptyBlock(above) && random.nextFloat() < 0.35f) {
                    level.setBlock(above, random.nextFloat() < 0.7f ? Blocks.SHORT_GRASS.defaultBlockState() : Blocks.FERN.defaultBlockState(), 2);
                }
            }
        }
    }

    /** Vines down the sunny faces of a karst tower. */
    private static void hangVines(WorldGenLevel level, RandomSource random, BlockPos origin, int ground, int top, int radius) {
        int attempts = 10 + radius * 3;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < attempts; i++) {
            Direction face = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int y = Mth.nextInt(random, ground + 3, Math.max(ground + 4, top - 2));
            int along = random.nextInt(radius * 2 + 1) - radius;
            int x = origin.getX() + face.getStepX() * (radius + 3) + (face.getAxis() == Direction.Axis.Z ? along : 0);
            int z = origin.getZ() + face.getStepZ() * (radius + 3) + (face.getAxis() == Direction.Axis.X ? along : 0);
            pos.set(x, y, z);
            // Walk inward until touching rock.
            for (int k = 0; k < radius + 4 && level.isEmptyBlock(pos.relative(face.getOpposite())); k++) pos.move(face.getOpposite());
            if (!level.isEmptyBlock(pos) || level.isEmptyBlock(pos.relative(face.getOpposite()))) continue;
            BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(face.getOpposite()), true);
            int length = 3 + random.nextInt(8);
            for (int k = 0; k < length && level.isEmptyBlock(pos) && !level.getBlockState(pos.relative(face.getOpposite())).is(BlockTags.LEAVES); k++) {
                level.setBlock(pos, vine, 2);
                pos.move(Direction.DOWN);
            }
        }
    }
}
