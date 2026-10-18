package com.murimblock.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.cultivation.inner.InnerWorldService;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.martial.MartialService;
import com.murimblock.qi.QiService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Meditation (docs/cultivation/qi-cultivation.md), on the server. The key sits the player down in the lotus (and up
 * again); seated, they cannot move, fight or cast, the reserve fills fast (QiEvents), and once it is full the Qi
 * condenses into Qi Max, more in places rich in Qi, less once the day's share is spent. A blow, a knock-down or getting
 * carried ends it. Held while seated, the key opens the inner world ({@link InnerWorldService}). Other clients learn of
 * it through {@link AuraState} (the pose, the aura).
 */
public final class MeditationService {
    static final ResourceLocation LOCK = id("meditation_lock"), JUMP_LOCK = id("meditation_jump_lock");
    /** Who meditates, since when (game time). */
    private static final Map<UUID, Long> SEATED = new HashMap<>();
    /** How much the reserve's fill must change before other clients are told (the aura's size). */
    private static final float FILL_STEP = 0.02F;

    private MeditationService() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
    }

    public static AuraState aura(Player player) {
        return player.getData(CultivationAttachments.PLAYER_AURA);
    }

    /** Whether the player meditates (on a client: as the server last said). */
    public static boolean isMeditating(Player player) {
        if (player.level().isClientSide) return aura(player).meditating();
        return SEATED.containsKey(player.getUUID());
    }

    /** Ticks since this player sat down (0 if they do not meditate). */
    public static long seatedFor(ServerPlayer player) {
        Long since = SEATED.get(player.getUUID());
        return since == null ? 0 : player.level().getGameTime() - since;
    }

    /** Whether the player may sit down to meditate now: on the ground, free, not fighting through an animation. */
    public static boolean canStart(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && player.onGround() && !player.isPassenger() && !player.isInWater()
                && !EpicFightBridge.isBusy(player) && !KnockdownService.isHindered(player) && !KnockdownService.isCarried(player);
    }

    public static boolean start(ServerPlayer player) {
        return !isMeditating(player) && canStart(player) && seat(player);
    }

    /** Back from the inner world: the cultivator wakes up seated as they left, whatever their body was doing. */
    public static boolean resume(ServerPlayer player) {
        if (isMeditating(player) || !player.isAlive() || KnockdownService.isHindered(player)) return false;
        return seat(player);
    }

    private static boolean seat(ServerPlayer player) {
        SEATED.put(player.getUUID(), player.level().getGameTime());
        lock(player, true);
        player.setSprinting(false);
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (patch != null) patch.playAnimationSynchronized(MurimAnimations.MEDITATE_SIT, 0.0F);
        setAura(player, aura(player).withMeditating(true).withFill(fill(player)));
        return true;
    }

    /** Ends the meditation; `rise`: the player gets up (the animation), else it just stops (a blow, a teleport). */
    public static boolean stop(ServerPlayer player, boolean rise) {
        if (SEATED.remove(player.getUUID()) == null) return false;
        lock(player, false);
        if (rise && player.isAlive() && !KnockdownService.isHindered(player)) {
            PlayerPatch<?> patch = EpicFightBridge.patch(player);
            if (patch != null) patch.playAnimationSynchronized(MurimAnimations.MEDITATE_RISE, 0.0F);
        }
        setAura(player, aura(player).withMeditating(false));
        return true;
    }

    /** How long the meditation key is held, seated, to open the inner world (7 s, Enzo 18/10), and anywhere inside to leave it (ticks). */
    public static final int ENTER_HOLD_TICKS = 7 * 20, LEAVE_HOLD_TICKS = 30;

    /**
     * The key held down while seated (or anywhere in the inner world): into the inner world, or back out of it. One
     * cannot have held it seated for longer than one has sat: a client claiming so sooner is not heard.
     */
    public static void held(ServerPlayer player) {
        if (InnerWorldService.isInside(player)) InnerWorldService.leave(player, InnerWorldService.Exit.WOKE);
        else if (isMeditating(player) && seatedFor(player) >= ENTER_HOLD_TICKS - 10) InnerWorldService.enter(player);
    }

    /** Every tick of a player: the aura's state for others, then the meditation itself. */
    public static void tick(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (now % 10 == 0) refreshAura(player);
        if (!isMeditating(player)) return;
        if (!player.isAlive() || player.isSpectator() || KnockdownService.isHindered(player) || player.isPassenger()) {
            stop(player, false);
            return;
        }
        player.setSprinting(false);
        lock(player, true);
        if (now % 20 == 0) condense(player, 20);
        // The Qi of the place gathers round the body, as motes to draw in with one's gaze.
        MeditationOrbs.tick(player, seatedFor(player));
        // On the Star-Plucking Peak's summit, three minutes give the Seven Stars Sword (from Qi Liquidation).
        com.murimblock.world.structure.StarPeakPlacement.meditating(player, seatedFor(player));
    }

    /** `ticks` of meditation with a full reserve: the Qi condenses into Qi Max (nothing at the bottleneck). */
    public static void condense(ServerPlayer player, int ticks) {
        double qiMax = QiService.getQiMax(player);
        if (QiService.getQi(player) < qiMax - 1e-6) return;
        CultivationData data = CultivationService.getCultivation(player);
        if (CultivationRules.atBottleneck(data, qiMax)) return;
        long now = player.level().getGameTime();
        CultivationJournal journal = CultivationService.journal(player);
        // The method practised: more in its element's places, half while out of tune after a change.
        double amount = CultivationRules.condensed(qiMax, data.realm(), CultivationService.placeOf(player), journal.condensedOn(now), ticks) * MethodService.factor(player);
        CultivationService.setJournal(player, journal.condensed(now, ticks));
        CultivationService.gainQiMax(player, amount, CultivationService.Source.MEDITATION);
    }

    /** A blow taken while seated ends the meditation. */
    public static void onHurt(ServerPlayer player) {
        stop(player, false);
    }

    public static void forget(ServerPlayer player) {
        SEATED.remove(player.getUUID());
        lock(player, false);
        setAura(player, aura(player).withMeditating(false));
    }

    public static void clearForTests() {
        SEATED.clear();
    }

    // ---------------------------------------------------------------- the aura for other clients

    static float fill(ServerPlayer player) {
        double max = QiService.getQiMax(player);
        return max <= 0 ? 0 : (float) Math.min(1, QiService.getQi(player) / max);
    }

    /**
     * Tells the clients that see this player how full their reserve is, their main style, the art their aura shows and
     * how much Qi they hold, when it changed (the Qi by a twentieth at least).
     */
    public static void refreshAura(ServerPlayer player) {
        AuraState now = aura(player);
        float fill = fill(player);
        var data = MartialService.data(player);
        var art = data.mainArt();
        var auraArt = auraArt(data);
        float qi = (float) QiService.getQi(player);
        boolean meditating = SEATED.containsKey(player.getUUID());
        int place = CultivationService.placeOf(player).ordinal();
        if (now.meditating() != meditating || Math.abs(now.fill() - fill) >= FILL_STEP || (fill >= 1 && now.fill() < 1)
                || !Objects.equals(now.mainArt(), art) || now.place() != place || !Objects.equals(now.auraArt(), auraArt)
                || Math.abs(qi - now.qi()) > Math.max(1, now.qi() * 0.05)) {
            setAura(player, new AuraState(meditating, fill, art, place, auraArt, qi));
        }
    }

    /**
     * The art an aura shows (Enzo, 17/10/2026: "par rapport à la mastery"): of the arts one knows, the one mastered best
     * (MartialData.styleMastery); on a tie, the main style; none known, none.
     */
    public static java.util.Optional<net.minecraft.resources.ResourceLocation> auraArt(com.murimblock.martial.MartialData data) {
        net.minecraft.resources.ResourceLocation best = null;
        int bestMastery = -1;
        for (var id : data.arts()) {
            var art = com.murimblock.martial.MartialArts.art(id);
            if (art == null) continue;
            int m = data.styleMastery(art);
            if (m > bestMastery || m == bestMastery && data.mainArt().filter(id::equals).isPresent()) {
                best = id;
                bestMastery = m;
            }
        }
        return java.util.Optional.ofNullable(best);
    }

    private static void setAura(ServerPlayer player, AuraState state) {
        player.setData(CultivationAttachments.PLAYER_AURA, state);
    }

    // ---------------------------------------------------------------- seated still

    private static void lock(ServerPlayer player, boolean on) {
        lock(player, Attributes.MOVEMENT_SPEED, LOCK, on);
        lock(player, Attributes.JUMP_STRENGTH, JUMP_LOCK, on);
    }

    private static void lock(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, boolean on) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        if (!on) {
            instance.removeModifier(id);
        } else if (instance.getModifier(id) == null) {
            instance.addTransientModifier(new AttributeModifier(id, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
