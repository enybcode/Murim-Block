package com.murimblock.world.structure.site;

import com.murimblock.Murimblock;
import com.murimblock.mob.MountainBanditEntity;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The count of a bandit camp's garrison, its thirty bandits and five Bandit Chiefs (Enzo, 16/10/2026): inside the camp,
 * a bar at the top of the screen, like a boss's life, that fills each time one of them is killed; when all are dead, the camp is cleared and its boss
 * comes: the Bandit Warlord falls from the sky (WarlordEntrance, docs/mobs/bandit-warlord.md).
 * <p>Each of them is marked with his camp (its centre) when it places him, and counted once when he dies, wherever
 * he dies. A camp placed before the count (its bandits unmarked) is looked at the first time a player comes in, once its
 * residents are there: those still at home are marked, the missing ones counted as dead. Saved per world.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class BanditCampCount extends SavedData {
    static final String NAME = "murimblock_bandit_camps";
    /** A bandit's mark: the centre of his camp (BlockPos.asLong, y 0). */
    static final String TAG = "murimblock:bandit_camp";
    /** How often each player's place is looked at (ticks). */
    static final int EVERY = 10;
    /** How far from the camp's centre the count is shown: the palisade and its ditch. */
    static final int SHOWN = BanditCamp.R + 8;
    public static final SavedData.Factory<BanditCampCount> FACTORY = new SavedData.Factory<>(BanditCampCount::new, BanditCampCount::load);

    /** The bandits killed in each camp. */
    private final Map<Long, Integer> kills = new HashMap<>();
    /** The camps looked at (their bandits marked). */
    private final Set<Long> surveyed = new HashSet<>();
    /** The camps whose warlord has fallen (he comes once). */
    private final Set<Long> warlords = new HashSet<>();
    /** The bars on show, per level and camp; not saved. */
    private static final Map<ResourceKey<Level>, Map<Long, ServerBossEvent>> BARS = new HashMap<>();

    public static BanditCampCount get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    /** The camp whose centre is this column. */
    public static long camp(int x, int z) {
        return BlockPos.asLong(x, 0, z);
    }

    /** Marks a bandit as one of the garrison of the camp whose centre is (x, z). */
    public static void enlist(Mob bandit, int x, int z) {
        bandit.getPersistentData().putLong(TAG, camp(x, z));
    }

    /** The camp a bandit belongs to, if he is one of a camp's garrison. */
    public static OptionalLong campOf(Mob mob) {
        CompoundTag tag = mob.getPersistentData();
        return tag.contains(TAG, Tag.TAG_LONG) ? OptionalLong.of(tag.getLong(TAG)) : OptionalLong.empty();
    }

    /** How many of the camp's garrison are dead. */
    public static int killed(ServerLevel level, long camp) {
        return get(level).kills.getOrDefault(camp, 0);
    }

    public static boolean cleared(ServerLevel level, long camp) {
        return killed(level, camp) >= BanditCamp.GARRISON;
    }

    // ---------------------------------------------------------------- deaths

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        OptionalLong camp = campOf(mob);
        if (camp.isEmpty()) return;
        mob.getPersistentData().remove(TAG); // counted once
        count(level, camp.getAsLong(), 1);
    }

    /** Counts {@code n} more of the camp's bandits dead. */
    static void count(ServerLevel level, long camp, int n) {
        BanditCampCount data = get(level);
        int before = data.kills.getOrDefault(camp, 0), now = Math.min(BanditCamp.GARRISON, before + n);
        if (now == before) return;
        data.kills.put(camp, now);
        data.setDirty();
        ServerBossEvent bar = bars(level).get(camp);
        if (bar != null) show(bar, now);
        if (now >= BanditCamp.GARRISON) fallSilent(level, camp);
    }

    /** The garrison is dead: the bar goes, the players in the camp hear it, and the Bandit Warlord falls (WarlordEntrance). */
    private static void fallSilent(ServerLevel level, long camp) {
        BlockPos centre = BlockPos.of(camp);
        ServerBossEvent bar = bars(level).remove(camp);
        if (bar != null) bar.removeAllPlayers();
        for (ServerPlayer player : level.players()) if (inside(player, centre)) {
            player.displayClientMessage(Component.translatable("message.murimblock.bandit_camp.cleared"), true);
            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 1, 0.8f);
        }
        if (!get(level).warlords.contains(camp)) WarlordEntrance.begin(level, camp);
    }

    // ---------------------------------------------------------------- the bar

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % EVERY != 0) return;
        ServerLevel level = player.serverLevel();
        Long here = null;
        for (StructureStart start : level.structureManager().startsForStructure(player.chunkPosition(),
                s -> s instanceof SiteStructure site && site.plan() == SitePlan.BANDIT_CAMP)) {
            BlockPos centre = centre(start);
            if (!start.isValid() || centre == null || !inside(player, centre)) continue;
            long camp = camp(centre.getX(), centre.getZ());
            survey(level, start, camp);
            // A camp cleared whose warlord never came (the server stopped before he fell): he comes now.
            if (cleared(level, camp) && !get(level).warlords.contains(camp)) WarlordEntrance.begin(level, camp);
            if (get(level).surveyed.contains(camp) && !cleared(level, camp)) here = camp;
            break;
        }
        Map<Long, ServerBossEvent> bars = bars(level);
        for (Map.Entry<Long, ServerBossEvent> e : bars.entrySet()) if (!e.getKey().equals(here)) e.getValue().removePlayer(player);
        if (here != null) {
            long camp = here;
            bars.computeIfAbsent(camp, k -> show(new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10),
                    killed(level, camp))).addPlayer(player);
        }
    }

    private static ServerBossEvent show(ServerBossEvent bar, int killed) {
        bar.setName(Component.translatable("bossbar.murimblock.bandit_camp", killed, BanditCamp.GARRISON));
        bar.setProgress(killed / (float) BanditCamp.GARRISON);
        return bar;
    }

    /** Whether a player is in the camp whose centre is here: inside its ditch, not far above or below it. */
    static boolean inside(ServerPlayer player, BlockPos centre) {
        double dx = player.getX() - centre.getX() - 0.5, dz = player.getZ() - centre.getZ() - 0.5;
        return dx * dx + dz * dz <= SHOWN * SHOWN && Math.abs(player.getY() - centre.getY()) < 48;
    }

    /** The centre of a camp: its pieces' origin. */
    private static BlockPos centre(StructureStart start) {
        for (StructurePiece piece : start.getPieces()) if (piece instanceof SitePiece site) return site.origin();
        return null;
    }

    /**
     * The first look at a camp, once its residents are there: a camp placed before the count has none of its bandits
     * marked; those still at home are marked now, the missing ones counted dead.
     */
    private static void survey(ServerLevel level, StructureStart start, long camp) {
        BanditCampCount data = get(level);
        if (data.surveyed.contains(camp) || !SiteResidents.settled(level, start) || !SiteResidents.entitiesLoaded(level, start.getBoundingBox())) return;
        List<Mob> home = level.getEntitiesOfClass(Mob.class, AABB.of(start.getBoundingBox()).inflate(4, 16, 4),
                m -> (m instanceof MountainBanditEntity || m instanceof com.murimblock.mob.BanditChiefEntity) && m.isPersistenceRequired() && m.isAlive());
        boolean marked = data.kills.containsKey(camp) || home.stream().anyMatch(m -> campOf(m).isPresent());
        data.surveyed.add(camp);
        data.setDirty();
        if (marked) return;
        BlockPos centre = BlockPos.of(camp);
        int n = Math.min(home.size(), BanditCamp.GARRISON);
        for (int i = 0; i < n; i++) enlist(home.get(i), centre.getX(), centre.getZ());
        count(level, camp, BanditCamp.GARRISON - n);
    }

    /** Whether this camp's warlord has fallen. */
    public static boolean warlordCame(ServerLevel level, long camp) {
        return get(level).warlords.contains(camp);
    }

    static void markWarlord(ServerLevel level, long camp) {
        BanditCampCount data = get(level);
        data.warlords.add(camp);
        data.setDirty();
    }

    private static Map<Long, ServerBossEvent> bars(ServerLevel level) {
        return BARS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
    }

    /** Takes a player off every bar (leaving the world or the level). */
    private static void forget(ServerPlayer player) {
        for (Map<Long, ServerBossEvent> bars : BARS.values()) for (ServerBossEvent bar : bars.values()) bar.removePlayer(player);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) forget(player);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) forget(player);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BARS.clear();
    }

    /** Tests: the bar shown in this camp, if any. */
    public static ServerBossEvent barForTests(ServerLevel level, long camp) {
        return bars(level).get(camp);
    }

    /** Tests: forgets a camp's count, as in a world that never saw it. */
    public static void forgetForTests(ServerLevel level, long camp) {
        BanditCampCount data = get(level);
        data.kills.remove(camp);
        data.surveyed.remove(camp);
        data.warlords.remove(camp);
        data.setDirty();
        ServerBossEvent bar = bars(level).remove(camp);
        if (bar != null) bar.removeAllPlayers();
    }

    // ---------------------------------------------------------------- saving

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag camps = new ListTag();
        Set<Long> all = new HashSet<>(kills.keySet());
        all.addAll(surveyed);
        all.addAll(warlords);
        for (long camp : all) {
            CompoundTag c = new CompoundTag();
            c.putLong("Camp", camp);
            c.putInt("Killed", kills.getOrDefault(camp, 0));
            c.putBoolean("Surveyed", surveyed.contains(camp));
            c.putBoolean("Warlord", warlords.contains(camp));
            camps.add(c);
        }
        tag.put("camps", camps);
        return tag;
    }

    private static BanditCampCount load(CompoundTag tag, HolderLookup.Provider registries) {
        BanditCampCount data = new BanditCampCount();
        for (Tag t : tag.getList("camps", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            long camp = c.getLong("Camp");
            if (c.getInt("Killed") > 0) data.kills.put(camp, c.getInt("Killed"));
            if (c.getBoolean("Surveyed")) data.surveyed.add(camp);
            if (c.getBoolean("Warlord")) data.warlords.add(camp);
        }
        return data;
    }
}
