package com.murimblock.cultivation.inner;

import com.murimblock.world.block.MurimBlocks;
import com.murimblock.world.structure.kit.BuildKit;
import com.murimblock.world.structure.kit.RoofTiles;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A cultivator's island in the inner world, built to their image (docs/cultivation/qi-cultivation.md):
 * <ul>
 *   <li>its size and what grows on it follow the realm: a bare rock in the mist at Qi Sensing, a pond from Qi Guiding,
 *   pines from Qi Condensation, floating stones around from Qi Liquefaction, a golden core above from Core Formation;</li>
 *   <li>its ground, stone, trees, water and landmarks follow the main style and its element, one world per martial
 *   art (InnerTheme, InnerWorlds; Enzo 19/10/2026): a sea of clouds and a pavilion for the Azure Cloud Sword, a
 *   training yard for the Long Fist, a bell tower for the Golden Bell, a storm for the Thunder Saber...;</li>
 *   <li>a stele stands round the meditation platform for each art learned, and each failed great breakthrough leaves a
 *   crack across the ground (the red mist of an unstable foundation is drawn by the client).</li>
 * </ul>
 * The platform in the middle is where the cultivator arrives; the Heart Demon rises in front of it.
 */
public final class InnerIsland {
    /** The island's radius per realm. */
    static final int[] RADIUS = {7, 9, 12, 15, 18, 22, 26, 30, 34};
    /** The height of the island's ground. */
    public static final int GROUND = 100;

    /** What the island is built from: a change of any rebuilds it. */
    public record Look(int realm, Optional<ResourceLocation> mainArt, List<ResourceLocation> arts, int failures) {
        public String signature() {
            return realm + "|" + mainArt.map(Object::toString).orElse("-") + "|" + arts + "|" + Math.min(3, failures);
        }

        int radius() {
            return RADIUS[Math.max(0, Math.min(RADIUS.length - 1, realm))];
        }
    }

    /** Where things stand on a built island (absolute positions). */
    public record Layout(BlockPos centre, BlockPos arrival, BlockPos demon, BlockPos soul, int radius) {
    }

    private InnerIsland() {
    }

    public static Layout layout(BlockPos centre, int radius) {
        return new Layout(centre, centre.offset(0, 1, 0), centre.offset(0, 1, -5),
                centre.offset((int) Math.round(radius * 0.3), 1, (int) Math.round(radius * 0.65)), radius);
    }

    /** A little deterministic noise from a position (0..1). */
    static double noise(int x, int z, int salt) {
        long h = x * 73856093L ^ z * 19349663L ^ salt * 83492791L;
        h = (h ^ (h >>> 13)) * 0x5bd1e995L;
        return ((h ^ (h >>> 15)) & 0xFFFF) / 65535.0;
    }

    /** Clears what was there (an island of `oldRadius`), then builds the island of `look` around `centre`. */
    public static Layout build(ServerLevel level, BlockPos centre, Look look, int oldRadius) {
        int cx = centre.getX(), cz = centre.getZ();
        // Load the chunks first (empty: the inner world generates nothing).
        // The themes' landmarks reach beyond the island (peaks, floating blocks, the demon's eye).
        int reach = Math.max(oldRadius, look.radius()) + 30;
        for (int x = cx - reach; x <= cx + reach; x += 16) for (int z = cz - reach; z <= cz + reach; z += 16) level.getChunk(x >> 4, z >> 4);
        BuildKit kit = new BuildKit(level, cx, cz);
        if (oldRadius > 0) {
            int r = oldRadius + 28;
            kit.fillRel(-r, GROUND - 45, -r, r, GROUND + 30, r, Blocks.AIR.defaultBlockState());
        }
        int radius = look.radius();
        InnerTheme theme = InnerTheme.of(look.mainArt());
        boolean azure = theme == InnerTheme.AZURE_CLOUD, fist = theme == InnerTheme.LONG_FIST;
        rock(kit, radius, theme);
        if (azure) clouds(kit, radius);
        platform(kit);
        if (look.realm() >= 1 && InnerWorlds.pond(theme)) pond(kit, radius, theme == InnerTheme.GOLDEN_BELL);
        if (look.realm() >= 2) {
            if (theme == InnerTheme.PLAIN || azure) pines(kit, radius);
            else InnerWorlds.trees(kit, theme, radius);
        }
        if (look.realm() >= 3) floatingStones(kit, radius, InnerWorlds.floating(theme));
        if (look.realm() >= 6) core(kit);
        if (azure) pavilion(kit, radius);
        if (fist) dummies(kit, radius);
        InnerWorlds.landmarks(kit, theme, radius);
        steles(level, kit, centre, look.arts());
        cracks(kit, radius, look.failures());
        lights(level, kit, centre, radius);
        return layout(centre, radius);
    }

