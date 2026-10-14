package com.murimblock.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
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
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Freestanding rock peaks, the signature of three Murim biomes. They are built the way the real ones were shaped by
 * water and frost, not as plain cylinders:
 * <ul>
 * <li>TOWER: Guilin karst tower (fenglin). A flared foot of scree, steep walls fluted by vertical grooves where rain
 * runs, a few ledges with plants and an overhang under each, a rounded wooded dome. Dark wet streaks run down in long
 * continuous lines, moss on the damp faces, vines down the walls.</li>
 * <li>SPIRE: Huangshan granite. A sheaf of jointed columns of different heights pressed together, each with a
 * weathered rounded top (the "lotus peak" look), moss in the ledges, pines on the summits.</li>
 * <li>PILLAR: Wulingyuan quartz sandstone. A slender rounded-square column, stepping in and out a little along its
 * bedding planes, with a flat green top bearing a few trees; ferns and moss on its ledges.</li>
 * </ul>
 * The rock itself carries the colour variation (accent = streaks or a second stone), the top provider is the soil on
 * flat rock, and an optional summit feature (a tree) is grown on the top. Height and radius are in blocks; the column
 * starts a few blocks under the ground so it never floats.
 */
public class RockPillarFeature extends Feature<RockPillarFeature.Config> {
    public RockPillarFeature() {
        super(Config.CODEC);
    }

    public enum Style implements StringRepresentable {
        TOWER, SPIRE, PILLAR;

        public static final Codec<Style> CODEC = StringRepresentable.fromEnum(Style::values);

