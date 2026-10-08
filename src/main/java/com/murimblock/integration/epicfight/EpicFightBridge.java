package com.murimblock.integration.epicfight;

import com.murimblock.combat.CombatService;
import com.murimblock.qi.charge.QiChargeService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

/** The only boundary between Murimblock gameplay and the pinned combat engine. */
public final class EpicFightBridge {
    private EpicFightBridge() { }

    public static PlayerPatch<?> patch(Player player) {
        return EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
    }

    public static boolean isCombatMode(Player player) {
        PlayerPatch<?> patch = patch(player);
        return patch != null && patch.isEpicFightMode();
    }

    public static boolean isBusy(Player player) {
        PlayerPatch<?> patch = patch(player);
        return patch != null && (patch.getEntityState().inaction() || patch.isHoldingAny());
    }

    public static boolean setMode(ServerPlayer player, boolean enabled) {
        PlayerPatch<?> patch = patch(player);
        if (patch == null || !EpicFightGameRules.CAN_SWITCH_PLAYER_MODE.getRuleValue(player.level())) return false;
        boolean before = patch.isEpicFightMode();
        patch.toMode(enabled ? PlayerPatch.PlayerMode.EPICFIGHT : PlayerPatch.PlayerMode.VANILLA, true);
        return before != patch.isEpicFightMode();
    }

    public static void registerHooks() {
        EpicFightEventHooks.Player.CAST_SKILL.registerEvent(event -> {
            if (event.getPlayerPatch().getOriginal() instanceof ServerPlayer player
                    && QiChargeService.isCharging(player)) {
                event.cancel();
            }
        }, "murimblock:qi_charge", 100);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) CombatService.refreshMode(player);
    }
}