    private static BlockState s(net.minecraft.world.level.block.Block block) {
        return block.defaultBlockState();
    }

    /** The floating rock: its theme's ground, two layers of soil under it, then its stone tapering to a point. */
    private static void rock(BuildKit kit, int radius, InnerTheme theme) {
        for (int x = -radius - 3; x <= radius + 3; x++) for (int z = -radius - 3; z <= radius + 3; z++) {
            double d = Math.hypot(x, z);
            double edge = radius + (noise(x, z, 1) - 0.5) * 2.4;
            if (d > edge || InnerWorlds.eaten(theme, x, z, d, radius)) continue;
            int depth = (int) Math.round(2 + (edge - d) * 1.1 + noise(x, z, 2) * 3);
            BlockState top = InnerWorlds.top(theme, x, z, d, radius);
            kit.setRel(x, GROUND, z, top);
            for (int y = 1; y <= depth; y++) {
                BlockState under = y <= 2 ? InnerWorlds.soil(theme) : InnerWorlds.rock(theme, noise(x, z + y, 4) < 0.25);
                kit.setRel(x, GROUND - y, z, under);
            }
            // Short grass and ferns here and there.
            if (top.is(Blocks.GRASS_BLOCK) && InnerWorlds.grassy(theme) && d > 3.5 && noise(x, z, 5) < 0.18) kit.setRel(x, GROUND + 1, z, s(Blocks.SHORT_GRASS));
        }
    }

