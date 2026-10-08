package com.murimblock.integration.epicfight;

import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPSetRemotePlayerSkill;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillSlots;

/** Basic engine actions are available without books; native progression is not Murim's technique system. */
public final class EpicFightCombatDefaults {
    private EpicFightCombatDefaults() { }

    public static boolean allowed(Skill skill) {
        return skill == null || !EpicFightContentPolicy.isNative(skill.getRegistryName())
                || skill.getCategory() == SkillCategories.BASIC_ATTACK
                || skill.getCategory() == SkillCategories.KNOCKDOWN_WAKEUP
                || skill == EpicFightSkills.GUARD.get() || skill == EpicFightSkills.ROLL.get();
    }

    public static void enforce(ServerPlayer player) {
        var patch = EpicFightBridge.patch(player);
        if (patch == null) return;
        patch.getPlayerSkills().listSkillContainers().forEach(container -> {
            Skill current = container.getSkill();
            Skill desired = current;
            boolean nativeOrEmpty = current == null || EpicFightContentPolicy.isNative(current.getRegistryName());
            if (nativeOrEmpty && container.getSlot() == SkillSlots.GUARD) desired = EpicFightSkills.GUARD.get();
            else if (nativeOrEmpty && container.getSlot() == SkillSlots.DODGE) desired = EpicFightSkills.ROLL.get();
            else if (!allowed(current)) desired = null;
            if (desired == current || !container.setSkill(desired)) return;
            EpicFightNetworkManager.sendToPlayer(container.createSyncPacketToLocalPlayer(), player);
            EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(
                    new SPSetRemotePlayerSkill(container.getSlot(), player.getId(), Skill.holderOrNull(desired)), player);
        });
    }
}