        @Override public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Config(Style style, BlockStateProvider rock, BlockStateProvider accent, BlockStateProvider top,
                         IntProvider radius, IntProvider height, Optional<BlockStateProvider> plant,
                         Optional<Holder<ConfiguredFeature<?, ?>>> summit) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Style.CODEC.fieldOf("style").forGetter(Config::style),
                BlockStateProvider.CODEC.fieldOf("rock").forGetter(Config::rock),
                BlockStateProvider.CODEC.fieldOf("accent").forGetter(Config::accent),
                BlockStateProvider.CODEC.fieldOf("top").forGetter(Config::top),
                IntProvider.codec(1, 16).fieldOf("radius").forGetter(Config::radius),
                IntProvider.codec(4, 120).fieldOf("height").forGetter(Config::height),
                BlockStateProvider.CODEC.optionalFieldOf("plant").forGetter(Config::plant),
                ConfiguredFeature.CODEC.optionalFieldOf("summit").forGetter(Config::summit)
        ).apply(i, Config::new));
    }

    /** One column of a granite sheaf: its centre offset, radius and height (relative). */
    private record Column(double x, double z, double radius, double height) { }

    @Override public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Config config = context.config();
        BlockPos origin = context.origin();
        int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        if (ground <= level.getMinBuildHeight() + 8) return false;
        BlockState rockState = config.rock().getState(random, origin);
        // Not on top of another peak.
        if (level.getBlockState(new BlockPos(origin.getX(), ground - 1, origin.getZ())).is(rockState.getBlock())) return false;
        int radius = config.radius().sample(random);
        int height = config.height().sample(random);
        // Keep peaks apart, so they rise as separate towers and never melt into walls.
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int k = 0; k < 16; k++) {
            double a = k * Math.PI / 8;
            for (int reach : new int[]{radius + 2, radius * 2 + 3}) {
                probe.set(origin.getX() + Math.cos(a) * reach, ground + 8, origin.getZ() + Math.sin(a) * reach);
                if (level.getBlockState(probe).is(rockState.getBlock())) return false;
            }
        }
        SimplexNoise noise = new SimplexNoise(new WorldgenRandom(new LegacyRandomSource(random.nextLong())));
        int bottom = ground - 5;
        int top = ground + height;
        // A gentle lean and meander, so the axis is never a ruler line.
        double leanAngle = random.nextDouble() * Math.PI * 2, lean = random.nextDouble() * 0.06;
        double squash = 0.8 + random.nextDouble() * 0.4;
        // Ledges (towers, pillars): heights where the wall steps out by a block, with an overhang under it.
        List<Integer> ledges = new ArrayList<>();
        int nLedges = switch (config.style()) {
            case SPIRE -> 0;
            case PILLAR -> random.nextInt(2);
            case TOWER -> 1 + random.nextInt(3);
        };
        for (int k = 0; k < nLedges; k++) ledges.add(ground + 6 + random.nextInt(Math.max(1, height - 14)));
        // Granite: the sheaf of columns.
        List<Column> columns = new ArrayList<>();
        if (config.style() == Style.SPIRE) {
            int n = 3 + random.nextInt(4);
            for (int k = 0; k < n; k++) {
                double a = random.nextDouble() * Math.PI * 2, d = k == 0 ? 0 : radius * (0.45 + random.nextDouble() * 0.55);
                columns.add(new Column(Math.cos(a) * d, Math.sin(a) * d, Math.max(1.6, radius * (0.42 + random.nextDouble() * 0.38)),
                        k == 0 ? 1.0 : 0.45 + random.nextDouble() * 0.5));
            }
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int reach = radius * 2 + 3;
        // Sandstone pillars stand straight; karst and granite wander a little more.
        double wander = config.style() == Style.PILLAR ? 0.35 : 1.2;
        if (config.style() == Style.PILLAR) lean *= 0.4;
        for (int y = bottom; y <= top; y++) {
            double t = (double) (y - ground) / height;
            double meander = noise.getValue(y * 0.05, 0, 0) * wander;
            double cx = origin.getX() + 0.5 + Math.cos(leanAngle) * lean * (y - ground) + meander;
            double cz = origin.getZ() + 0.5 + Math.sin(leanAngle) * lean * (y - ground) + noise.getValue(0, y * 0.05, 3) * wander;
            boolean ledge = ledges.contains(y), underLedge = ledges.contains(y + 1);
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    int x = Mth.floor(cx) + dx, z = Mth.floor(cz) + dz;
                    double ox = x + 0.5 - cx, oz = (z + 0.5 - cz) / squash;
                    if (!inside(config.style(), noise, columns, ox, oz, x, y, z, height, t, radius, ledge, underLedge)) continue;
                    pos.set(x, y, z);
                    BlockState existing = level.getBlockState(pos);
                    if (existing.is(Blocks.BEDROCK)) continue;
                    level.setBlock(pos, material(config, random, noise, x, y, z), 2);
                }
            }
        }
        scree(level, random, noise, config, origin, ground, radius);
        dress(level, random, config, origin, ground, top, reach);
        if (config.style() == Style.TOWER) hangVines(level, random, origin, ground, top, radius);
        summit(context, level, random, config, origin, top, reach);
        return true;
    }

    /** Whether (ox, oz) at relative height t is rock. */
    private static boolean inside(Style style, SimplexNoise noise, List<Column> columns, double ox, double oz, int x, int y, int z, int height,
                                  double t, int radius, boolean ledge, boolean underLedge) {
        double dist = Math.hypot(ox, oz);
        double angle = Math.atan2(oz, ox);
        switch (style) {
            case TOWER -> {
                double r;
                if (t < 0) r = radius * 1.35;                                  // buried foot
                else if (t < 0.1) r = radius * (1.35 - t * 3.5);               // flared foot of scree
                else if (t < 0.8) r = radius * (1.0 - 0.12 * (t - 0.1));       // steep walls, a little taper
                else r = radius * 0.92 * Math.sqrt(Math.max(0, 1 - Math.pow((t - 0.8) / 0.2, 2))); // rounded dome
                // Vertical fluting: the angle sets the groove, the height barely changes it, so grooves run down.
                double flute = noise.getValue(Math.cos(angle) * 1.6 + x * 0.02, y * 0.035, Math.sin(angle) * 1.6 + z * 0.02) * 0.17
                        + noise.getValue(x * 0.21, y * 0.09, z * 0.21) * 0.07;
                double shelf = ledge ? 1.1 : underLedge ? -0.8 : 0;
                return dist <= r * (1 + flute) + shelf;
            }
            case SPIRE -> {
                if (t < 0) return dist <= radius * 1.2;
                for (Column c : columns) {
                    double d = Math.hypot(ox - c.x, oz - c.z);
                    if (t > c.height) continue;
                    double r = c.radius * (1 - 0.15 * t);
                    // A weathered, rounded top over the column's last blocks.
                    double cap = c.radius * 1.3 / height;
                    if (t > c.height - cap) r *= Math.sqrt(Math.max(0, (c.height - t) / cap));
                    // Weathered joints: faint bulges and hollows.
                    r += noise.getValue(x * 0.25, y * 0.12, z * 0.25) * 0.6;
                    if (d <= r) return true;
                }
                return false;
            }
            default -> { // PILLAR
                double r = t < 0 ? radius * 1.15 : t < 0.04 ? radius * 1.08 : radius * (1.0 - 0.1 * t);
                // Rounded square: between a square and a circle.
                double sq = Math.max(Math.abs(ox), Math.abs(oz)) * 0.72 + dist * 0.28;
                double erosion = noise.getValue(Math.cos(angle) * 2 + x * 0.03, y * 0.04, Math.sin(angle) * 2 + z * 0.03) * 0.09;
                double step = ledge ? 0.6 : underLedge ? -0.4 : 0;
                if (t > 0.97) r *= 0.9; // a slightly rounded rim at the top
                return sq <= r * (1 + erosion) + step;
            }
        }
    }

    /**
     * The rock at a block: the main stone, with the accent (dark wet streaks on a karst tower, a second stone in long
     * vertical runs on granite and sandstone) where a stretched noise says so.
     */
    private static BlockState material(Config config, RandomSource random, SimplexNoise noise, int x, int y, int z) {
        BlockPos at = new BlockPos(x, y, z);
        double streak = noise.getValue(x * 0.55, y * 0.025, z * 0.55);
        double threshold = switch (config.style()) {
            case TOWER -> 0.5;
            case SPIRE -> 0.62;
            case PILLAR -> 0.68;
        };
        if (streak > threshold) return config.accent().getState(random, at);
        return config.rock().getState(random, at);
    }

    /** Fallen blocks and boulders around the foot (a karst tower or a granite sheaf sheds them). */
    private static void scree(WorldGenLevel level, RandomSource random, SimplexNoise noise, Config config, BlockPos origin, int ground, int radius) {
        if (config.style() == Style.PILLAR) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int count = 3 + random.nextInt(5);
        for (int k = 0; k < count; k++) {
            double a = random.nextDouble() * Math.PI * 2, d = radius * 1.3 + 1 + random.nextDouble() * 3;
            int x = origin.getX() + (int) Math.round(Math.cos(a) * d), z = origin.getZ() + (int) Math.round(Math.sin(a) * d);
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            if (!level.getBlockState(pos.set(x, y - 1, z)).isSolid()) continue;
            int size = random.nextInt(3);
            for (int dy = 0; dy <= (size == 2 ? 1 : 0); dy++) {
                for (int ddx = 0; ddx <= (size > 0 ? 1 : 0); ddx++) for (int ddz = 0; ddz <= (size > 0 ? 1 : 0); ddz++) {
                    if (dy == 1 && random.nextBoolean()) continue;
                    pos.set(x + ddx, y + dy, z + ddz);
                    if (level.isEmptyBlock(pos) || level.getBlockState(pos).is(BlockTags.REPLACEABLE)) {
                        level.setBlock(pos, material(config, random, noise, x, y, z), 2);
                    }
                }
            }
        }
    }

    /**
     * Soil and plants on every flat rock surface of the peak (summit, ledges): the top provider (grass, moss), then low
     * plants on it; damp mossy patches on the karst rock near those.
     */
    private static void dress(WorldGenLevel level, RandomSource random, Config config, BlockPos origin, int ground, int top, int reach) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState rock = config.rock().getState(random, origin);
        BlockState accent = config.accent().getState(random, origin);
        for (int dx = -reach - 2; dx <= reach + 2; dx++) {
            for (int dz = -reach - 2; dz <= reach + 2; dz++) {
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                // Walk down the column: every rock block with air above it is a ledge or the summit.
                for (int y = top + 1; y > ground + 2; y--) {
                    pos.set(x, y, z);
                    BlockState s = level.getBlockState(pos);
                    if (!s.is(rock.getBlock()) && !s.is(accent.getBlock())) continue;
                    if (!level.isEmptyBlock(pos.above())) continue;
                    // Only real flats hold soil: most of the neighbours at this height must be open rock tops too.
                    // A lone step in a fluted wall gets moss instead (soil there would show its dirt sides).
                    int flat = 0;
                    for (int ox = -1; ox <= 1; ox++) for (int oz = -1; oz <= 1; oz++) {
                        BlockPos n = pos.offset(ox, 0, oz);
                        if (level.getBlockState(n).isSolid() && level.isEmptyBlock(n.above())) flat++;
                    }
                    if (flat < 6) {
                        if (config.style() != Style.PILLAR && random.nextFloat() < 0.5f) {
                            level.setBlock(pos.above(), Blocks.MOSS_CARPET.defaultBlockState(), 2);
                        }
                        continue;
                    }
                    // Granite keeps much of its top bare; ledges hold soil more often than not.
                    if (config.style() == Style.SPIRE && random.nextFloat() < 0.45f) continue;
                    level.setBlock(pos, config.top().getState(random, pos), 2);
                    BlockPos above = pos.above();
                    float roll = random.nextFloat();
                    if (roll < 0.28f) {
                        BlockState plant = config.plant().map(p -> p.getState(random, above)).orElse(Blocks.SHORT_GRASS.defaultBlockState());
                        if (plant.canSurvive(level, above)) level.setBlock(above, plant, 2);
                    } else if (roll < 0.36f) {
                        level.setBlock(above, Blocks.FERN.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /** Vines down the walls of a karst tower, from under its ledges and its dome. */
    private static void hangVines(WorldGenLevel level, RandomSource random, BlockPos origin, int ground, int top, int radius) {
        int attempts = 14 + radius * 4;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < attempts; i++) {
            Direction face = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int y = Mth.nextInt(random, ground + 4, Math.max(ground + 5, top - 3));
            int along = random.nextInt(radius * 2 + 1) - radius;
            int x = origin.getX() + face.getStepX() * (radius * 2 + 3) + (face.getAxis() == Direction.Axis.Z ? along : 0);
            int z = origin.getZ() + face.getStepZ() * (radius * 2 + 3) + (face.getAxis() == Direction.Axis.X ? along : 0);
            pos.set(x, y, z);
            // Walk inward until touching rock.
            for (int k = 0; k < radius * 2 + 4 && level.isEmptyBlock(pos.relative(face.getOpposite())); k++) pos.move(face.getOpposite());
            if (!level.isEmptyBlock(pos) || level.isEmptyBlock(pos.relative(face.getOpposite()))) continue;
            BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(face.getOpposite()), true);
            int length = 4 + random.nextInt(12);
            for (int k = 0; k < length && level.isEmptyBlock(pos) && level.getBlockState(pos.relative(face.getOpposite())).isSolid(); k++) {
                level.setBlock(pos, vine, 2);
                pos.move(Direction.DOWN);
            }
        }
    }

    /** A few trees on the summit (and the ledges of a tower), from the configured summit feature. */
    private static void summit(FeaturePlaceContext<Config> context, WorldGenLevel level, RandomSource random, Config config,
                               BlockPos origin, int top, int reach) {
        if (config.summit().isEmpty()) return;
        int tries = config.style() == Style.SPIRE ? 2 : 4;
        for (int k = 0; k < tries; k++) {
            int x = origin.getX() + random.nextInt(reach + 1) - reach / 2, z = origin.getZ() + random.nextInt(reach + 1) - reach / 2;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            if (y < top - 12) continue;
            config.summit().get().value().place(level, context.chunkGenerator(), random, new BlockPos(x, y, z));
        }
    }
}
