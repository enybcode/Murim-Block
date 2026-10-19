package com.murimblock.martial;

import com.murimblock.Murimblock;
import com.murimblock.combat.CombatService;
import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.network.TechniquePayloads;
import com.murimblock.qi.QiService;
import com.murimblock.cultivation.CultivationRules;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.MeditationService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * A player's martial arts on the server: learning an art from its manual, the combat bar, and casting a technique
 * (every check here: combat mode, unlocked, free to act, cooldown, Qi). What a technique does is in {@link Techniques}.
 */
public final class MartialService {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Murimblock.MOD_ID);

    /** Kept through death (an art, once learned, is not forgotten); synced to its player only. */
    static final Supplier<AttachmentType<MartialData>> PLAYER_MARTIAL = ATTACHMENT_TYPES.register("player_martial",
            () -> AttachmentType.builder(() -> MartialData.EMPTY)
                    .serialize(MartialData.CODEC)
                    .copyOnDeath()
                    .sync((holder, recipient) -> holder == recipient, MartialData.STREAM_CODEC)
                    .build());

    /** Game time at which each player's techniques are ready again (not saved: a restart resets cooldowns). */
    private static final Map<UUID, Map<ResourceLocation, Long>> READY_AT = new HashMap<>();

    public enum CastResult { CAST, NO_TECHNIQUE, NOT_IN_COMBAT, HINDERED, COOLDOWN, NO_QI, NO_WEAPON, NO_ROOM, SWORD_OUT, NO_TARGET, FAILED }

    private MartialService() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static MartialData data(Player player) {
        return player.getData(PLAYER_MARTIAL);
    }

    static void set(ServerPlayer player, MartialData data) {
        player.setData(PLAYER_MARTIAL, data);
    }

    /** Learns an art from its manual. False if already known. */
    public static boolean learn(ServerPlayer player, MartialArts.Art art) {
        MartialData before = data(player);
        if (before.knowsArt(art.id())) return false;
        MartialData after = before.learn(art);
        set(player, after);
        List<MartialArts.Technique> taught = art.techniques().stream().filter(t -> after.hasTechnique(t.id()) && t.castable()).toList();
        if (taught.size() > 1) {
            // The manual taught more than its first technique (the Traveller's Staff Art's teaches all three).
            MutableComponent names = Component.empty();
            for (int i = 0; i < taught.size(); i++) {
                if (i > 0) names.append(", ");
                names.append(Component.translatable(taught.get(i).translationKey()));
            }
            player.sendSystemMessage(Component.translatable("art.murimblock.learned_all", Component.translatable(art.translationKey()), names));
        } else {
            player.sendSystemMessage(Component.translatable("art.murimblock.learned", Component.translatable(art.translationKey()),
                    Component.translatable(art.first().translationKey())));
        }
        return true;
    }

    public static void assign(ServerPlayer player, int slot, ResourceLocation technique) {
        set(player, data(player).assign(slot, technique));
    }

    /** Ticks until a technique is ready again (0 when ready). */
    public static int cooldownLeft(ServerPlayer player, ResourceLocation technique) {
        Long ready = READY_AT.getOrDefault(player.getUUID(), Map.of()).get(technique);
        return ready == null ? 0 : (int) Math.max(0, ready - player.level().getGameTime());
    }

    /**
     * Casts the technique in a combat bar slot, after every check. Right after a perfect parry with Flowing Cloud
     * Stance, the technique is a riposte: half its Qi, and it may cut the guard's recoil short. Thousand-Li Cloud Step
     * (Cloud Step Slash evolved) gives a second dash, free, right after the first.
     */
    public static CastResult castSlot(ServerPlayer player, int slot) {
        ResourceLocation id = data(player).slot(slot).orElse(null);
        MartialArts.Technique technique = id == null ? null : MartialArts.technique(id);
        // The crescent's key again at the top of its rise, the body still held: the slam, free, whatever the cooldown
        // or the attack under way (the time up there is for other attacks).
        if (technique == MartialArts.HEAVEN_SPLITTING_CRESCENT && MartialCombat.hovering(player) && data(player).hasTechnique(id))
            return AzureCloudSword.slam(player) ? CastResult.CAST : CastResult.FAILED;
        // Scarlet Tear's key again while its blood sword is out: the throw, free, whatever the cooldown.
        if (technique == MartialArts.SCARLET_TEAR && SanguineArt.holdsSword(player) && data(player).hasTechnique(id))
            return SanguineArt.throwSword(player) ? CastResult.CAST : CastResult.FAILED;
        // Heaven's Drum's key let go: the pommel struck (it ended by itself just before: nothing more to say).
        if (technique == MartialArts.HEAVENS_DRUM && data(player).hasTechnique(id)) {
            if (ThunderSaberMoves.drumming(player)) { ThunderSaberMoves.endDrum(player, true); return CastResult.CAST; }
            if (ThunderSaberMoves.drumJustEnded(player)) return CastResult.CAST;
        }
        // The golden form's key again while in it: it ends, free.
        if (technique == MartialArts.GOLDEN_BELL_FORM && data(player).hasTechnique(id) && GoldenBell.leaveIfIn(player)) return CastResult.CAST;
        // Mirror Lake Stance's key let go: the halberd lowered (it ended by itself just before: nothing more to say).
        if (technique == MartialArts.MIRROR_LAKE_STANCE && data(player).hasTechnique(id)) {
            if (CrescentMoonHalberd.mirroring(player)) { CrescentMoonHalberd.endMirror(player, true); return CastResult.CAST; }
            if (CrescentMoonHalberd.mirrorJustEnded(player)) return CastResult.CAST;
        }
        // The Spinning Rampart's key let go: it stops (it stopped by itself just before: nothing more to say).
        if (technique == MartialArts.SPINNING_RAMPART && data(player).hasTechnique(id)) {
            if (TravellerStaff.spinning(player)) { TravellerStaff.endRampart(player, true); return CastResult.CAST; }
            if (TravellerStaff.justEnded(player)) return CastResult.CAST;
        }
        CastResult check = check(player, technique);
        if (check != CastResult.CAST) return check;
        // Not let go but tapped and refused at its start: say why (check), never cast it here.
        if (technique == MartialArts.SPINNING_RAMPART || technique == MartialArts.HEAVENS_DRUM || technique == MartialArts.MIRROR_LAKE_STANCE) return CastResult.FAILED;
        MartialCombat.Riposte riposte = MartialCombat.riposte(player);
        boolean secondStep = technique == MartialArts.CLOUD_STEP_SLASH && cooldownLeft(player, id) > 0 && MartialCombat.secondStepOpen(player);
        boolean twinComet = technique == MartialArts.SHOOTING_STAR && cooldownLeft(player, id) > 0 && SevenStarsSword.recastOpen(player)
                || technique == MartialArts.THUNDER_DESCENDS && cooldownLeft(player, id) > 0 && ThunderSaberMoves.recastOpen(player);
        double cost = secondStep || twinComet ? 0 : riposte != null ? technique.qiCost() / 2 : technique.qiCost();
        if (!Techniques.perform(player, technique, riposte)) return CastResult.FAILED;
        if (riposte != null) MartialCombat.consumeRiposte(player);
        if (cost > 0) pay(player, cost);
        if (secondStep) {
            MartialCombat.takeSecondStep(player);
        } else if (twinComet) {
            SevenStarsSword.takeRecast(player);
            ThunderSaberMoves.takeRecast(player);
        } else {
            READY_AT.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(id, player.level().getGameTime() + technique.cooldownTicks());
            if (technique == MartialArts.CLOUD_STEP_SLASH && data(player).evolved(AzureCloudSword.art(), id)) MartialCombat.openSecondStep(player);
            if (technique == MartialArts.SHOOTING_STAR && data(player).evolved(SevenStarsSword.art(), id)) SevenStarsSword.openRecast(player);
        }
        return CastResult.CAST;
    }

    /**
     * Whether a technique could be cast now (CAST if so, else why not): known, in combat mode, free to act (a riposte or
     * Inch Force may cut a guard's recoil short; Cloud Step Slash's draw stance is there to be let go), off cooldown,
     * enough Qi, the weapon it needs.
     */
    private static CastResult check(ServerPlayer player, @Nullable MartialArts.Technique technique) {
        if (technique == null || !data(player).hasTechnique(technique.id())) return CastResult.NO_TECHNIQUE;
        if (!CombatService.isInCombatMode(player)) return CastResult.NOT_IN_COMBAT;
        // A blood sword in hand takes the hands and the mind: nothing else until it is thrown.
        if (SanguineArt.holdsSword(player)) return CastResult.SWORD_OUT;
        // The Spinning Rampart takes the hands: no other technique while it spins.
        if (TravellerStaff.spinning(player)) return CastResult.HINDERED;
        // Heaven's Drum takes the hands too.
        if (ThunderSaberMoves.drumming(player)) return CastResult.HINDERED;
        // Mirror Lake Stance takes the hands too.
        if (CrescentMoonHalberd.mirroring(player)) return CastResult.HINDERED;
        MartialCombat.Riposte riposte = MartialCombat.riposte(player);
        if (KnockdownService.isHindered(player) || KnockdownService.isCarried(player) || MeditationService.isMeditating(player)) return CastResult.HINDERED;
        boolean counter = riposte != null || LongFist.countersParry(player, technique);
        if (EpicFightBridge.isBusy(player) && !(counter && guardRecoil(player)) && !AzureCloudSword.aiming(player) && !SanguineArt.charging(player)
                && !(technique == MartialArts.SEVERING_FLASH && ThunderSaberMoves.charging(player))
                && !(technique == MartialArts.TYRANTS_THRUST && OverlordSpear.charging(player))) return CastResult.HINDERED;
        ResourceLocation id = technique.id();
        boolean secondStep = technique == MartialArts.CLOUD_STEP_SLASH && cooldownLeft(player, id) > 0 && MartialCombat.secondStepOpen(player)
                || technique == MartialArts.SHOOTING_STAR && cooldownLeft(player, id) > 0 && SevenStarsSword.recastOpen(player)
                || technique == MartialArts.THUNDER_DESCENDS && cooldownLeft(player, id) > 0 && ThunderSaberMoves.recastOpen(player);
        if (cooldownLeft(player, id) > 0 && !secondStep) return CastResult.COOLDOWN;
        double cost = secondStep ? 0 : riposte != null ? technique.qiCost() / 2 : technique.qiCost();
        if (QiService.getQi(player) < cost) return CastResult.NO_QI;
        if (!Techniques.canUse(player, technique)) return CastResult.NO_WEAPON;
        // The crescent rises 7 blocks: not under a roof (in the air it is the descent, which needs none).
        if (technique == MartialArts.HEAVEN_SPLITTING_CRESCENT && !AzureCloudSword.airborne(player) && !AzureCloudSword.roomAbove(player)) return CastResult.NO_ROOM;
        // The Seven Stars Sword's techniques that go for someone: not cast with nobody there (nothing spent).
        if (technique.art().equals(MartialArts.SEVEN_STARS_SWORD) && !SevenStarsMoves.hasTarget(player, technique)) return CastResult.NO_TARGET;
        return CastResult.CAST;
    }

    /**
     * The key of a slot went down. For Cloud Step Slash on the ground (no riposte to answer), if it could be cast, the
     * player takes its draw stance and aims with the camera; letting the key go casts it ({@link #castSlot}) where they
     * face. Nothing otherwise: the release casts whatever applies, or says why not. True if the stance was taken.
     */
    public static boolean aim(ServerPlayer player, int slot) {
        ResourceLocation id = data(player).slot(slot).orElse(null);
        MartialArts.Technique technique = id == null ? null : MartialArts.technique(id);
        if (check(player, technique) != CastResult.CAST) return false;
        if (technique == MartialArts.BLOOD_CRYSTAL) return SanguineArt.aim(player, slot);
        // Severing Flash charges while held; its Qi is paid when it is let go (castSlot).
        if (technique == MartialArts.SEVERING_FLASH) return ThunderSaberMoves.aimFlash(player);
        // Tyrant's Thrust charges while held; its Qi is paid when it is let go (castSlot).
        if (technique == MartialArts.TYRANTS_THRUST) return OverlordSpear.aimThrust(player);
        if (technique == MartialArts.HEAVENS_DRUM) {
            if (!ThunderSaberMoves.startDrum(player)) return false;
            pay(player, technique.qiCost());
            // Not again while it is held; its true cooldown starts at its end (startCooldown).
            READY_AT.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(id, Long.MAX_VALUE / 2);
            return true;
        }
        if (technique == MartialArts.MIRROR_LAKE_STANCE) {
            if (!CrescentMoonHalberd.startMirror(player)) return false;
            pay(player, technique.qiCost());
            // Not again while it is held; its true cooldown starts at its end (startCooldown).
            READY_AT.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(id, Long.MAX_VALUE / 2);
            return true;
        }
        if (technique == MartialArts.SPINNING_RAMPART) {
            if (!TravellerStaff.startRampart(player)) return false;
            pay(player, technique.qiCost());
            // Not again while it spins; its true cooldown starts at its end (startCooldown).
            READY_AT.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(id, Long.MAX_VALUE / 2);
            return true;
        }
        return technique == MartialArts.CLOUD_STEP_SLASH && AzureCloudSword.aim(player);
    }

    /** Spends a technique's Qi; the meridians widen with use: a share of the Qi spent goes to Qi Max. */
    static void pay(ServerPlayer player, double cost) {
        QiService.removeQi(player, cost);
        CultivationService.gainQiMax(player, cost * CultivationRules.TECHNIQUE_SHARE * CultivationRules.growth(CultivationService.getRealm(player)),
                CultivationService.Source.TECHNIQUE);
    }

    /** A technique's cooldown from now (the Spinning Rampart's, at its end), on the server and on the player's bar. */
    static void startCooldown(ServerPlayer player, MartialArts.Technique technique) {
        READY_AT.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(technique.id(), player.level().getGameTime() + technique.cooldownTicks());
        if (player.connection != null) PacketDistributor.sendToPlayer(player, new TechniquePayloads.Cooldown(technique.id(), technique.cooldownTicks()));
    }

    /** The player is busy only with a guard's recoil (holding the guard, or the parry's motion). */
    private static boolean guardRecoil(ServerPlayer player) {
        var patch = EpicFightBridge.patch(player);
        return patch != null && (AzureCloudSword.inGuardMotion(patch) || (patch.isHoldingAny() && !patch.getEntityState().inaction()));
    }

    /** Takes `ticks` off a technique's cooldown, on the server and on the player's bar. */
    static void reduceCooldown(ServerPlayer player, MartialArts.Technique technique, int ticks) {
        Map<ResourceLocation, Long> ready = READY_AT.get(player.getUUID());
        Long at = ready == null ? null : ready.get(technique.id());
        if (at == null) return;
        ready.put(technique.id(), at - ticks);
        if (player.connection != null) PacketDistributor.sendToPlayer(player, new TechniquePayloads.Cooldown(technique.id(), cooldownLeft(player, technique.id())));
    }

    /** A technique not to be cast again until its cooldown is started at its end (startCooldown): the Golden Bell's form. */
    static void holdCooldown(ServerPlayer player, MartialArts.Technique technique) {
        READY_AT.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(technique.id(), Long.MAX_VALUE / 2);
    }

    /** A technique ready again in at most `ticks` (a missed Iron Rebound), on the server and on the player's bar. */
    static void reduceCooldownTo(ServerPlayer player, MartialArts.Technique technique, int ticks) {
        Map<ResourceLocation, Long> ready = READY_AT.get(player.getUUID());
        Long at = ready == null ? null : ready.get(technique.id());
        long target = player.level().getGameTime() + ticks;
        if (at == null || at <= target) return;
        ready.put(technique.id(), target);
        if (player.connection != null) PacketDistributor.sendToPlayer(player, new TechniquePayloads.Cooldown(technique.id(), ticks));
    }

    /** Places a strike of the main style's passive in one of its four M1 slots (refused if it breaks the chain's rule). */
    public static void setStrike(ServerPlayer player, int slot, ResourceLocation strike) {
        set(player, data(player).withStrike(slot, strike));
    }

    /** Makes a known art the main style: its passive gives the M1 chain and the parry. */
    public static void setMainArt(ServerPlayer player, ResourceLocation art) {
        set(player, data(player).withMainArt(art));
    }

    // Commands (/martial) and tests.

    public static void unlock(ServerPlayer player, MartialArts.Technique technique) {
        set(player, data(player).unlockWithArt(technique));
    }

    public static void forgetTechnique(ServerPlayer player, ResourceLocation technique) {
        set(player, data(player).forget(technique));
    }

    public static void forgetArt(ServerPlayer player, MartialArts.Art art) {
        set(player, data(player).forgetArt(art));
    }

    /** Sets a technique's mastery, 0 to 100. */
    public static void setMastery(ServerPlayer player, ResourceLocation technique, int value) {
        set(player, data(player).withMastery(technique, value));
    }

    /** Every technique ready again, on the server and on the player's bar. */
    public static void resetCooldowns(ServerPlayer player) {
        Map<ResourceLocation, Long> ready = READY_AT.remove(player.getUUID());
        if (ready != null && player.connection != null)
            for (ResourceLocation id : ready.keySet()) PacketDistributor.sendToPlayer(player, new TechniquePayloads.Cooldown(id, 0));
    }

    public static void forget(ServerPlayer player) {
        READY_AT.remove(player.getUUID());
        MartialCombat.forget(player);
    }

    public static void clearCooldownsForTests(ServerPlayer player) {
        READY_AT.remove(player.getUUID());
        MartialCombat.forget(player);
    }

    /** For tests and commands: sets the whole martial data of a player. */
    public static void setForTests(ServerPlayer player, MartialData data) {
        set(player, data);
    }
}
