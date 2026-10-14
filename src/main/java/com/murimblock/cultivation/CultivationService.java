package com.murimblock.cultivation;

import com.murimblock.qi.QiService;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Internal Cultivation implementation service used by Murimblock systems.
 *
 * <p>Addons should prefer {@code com.murimblock.api.MurimblockApi#cultivation()}.</p>
 */
public final class CultivationService {
    private CultivationService() {
    }

    public static CultivationData getCultivation(Player player) {
        return player.getData(CultivationAttachments.PLAYER_CULTIVATION);
    }

    public static CultivationRealm getRealm(Player player) {
        return getCultivation(player).realm();
    }

    public static CultivationStage getStage(Player player) {
        return getCultivation(player).stage();
    }

    public static Optional<CultivationData> getNextCultivation(Player player) {
        return CultivationProgression.getNext(getCultivation(player));
    }

    public static Optional<Double> getRequiredQiMaxForNextStage(Player player) {
        return CultivationProgression.getRequiredQiMaxForNext(getCultivation(player));
    }

    public static boolean canAttemptBreakthrough(Player player) {
        return CultivationProgression.canAttemptBreakthrough(getCultivation(player), QiService.getQiMax(player));
    }

    public static Optional<BreakthroughType> getBreakthroughType(Player player) {
        return CultivationProgression.getBreakthroughType(getCultivation(player));
    }

    public static boolean advanceAfterSuccessfulBreakthrough(ServerPlayer player) {
        return advance(player, false);
    }

    public static boolean forceAdvance(ServerPlayer player) {
        return advance(player, true);
    }

    public static void setCultivation(ServerPlayer player, CultivationRealm realm, CultivationStage stage) {
        player.setData(CultivationAttachments.PLAYER_CULTIVATION, new CultivationData(realm, stage));
        RealmBonuses.apply(player);
    }

    public static void resetCultivation(ServerPlayer player) {
        player.setData(CultivationAttachments.PLAYER_CULTIVATION, CultivationProgression.reset());
        RealmBonuses.apply(player);
    }

    // ---------------------------------------------------------------- the journal (toxicity, the day's condensation, failures)

    public static CultivationJournal journal(Player player) {
        return player.getData(CultivationAttachments.PLAYER_JOURNAL);
    }

    public static void setJournal(ServerPlayer player, CultivationJournal journal) {
        player.setData(CultivationAttachments.PLAYER_JOURNAL, journal);
    }

    // ---------------------------------------------------------------- Qi Max grows

    /** Where a Qi Max gain came from (the bottleneck and small breakthroughs are the same for all). */
    public enum Source { MEDITATION, TECHNIQUE, KILL, BOSS, PILL, MISSION, COMMAND }

    /**
     * Adds Qi Max to a player: every stage threshold crossed is a small breakthrough (shown, told); at the threshold of
     * a new realm it stops (the bottleneck: the great breakthrough is fought in the inner world). Returns what was added.
     */
    public static double gainQiMax(ServerPlayer player, double amount, Source source) {
        if (!(amount > 0)) return 0;
        CultivationData before = getCultivation(player);
        double qiMax = QiService.getQiMax(player);
        boolean wasBottleneck = CultivationRules.atBottleneck(before, qiMax);
        CultivationRules.Growth growth = CultivationRules.grow(before, qiMax, amount);
        if (growth.gained() <= 0 && growth.minorBreakthroughs() == 0) return 0;
        QiService.setQiMax(player, growth.qiMax());
        if (!growth.data().equals(before)) {
            player.setData(CultivationAttachments.PLAYER_CULTIVATION, growth.data());
            RealmBonuses.apply(player);
            CultivationEffects.minorBreakthrough(player, growth.data());
        }
        if (!wasBottleneck && CultivationRules.atBottleneck(growth.data(), growth.qiMax())) CultivationEffects.bottleneck(player);
        return growth.gained();
    }

    // ---------------------------------------------------------------- where one meditates

    /** How rich in Qi the place where the player is: the sanctuary, the peaks, a Murim land, the inner world. */
    public static CultivationRules.Place placeOf(ServerPlayer player) {
        return placeAt(player.serverLevel(), player.blockPosition(), player.getY());
    }

    /** How rich in Qi this spot is (see placeOf). */
    public static CultivationRules.Place placeAt(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, double y) {
        if (com.murimblock.cultivation.inner.InnerWorldService.isInnerWorld(level)) return CultivationRules.Place.INNER_WORLD;
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD
                && com.murimblock.world.structure.SanctuaryPlacement.near(level, pos, SANCTUARY_REACH)) {
            return CultivationRules.Place.POWER;
        }
        if (y > CultivationRules.HIGH_Y) return CultivationRules.Place.HIGH;
        if (level.getBiome(pos).is(com.murimblock.world.MurimBiomes.MURIM_TAG)) return CultivationRules.Place.MURIM_LAND;
        return CultivationRules.Place.ORDINARY;
    }

    /** How near the Cloud Cliff Sanctuary counts as being in it (blocks, horizontally). */
    public static final double SANCTUARY_REACH = 40;

    // ---------------------------------------------------------------- the great breakthrough

    /** The Heart Demon's strength for this player's great breakthrough now (what they prepared). */
    public static double demonStrength(ServerPlayer player, CultivationRules.Place bodyPlace) {
        CultivationJournal j = journal(player);
        long now = player.level().getGameTime();
        return CultivationRules.demonStrength(new CultivationRules.Preparation(j.breakingPillActive(now), bodyPlace, j.failures(), j.toxicityAt(now)));
    }

    /** The Heart Demon defeated: the next realm, its bonuses, the reserve full, the failures forgotten. */
    public static boolean greatBreakthrough(ServerPlayer player) {
        CultivationData data = getCultivation(player);
        if (!CultivationRules.atBottleneck(data, QiService.getQiMax(player))) return false;
        CultivationData next = CultivationProgression.getNext(data).orElse(null);
        if (next == null) return false;
        player.setData(CultivationAttachments.PLAYER_CULTIVATION, next);
        setJournal(player, journal(player).withFailures(0).withBreakingPill(0));
        RealmBonuses.apply(player);
        QiService.refillQi(player);
        CultivationEffects.greatBreakthrough(player, next);
        return true;
    }

    /** The Heart Demon won: a deviation of the Qi (Qi Max lost down to the stage, the reserve emptied, the effect). */
    public static void deviation(ServerPlayer player) {
        CultivationData data = getCultivation(player);
        double qiMax = QiService.getQiMax(player);
        QiService.setQiMax(player, CultivationRules.afterLoss(data, qiMax, CultivationRules.DEVIATION_LOSS));
        QiService.setQi(player, 0);
        CultivationJournal j = journal(player);
        setJournal(player, j.withFailures(j.failures() + 1));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(CultivationRegistries.QI_DEVIATION, CultivationRules.DEVIATION_TICKS));
        CultivationEffects.deviation(player);
    }

    /** A death: a share of Qi Max lost for good, never below the stage reached. */
    public static double deathPenalty(ServerPlayer player) {
        double before = QiService.getQiMax(player);
        QiService.setQiMax(player, CultivationRules.afterLoss(getCultivation(player), before, CultivationRules.DEATH_LOSS));
        return before - QiService.getQiMax(player);
    }

    private static boolean advance(ServerPlayer player, boolean ignoreQiMax) {
        CultivationData current = getCultivation(player);
        Optional<CultivationData> next = CultivationProgression.getNext(current);
        if (next.isEmpty()) {
            return false;
        }
        if (!ignoreQiMax && !CultivationProgression.canAttemptBreakthrough(current, QiService.getQiMax(player))) {
            return false;
        }
        player.setData(CultivationAttachments.PLAYER_CULTIVATION, next.get());
        RealmBonuses.apply(player);
        return true;
    }
}