    /** The meditation platform: a round of smooth stone ringed with polished andesite. */
    private static void platform(BuildKit kit) {
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            double d = Math.hypot(x, z);
            if (d <= 2.2) kit.setRel(x, GROUND, z, s(Blocks.SMOOTH_STONE));
            else if (d <= 3.2) kit.setRel(x, GROUND, z, s(Blocks.POLISHED_ANDESITE));
            if (d <= 3.2) kit.setRel(x, GROUND + 1, z, s(Blocks.AIR));
        }
    }

    private static void pond(BuildKit kit, int radius, boolean lotus) {
        int px = (int) Math.round(radius * 0.5), pz = (int) Math.round(radius * 0.35);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            if (Math.hypot(x, z) > 1.8) continue;
            kit.setRel(px + x, GROUND + 1, pz + z, s(Blocks.AIR));
            kit.setRel(px + x, GROUND, pz + z, s(Blocks.WATER));
            kit.setRel(px + x, GROUND - 1, pz + z, s(Blocks.CLAY));
        }
        kit.setRel(px, GROUND + 1, pz + 1, lotus ? s(MurimBlocks.LOTUS_BLOSSOM.get()) : s(Blocks.LILY_PAD));
        kit.setRel(px - 1, GROUND + 1, pz - 1, lotus ? s(MurimBlocks.LOTUS_PAD.get()) : s(Blocks.LILY_PAD));
    }

    private static void pines(BuildKit kit, int radius) {
        BlockState log = s(MurimBlocks.HUANGSHAN_PINE.log().get());
        BlockState leaves = MurimBlocks.HUANGSHAN_PINE.leaves().get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (double angle : new double[]{0.0, 4.25, 5.6}) {
            int x = (int) Math.round(Math.cos(angle) * (radius - 2.5)), z = (int) Math.round(Math.sin(angle) * (radius - 2.5));
            int height = 4 + (int) (noise(x, z, 6) * 3);
            for (int y = 1; y <= height; y++) kit.setRel(x, GROUND + y, z, log);
            // Flat tiers of needles, wide below, narrow on top, like the pines of the Yellow Mountain.
            for (int tier = 0; tier < 2; tier++) {
                int ty = GROUND + height - 1 + tier * 2, r = 2 - tier;
                for (int dx = -r - 1; dx <= r + 1; dx++) for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > r + 1) continue;
                    if (dx == 0 && dz == 0 && tier == 0) continue;
                    kit.setRel(x + dx, ty, z + dz, leaves);
                }
            }
            kit.setRel(x, GROUND + height + 1, z, leaves);
        }
    }

    private static void floatingStones(BuildKit kit, int radius, BlockState[] look) {
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3 + 0.3;
            int x = (int) Math.round(Math.cos(angle) * (radius + 6)), z = (int) Math.round(Math.sin(angle) * (radius + 6));
            int y = GROUND + 2 + (int) (noise(i, 0, 7) * 5);
            kit.fillRel(x - 1, y, z - 1, x + 1, y, z + 1, look[0]);
            kit.setRel(x, y - 1, z, look[0]);
            kit.setRel(x, y + 1, z, look[1]);
        }
    }

    /** The golden core, floating high above the platform: a sphere of gold around a glowing heart. */
    private static void core(BuildKit kit) {
        int cy = GROUND + 10;
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) for (int z = -2; z <= 2; z++) {
            double d = Math.sqrt(x * x + y * y + z * z);
            if (d <= 1.0) kit.setRel(x, cy + y, z, s(Blocks.GLOWSTONE));
            else if (d <= 2.3) kit.setRel(x, cy + y, z, s(Blocks.GOLD_BLOCK));
        }
    }

    /** The sea of clouds under the Azure Cloud Sword's island: banks of white around and below it. */
    private static void clouds(BuildKit kit, int radius) {
        for (int x = -radius - 10; x <= radius + 10; x++) for (int z = -radius - 10; z <= radius + 10; z++) {
            double d = Math.hypot(x, z);
            if (d < radius - 1 || d > radius + 10) continue;
            if (noise(x, z, 8) < 0.45) continue;
            int y = GROUND - 4 - (int) (noise(x, z, 9) * 2);
            kit.setRel(x, y, z, s(Blocks.WHITE_WOOL));
            if (noise(x, z, 10) < 0.3) kit.setRel(x, y + 1, z, s(Blocks.WHITE_WOOL));
        }
    }

    /** A small open pavilion behind the platform, its green-glazed roof turned up at the corners. */
    private static void pavilion(BuildKit kit, int radius) {
        int z0 = -radius + 2, z1 = z0 + 4;
        kit.fillRel(-2, GROUND, z0, 2, GROUND, z1, s(Blocks.POLISHED_ANDESITE));
        for (int[] p : new int[][]{{-2, z0}, {2, z0}, {-2, z1}, {2, z1}}) {
            for (int y = 1; y <= 3; y++) kit.setRel(p[0], GROUND + y, p[1], s(Blocks.STRIPPED_DARK_OAK_LOG));
        }
        kit.roofRel(-2, z0, 2, z1, GROUND + 4, RoofTiles.GREEN_GLAZED, true);
    }

    /** Three wooden dummies round the Long Fist's yard: a post with two arms. */
    private static void dummies(BuildKit kit, int radius) {
        for (double angle : new double[]{2.6, 3.2, 3.8}) {
            int x = (int) Math.round(Math.cos(angle) * radius * 0.55), z = (int) Math.round(Math.sin(angle) * radius * 0.55);
            kit.setRel(x, GROUND + 1, z, s(Blocks.STRIPPED_OAK_LOG));
            kit.setRel(x, GROUND + 2, z, s(Blocks.STRIPPED_OAK_LOG));
            kit.setRel(x, GROUND + 3, z, s(Blocks.OAK_LOG));
            kit.setRel(x + 1, GROUND + 2, z, s(Blocks.OAK_FENCE));
            kit.setRel(x - 1, GROUND + 2, z, s(Blocks.OAK_FENCE));
        }
    }

    /** A stele for each art learned, round the platform, its name on a sign facing the middle. */
    private static void steles(ServerLevel level, BuildKit kit, BlockPos centre, List<ResourceLocation> arts) {
        for (int i = 0; i < arts.size() && i < 8; i++) {
            double angle = Math.PI / 2 + (i - (arts.size() - 1) / 2.0) * 0.55;
            int x = (int) Math.round(Math.cos(angle) * 4.5), z = (int) Math.round(Math.sin(angle) * 4.5);
            kit.setRel(x, GROUND + 1, z, s(Blocks.CHISELED_STONE_BRICKS));
            kit.setRel(x, GROUND + 2, z, s(Blocks.STONE_BRICKS));
            kit.setRel(x, GROUND + 3, z, s(Blocks.STONE_BRICK_SLAB));
            // The sign in front, facing the platform.
            double toward = Math.atan2(-z, -x);
            int sx = x + (int) Math.round(Math.cos(toward)), sz = z + (int) Math.round(Math.sin(toward));
            int rotation = Math.floorMod(Math.round((float) (Math.toDegrees(Math.atan2(x, -z)) / 22.5)) + 8, 16);
            BlockPos at = centre.offset(sx, 1, sz);
            level.setBlock(at, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation), 3);
            if (level.getBlockEntity(at) instanceof SignBlockEntity sign) {
                Component name = Component.translatable("art." + arts.get(i).getNamespace() + "." + arts.get(i).getPath());
                SignText text = new SignText().setMessage(1, name).setHasGlowingText(true);
                sign.setText(text, true);
                sign.setText(text, false);
                sign.setWaxed(true);
                sign.setChanged();
            }
        }
    }

    /** A crack across the ground for each failed great breakthrough (at most three). */
    private static void cracks(BuildKit kit, int radius, int failures) {
        for (int i = 0; i < Math.min(3, failures); i++) {
            double angle = i * 2.1 + 0.9;
            for (double r = 3.5; r < radius * 0.85; r += 0.5) {
                double wobble = Math.sin(r * 1.7 + i) * 0.6;
                int x = (int) Math.round(Math.cos(angle) * r - Math.sin(angle) * wobble), z = (int) Math.round(Math.sin(angle) * r + Math.cos(angle) * wobble);
                kit.setRel(x, GROUND, z, s(Blocks.CRACKED_DEEPSLATE_TILES));
                kit.setRel(x, GROUND + 1, z, s(Blocks.AIR));
            }
        }
    }

    /** Light without lamps: the inner world glows faintly by itself (invisible light blocks over the ground). */
    private static void lights(ServerLevel level, BuildKit kit, BlockPos centre, int radius) {
        BlockState light = s(Blocks.LIGHT).setValue(LightBlock.LEVEL, 13);
        for (int x = -radius; x <= radius; x += 5) for (int z = -radius; z <= radius; z += 5) {
            if (Math.hypot(x, z) > radius - 1) continue;
            if (level.getBlockState(new BlockPos(centre.getX() + x, GROUND + 4, centre.getZ() + z)).isAir()) kit.setRel(x, GROUND + 4, z, light);
        }
    }

    /** The direction the arriving cultivator faces: the demon's side (north). */
    public static float arrivalYaw() {
        return Direction.NORTH.toYRot();
    }
}
