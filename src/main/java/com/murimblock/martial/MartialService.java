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

    /** Casts the technique in a combat bar slot, after every check. */
    public static CastResult castSlot(ServerPlayer player, int slot) {
        ResourceLocation id = data(player).slot(slot).orElse(null);
        MartialArts.Technique technique = id == null ? null : MartialArts.technique(id);
        if (technique == null || !data(player).hasTechnique(id)) return CastResult.NO_TECHNIQUE;
        if (!CombatService.isInCombatMode(player)) return CastResult.NOT_IN_COMBAT;
        if (KnockdownService.isHindered(player) || KnockdownService.isCarried(player) || QiChargeService.isCharging(player)
                || EpicFightBridge.isBusy(player)) return CastResult.HINDERED;
        if (cooldownLeft(player, id) > 0) return CastResult.COOLDOWN;
        if (QiService.getQi(player) < technique.qiCost()) return CastResult.NO_QI;
        if (!Techniques.canUse(player, technique)) return CastResult.NO_WEAPON;
        if (!Techniques.perform(player, technique)) return CastResult.FAILED;
        QiService.removeQi(player, technique.qiCost());
        READY_AT.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(id, player.level().getGameTime() + technique.cooldownTicks());
        return CastResult.CAST;
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
    }

    public static void clearCooldownsForTests(ServerPlayer player) {
        READY_AT.remove(player.getUUID());
    }
}
