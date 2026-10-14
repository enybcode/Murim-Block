package com.murimblock.integration.epicfight;

import com.murimblock.combat.CombatService;
import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.cultivation.MeditationService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.utils.math.ValueModifier;
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
            if (!EpicFightCombatDefaults.allowed(event.getSkillContainer().getSkill())) event.cancel();
            if (event.getPlayerPatch().getOriginal() instanceof ServerPlayer player
                    && MeditationService.isMeditating(player)) {
                event.cancel();
            }
            // Knocked down, carrying someone or performing a finish: no attack, guard or roll.
            if (KnockdownService.isHindered(event.getPlayerPatch().getOriginal())) event.cancel();
        }, "murimblock:qi_charge", 100);
        // A knocked-down player stays lying when hit (the finish): no stun animation lifting them to their knees. A hit
        // taken on a normal parry does not make the player flinch either.
        EpicFightEventHooks.Entity.ON_STUNNED.registerEvent(event -> {
            if (!(event.getEntityPatch().getOriginal() instanceof Player player)) return;
            if (KnockdownService.isDowned(player) || (player instanceof ServerPlayer sp && MurimGuardSkill.guardedThisTick(sp))) event.cancel();
        }, "murimblock:knockdown_no_stun", 100);
        // A normal parry lets half the damage through.
        EpicFightEventHooks.Entity.TAKE_DAMAGE_PRE.registerEvent(event -> {
            if (event.getEntityPatch().getOriginal() instanceof ServerPlayer player) {
                float share = MurimGuardSkill.damageShare(player);
                if (share < 1.0F) event.attachValueModifier(ValueModifier.multiplier(share));
            }
        }, "murimblock:normal_parry", 100);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EpicFightCombatDefaults.enforce(player);
            CombatService.refreshMode(player);
        }
    }
}
