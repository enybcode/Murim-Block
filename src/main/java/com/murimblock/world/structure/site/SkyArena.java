package com.murimblock.world.structure.site;

import com.mojang.logging.LogUtils;
import com.murimblock.Murimblock;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * The Son of the Demon's arena (docs/mobs/bandit-warlord.md): the hollowed meteor floating in the sky, built from the
 * weapons session's schematic (art/structures/warlord-sky-arena, a WorldEdit Sponge v3 file of vanilla blocks), shipped in
 * the mod as data/murimblock/schematics/warlord_sky_arena.schem. Placed during the transformation's film, over the camp:
 * its paste point (the middle of the fighting floor, standing on it) at {@link #PASTE_Y}; {@link #BUDGET} blocks a tick,
 * bottom first, air skipped, so the server never freezes. Placed once per camp (BanditCampCount).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class SkyArena {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "schematics/warlord_sky_arena.schem");
    /** Where the paste point stands (the fighting floor is the block below); the schematic reaches some 56 blocks above it. */
    public static final int PASTE_Y = 250;
    /** Blocks a tick: its 1,475,785 blocks in some 185 ticks, well before the players are carried up (WarlordTransformation.CARRY). */
    public static final int BUDGET = 8000;

    /** A schematic read: its size, its offset from the paste point, its palette and its cells (palette indices, x fastest, then z, then y). */
    public record Schematic(int width, int height, int length, int[] offset, BlockState[] palette, int[] cells) {
        int at(int x, int y, int z) { return cells[x + z * width + y * width * length]; }
    }

    private static final class Job {
        final ServerLevel level;
        final Schematic schem;
        final BlockPos paste;
        int index;

        Job(ServerLevel level, Schematic schem, BlockPos paste) {
            this.level = level;
            this.schem = schem;
            this.paste = paste;
        }

        boolean done() { return index >= schem.cells.length; }
    }

    private static final List<Job> JOBS = new ArrayList<>();
    @Nullable private static Schematic cached;
    /** Whether the schematic was found missing (said once, not at every film). */
    private static boolean missing;

    private SkyArena() { }

    /** The schematic, read once from the mod's data (null if missing or unreadable: no arena then). */
    @Nullable public static Schematic schematic(ServerLevel level) {
        if (cached != null || missing) return cached;
        if (level.getServer().getResourceManager().getResource(FILE).isEmpty()) {
            missing = true;
            LOGGER.warn("No sky arena in the mod ({}): the Son of the Demon fights on the ground", FILE);
            return null;
        }
        try (InputStream in = level.getServer().getResourceManager().getResourceOrThrow(FILE).open()) {
            cached = read(level, NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap()));
        } catch (Exception e) {
            LOGGER.error("Could not read the sky arena {}", FILE, e);
        }
        return cached;
    }

    /** Reads a Sponge v3 schematic (the root's "Schematic", or the root itself). */
    static Schematic read(ServerLevel level, CompoundTag root) throws Exception {
        CompoundTag s = root.contains("Schematic") ? root.getCompound("Schematic") : root;
        int w = s.getShort("Width") & 0xffff, h = s.getShort("Height") & 0xffff, l = s.getShort("Length") & 0xffff;
        int[] offset = s.contains("Offset") ? s.getIntArray("Offset") : new int[3];
        CompoundTag blocks = s.getCompound("Blocks");
        CompoundTag paletteTag = blocks.getCompound("Palette");
        BlockState[] palette = new BlockState[paletteTag.size()];
        var lookup = level.holderLookup(Registries.BLOCK);
        for (String key : paletteTag.getAllKeys()) palette[paletteTag.getInt(key)] = BlockStateParser.parseForBlock(lookup, key, false).blockState();
        byte[] data = blocks.getByteArray("Data");
        int[] cells = new int[w * h * l];
        int i = 0, n = 0;
        while (i < data.length && n < cells.length) {
            int value = 0, shift = 0, b;
            do { b = data[i++]; value |= (b & 0x7f) << shift; shift += 7; } while ((b & 0x80) != 0);
            cells[n++] = value;
        }
        if (n != cells.length) throw new IllegalStateException("the arena's block data holds " + n + " cells, not " + cells.length);
        return new Schematic(w, h, l, offset, palette, cells);
    }

    /** Off in the server GameTests that only follow the film: a million and a half blocks would slow the tests after it. */
    public static boolean enabled = true;

    /** Begins placing the arena with its paste point at (x, PASTE_Y, z); false if no schematic. */
    public static boolean place(ServerLevel level, int x, int z) {
        Schematic schem = enabled ? schematic(level) : null;
        if (schem == null) return false;
        JOBS.add(new Job(level, schem, new BlockPos(x, Math.min(PASTE_Y, level.getMaxBuildHeight() - 1 - schem.height - schem.offset[1]), z)));
        return true;
    }

    /** Where a body stands on the middle of the fighting floor, for an arena placed at (x, z). */
    public static BlockPos floorMiddle(ServerLevel level, int x, int z) {
        Schematic schem = schematic(level);
        int y = schem == null ? PASTE_Y : Math.min(PASTE_Y, level.getMaxBuildHeight() - 1 - schem.height - schem.offset[1]);
        return new BlockPos(x, y, z);
    }

    /** Whether an arena is still being built. */
    public static boolean building() {
        return !JOBS.isEmpty();
    }

    /** Places the whole arena now (tests). */
    public static void placeNowForTests(ServerLevel level, int x, int z) {
        place(level, x, z);
        while (!JOBS.isEmpty()) step(JOBS.get(0), Integer.MAX_VALUE);
        JOBS.clear();
    }

    private static void step(Job job, int budget) {
        Schematic s = job.schem;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int placed = 0;
        while (!job.done() && placed < budget) {
            int i = job.index++;
            BlockState state = s.palette[s.cells[i]];
            if (state.isAir()) continue;
            int x = i % s.width, z = (i / s.width) % s.length, y = i / (s.width * s.length);
            p.set(job.paste.getX() + s.offset[0] + x, job.paste.getY() + s.offset[1] + y, job.paste.getZ() + s.offset[2] + z);
            job.level.getChunk(p.getX() >> 4, p.getZ() >> 4);
            job.level.setBlock(p, state, 2 | 16);
            placed++;
        }
        if (job.done()) JOBS.remove(job);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!JOBS.isEmpty()) step(JOBS.get(0), BUDGET);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        JOBS.clear();
        cached = null;
        missing = false;
    }
}
