package com.murimblock.world.structure;

import com.mojang.logging.LogUtils;
import com.murimblock.Murimblock;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.martial.SevenStarsItems;
import com.murimblock.world.map.MurimMap;
import java.util.Comparator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * The Star-Plucking Peak ({@link StarPeakBuilder}) is unique: built once per Murim world, deep in the north (the region's
 * heart, or the nearest place round it that is still the north, away from the coast and from every site), its summit on
 * the world's highest block. Its position is kept in the overworld's saved data. Built over server ticks after the
 * sanctuary and the Crab King's lair, like them ({@link Job}).
 * <p>
 * The Seven Stars Sword is found there (Enzo, 17/10/2026): a player at Qi Liquidation or beyond who meditates three
 * minutes on the summit is given its manual and its florett, once ({@link #meditating}).
 */
public final class StarPeakPlacement extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NAME = Murimblock.MOD_ID + "_star_peak";
    /** How long a player meditates on the summit before the stars answer (ticks: three minutes). */
    public static final int MEDITATION_TICKS = 3 * 60 * 20;
    /** The realm the stars answer from. */
    public static final CultivationRealm REALM = CultivationRealm.QI_LIQUIDATION;
    /** How far from the summit's centre a player still counts as on it (blocks, horizontally). */
    public static final double SUMMIT_REACH = 4.5;
    /** The player's saved mark: given the Seven Stars Sword on the peak. */
    static final String GIFT = Murimblock.MOD_ID + ":star_peak_gift";
    /** Where to look for it: this share of the bubble's radius out, in the north's quarter. */
    static final double HEART_SHARE = 0.55;

    @Nullable private BlockPos summit;

    public static final SavedData.Factory<StarPeakPlacement> FACTORY = new SavedData.Factory<>(StarPeakPlacement::new, StarPeakPlacement::load);

    public static StarPeakPlacement get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    /** The summit (the centre of its platform, on the world's highest block), or null if the peak is not built. */
    @Nullable public BlockPos summit() { return summit; }

    /** For tests and commands: record a summit built elsewhere (null: forget it). */
    public void record(@Nullable BlockPos summit) {
        this.summit = summit;
        setDirty();
    }

    /** Whether a player stands on the summit: on its platform, on the top block. */
    public static boolean onSummit(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return false;
        BlockPos s = get(player.serverLevel()).summit;
        if (s == null) return false;
        double dx = player.getX() - (s.getX() + 0.5), dz = player.getZ() - (s.getZ() + 0.5);
        return dx * dx + dz * dz <= SUMMIT_REACH * SUMMIT_REACH && player.getY() >= s.getY() + 0.5 && player.getY() <= s.getY() + 3;
    }

    /**
     * Each tick a player meditates, `seated` ticks so far (MeditationService): on the summit, the stars turn above them; at
     * three minutes, a player at Qi Liquidation or beyond is given the Seven Stars Sword's manual and florett, once. Below
     * the realm, they are told the stars stay silent. Returns whether the gift was given now.
     */
    public static boolean meditating(ServerPlayer player, long seated) {
        if (seated % 20 != 0 || !onSummit(player)) return false;
        boolean ready = CultivationService.getRealm(player).ordinal() >= REALM.ordinal();
        boolean given = player.getPersistentData().getBoolean(GIFT);
        if (seated == 40) {
            player.displayClientMessage(Component.translatable(given ? "message.murimblock.star_peak.again"
                    : ready ? "message.murimblock.star_peak.stars" : "message.murimblock.star_peak.silent", REALM.displayName()), true);
            return false;
        }
        if (seated < MEDITATION_TICKS || given || !ready) return false;
        give(player);
        return true;
    }

    /** The Seven Stars Sword's manual and florett, into the player's hands (or at their feet), and the mark. */
    static void give(ServerPlayer player) {
        player.getPersistentData().putBoolean(GIFT, true);
        for (ItemStack stack : new ItemStack[]{new ItemStack(SevenStarsItems.MANUAL.get()), new ItemStack(SevenStarsItems.FLORETT.get())})
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        player.displayClientMessage(Component.translatable("message.murimblock.star_peak.gift"), false);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 0.6F);
        LOGGER.info("{} was given the Seven Stars Sword on the Star-Plucking Peak", player.getGameProfile().getName());
    }

    /** For tests: whether a player was given it. */
    public static boolean given(ServerPlayer player) {
        return player.getPersistentData().getBoolean(GIFT);
    }

    // ---------------------------------------------------------------- the build

    @Nullable private static volatile Job job;

    public static boolean building() {
        return job != null;
    }

    public static void onServerStarted(ServerStartedEvent event) {
        job = null;
        ServerLevel overworld = event.getServer().overworld();
        StarPeakPlacement data = get(overworld);
        if (data.summit != null) return;
        if (!(overworld.getChunkSource().getGenerator().getBiomeSource() instanceof com.murimblock.world.map.MurimBiomeSource)) return;
        job = new Job(overworld, data);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        Job current = job;
        if (current == null || current.level.getServer() != event.getServer()) return;
        // After the sanctuary and the Crab King's lair: one build at a time.
        if (SanctuaryPlacement.building() || CrabLairPlacement.building()) return;
        if (!current.tick()) job = null;
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        job = null;
    }

    /** Search, then loading the chunks under it with a region ticket, then building, a few milliseconds per tick. */
    static final class Job {
        static final long BUILD_BUDGET_NANOS = 12_000_000L;
        /** Chunks loaded round the summit: the mountain and a margin. */
        static final int CHUNKS = StarPeakBuilder.RADIUS / 16 + 2;
        static final int LOAD_TIMEOUT_TICKS = 1200;
        static final TicketType<ChunkPos> TICKET = TicketType.create(Murimblock.MOD_ID + ":star_peak", Comparator.comparingLong(ChunkPos::toLong));

        final ServerLevel level;
        final StarPeakPlacement data;
        final long started = System.nanoTime();
        @Nullable BlockPos site;
        int loadTicks, buildTicks;
        @Nullable StarPeakBuilder builder;

        Job(ServerLevel level, StarPeakPlacement data) {
            this.level = level;
            this.data = data;
        }

        boolean tick() {
            if (site == null) {
                site = search(level);
                level.getChunkSource().addRegionTicket(TICKET, new ChunkPos(site), CHUNKS, new ChunkPos(site));
                return true;
            }
            if (builder == null) {
                if (!chunksReady() && ++loadTicks < LOAD_TIMEOUT_TICKS) return true;
                builder = prepare(level, site);
            }
            buildTicks++;
            if (builder.buildSome(BUILD_BUDGET_NANOS)) return true;
            level.getChunkSource().removeRegionTicket(TICKET, new ChunkPos(site), CHUNKS, new ChunkPos(site));
            data.record(builder.summit());
            LOGGER.info("Star-Plucking Peak built at {}: {} ms in all, {} build ticks", data.summit, (System.nanoTime() - started) / 1_000_000L, buildTicks);
            return false;
        }

        private boolean chunksReady() {
            int cx = site.getX() >> 4, cz = site.getZ() >> 4;
            for (int x = cx - CHUNKS; x <= cx + CHUNKS; x++) for (int z = cz - CHUNKS; z <= cz + CHUNKS; z++)
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
            return true;
        }
    }

    /** How far the peak reaches from its summit, for SiteSpacing. */
    static final int REACH = StarPeakBuilder.RADIUS;

    /**
     * Where to raise it: the north's heart, or the nearest point round it, ring by ring every 256 blocks, where the north
     * holds all round the mountain, far from the coast, and no site comes near.
     */
    static BlockPos search(ServerLevel level) {
        int[] heart = MurimMap.heart(MurimMap.Region.NORTH, HEART_SHARE);
        for (int ring = 0; ring <= 16; ring++)
            for (int k = 0; k < Math.max(1, ring * 8); k++) {
                double a = ring == 0 ? 0 : k * Math.PI * 2 / (ring * 8);
                int x = heart[0] + (int) Math.round(Math.cos(a) * ring * 256), z = heart[1] + (int) Math.round(Math.sin(a) * ring * 256);
                if (fits(level, x, z)) return new BlockPos(x, 0, z);
            }
        return new BlockPos(heart[0], 0, heart[1]);
    }

    static boolean fits(ServerLevel level, int x, int z) {
        if (MurimMap.reach(x, z) > MurimMap.RADIUS - MurimMap.EDGE - 2 * REACH) return false;
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6;
            if (MurimMap.region(x + (int) (Math.cos(a) * (REACH + 32)), z + (int) (Math.sin(a) * (REACH + 32))) != MurimMap.Region.NORTH) return false;
        }
        return MurimMap.region(x, z) == MurimMap.Region.NORTH && !com.murimblock.world.structure.site.SiteSpacing.crowded(level, x, z, REACH);
    }

    /** The builder for a site: its slopes meet the ground at the lowest of the ground round its foot. */
    static StarPeakBuilder prepare(ServerLevel level, BlockPos site) {
        int base = Integer.MAX_VALUE;
        for (int k = 0; k < 24; k++) {
            double a = k * Math.PI / 12;
            base = Math.min(base, level.getHeight(Heightmap.Types.OCEAN_FLOOR, site.getX() + (int) (Math.cos(a) * StarPeakBuilder.RADIUS * 0.7),
                    site.getZ() + (int) (Math.sin(a) * StarPeakBuilder.RADIUS * 0.7)));
        }
        return new StarPeakBuilder(level, site.getX(), site.getZ(), Math.max(level.getSeaLevel(), base), level.getMaxBuildHeight() - 1, level.getSeed());
    }

    // ---------------------------------------------------------------- saving

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (summit != null) tag.put("summit", NbtUtils.writeBlockPos(summit));
        return tag;
    }

    private static StarPeakPlacement load(CompoundTag tag, HolderLookup.Provider registries) {
        StarPeakPlacement data = new StarPeakPlacement();
        data.summit = NbtUtils.readBlockPos(tag, "summit").orElse(null);
        return data;
    }
}
