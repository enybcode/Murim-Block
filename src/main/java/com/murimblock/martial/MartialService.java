package com.murimblock.martial;

import com.murimblock.Murimblock;
import com.murimblock.combat.CombatService;
import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.network.TechniquePayloads;
import com.murimblock.qi.QiService;
import com.murimblock.qi.charge.QiChargeService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
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

    public enum CastResult { CAST, NO_TECHNIQUE, NOT_IN_COMBAT, HINDERED, COOLDOWN, NO_QI, NO_WEAPON, FAILED }

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
        set(player, before.learn(art));
        player.sendSystemMessage(Component.translatable("art.murimblock.learned", Component.translatable(art.translationKey()),
                Component.translatable(art.first().translationKey())));
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
        CastResult check = check(player, technique);
        if (check != CastResult.CAST) return check;
        MartialCombat.Riposte riposte = MartialCombat.riposte(player);
        boolean secondStep = technique == MartialArts.CLOUD_STEP_SLASH && cooldownLeft(player, id) > 0 && MartialCombat.secondStepOpen(player);
        double cost = secondStep ? 0 : riposte != null ? technique.qiCost() / 2 : technique.qiCost();
        if (!Techniques.perform(player, technique, riposte)) return CastResult.FAILED;
        if (riposte != null) MartialCombat.consumeRiposte(player);
        if (cost > 0) QiService.removeQi(player, cost);
        if (secondStep) {
            MartialCombat.takeSecondStep(player);
        } else {
            READY_AT.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(id, player.level().getGameTime() + technique.cooldownTicks());
            if (technique == MartialArts.CLOUD_STEP_SLASH && data(player).evolved(AzureCloudSword.art(), id)) MartialCombat.openSecondStep(player);
        }
        return CastResult.CAST;
    }

    /**
     * Whether a technique could be cast now (CAST if so, else why not): known, in combat mode, free to act (a riposte may
     * cut a guard's recoil short; Cloud Step Slash's draw stance is there to be let go), off cooldown, enough Qi, the
     * weapon it needs.
     */
    private static CastResult check(ServerPlayer player, @Nullable MartialArts.Technique technique) {
        if (technique == null || !data(player).hasTechnique(technique.id())) return CastResult.NO_TECHNIQUE;
        if (!CombatService.isInCombatMode(player)) return CastResult.NOT_IN_COMBAT;
        MartialCombat.Riposte riposte = MartialCombat.riposte(player);
        if (KnockdownService.isHindered(player) || KnockdownService.isCarried(player) || QiChargeService.isCharging(player)) return CastResult.HINDERED;
        if (EpicFightBridge.isBusy(player) && !(riposte != null && guardRecoil(player)) && !AzureCloudSword.aiming(player)) return CastResult.HINDERED;
        ResourceLocation id = technique.id();
        boolean secondStep = technique == MartialArts.CLOUD_STEP_SLASH && cooldownLeft(player, id) > 0 && MartialCombat.secondStepOpen(player);
        if (cooldownLeft(player, id) > 0 && !secondStep) return CastResult.COOLDOWN;
        double cost = secondStep ? 0 : riposte != null ? technique.qiCost() / 2 : technique.qiCost();
        if (QiService.getQi(player) < cost) return CastResult.NO_QI;
        if (!Techniques.canUse(player, technique)) return CastResult.NO_WEAPON;
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
        if (technique != MartialArts.CLOUD_STEP_SLASH || check(player, technique) != CastResult.CAST) return false;
        return AzureCloudSword.aim(player);
    }

    /** The player is busy only with a guard's recoil (holding the guard, or the parry's motion). */
    private static boolean guardRecoil(ServerPlayer player) {
        var patch = EpicFightBridge.patch(player);
        return patch != null && (AzureCloudSword.inGuardMotion(patch) || (patch.isHoldingAny() && !patch.getEntityState().inaction()));
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
