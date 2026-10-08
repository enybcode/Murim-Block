package com.murimblock.integration.epicfight;

import com.murimblock.qi.charge.QiChargeService;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPAddLearnedSkill;
import yesman.epicfight.network.server.SPSetRemotePlayerSkill;
import yesman.epicfight.network.server.SPSetSkillContainerValue;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.gamerule.EpicFightGameRules;
import yesman.epicfight.world.item.SkillBookItem;

/** Accepts identifiers only; ownership, books, slots and cooldowns are checked on the server. */
public final class EpicFightSkillService {
    private EpicFightSkillService() { }

    public static boolean validIndices(int slot, int slotCount, int bookSlot) {
        return slot >= 0 && slot < slotCount && (bookSlot == -1 || bookSlot == 40 || bookSlot >= 0 && bookSlot < 9);
    }

    public static boolean change(ServerPlayer player, int slotIndex, String skillId, int bookSlot) {
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (patch == null || !player.isAlive() || player.isSpectator()
                || EpicFightBridge.isBusy(player) || QiChargeService.isCharging(player)
                || !validIndices(slotIndex, patch.getPlayerSkills().skillContainers.length, bookSlot)) {
            return reject(player);
        }
        SkillContainer container = patch.getPlayerSkills().getSkillContainerFor(slotIndex);
        SkillSlot slot = container.getSlot();
        ResourceLocation id = ResourceLocation.tryParse(skillId);
        Skill skill = skillId.isEmpty() ? null : id == null ? null : EpicFightRegistries.SKILL.get(id);
        if (!slot.category().learnable() || container.onReplaceCooldown()
                || !skillId.isEmpty() && skill == null
                || skill != null && (skill.getCategory() != slot.category()
                    || patch.getPlayerSkills().isEquipping(skill))) return reject(player);

        if (slot.category() == SkillCategories.PASSIVE && slot instanceof SkillSlots) {
            int limit = EpicFightGameRules.MAX_PASSIVE_SKILLS.getRuleValue(player.level());
            int index = slot.universalOrdinal() - SkillSlots.PASSIVE1.universalOrdinal();
            if (skill != null && index >= limit) return reject(player);
        }

        ItemStack book = ItemStack.EMPTY;
        boolean learning = bookSlot != -1;
        if (learning) {
            if (skill == null || bookSlot != 40 && bookSlot != player.getInventory().selected) return reject(player);
            book = player.getInventory().getItem(bookSlot);
            if (!(book.getItem() instanceof SkillBookItem)
                    || SkillBookItem.getContainSkill(book).map(holder -> holder.value() != skill).orElse(true)
                    || skill.getPriorSkill() != null && !patch.getPlayerSkills().isEquipping(skill.getPriorSkill())) {
                return reject(player);
            }
        } else if (skill != null && !player.isCreative() && !patch.getPlayerSkills().hasLearned(skill)) {
            return reject(player);
        }
        if (!container.setSkill(skill)) return reject(player);
        if (learning) {
            patch.getPlayerSkills().addLearnedSkill(skill);
            if (!player.isCreative()) book.shrink(1);
            EpicFightNetworkManager.sendToPlayer(new SPAddLearnedSkill(List.of(skill.holder())), player);
        }
        container.setReplaceCooldown(EpicFightGameRules.SKILL_REPLACE_COOLDOWN.getRuleValue(player.level()));
        EpicFightNetworkManager.sendToPlayer(container.createSyncPacketToLocalPlayer(), player);
        EpicFightNetworkManager.sendToPlayer(SPSetSkillContainerValue.replaceCooldown(slot, container.getReplaceCooldown(), player.getId()), player);
        EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(
                new SPSetRemotePlayerSkill(slot, player.getId(), Skill.holderOrNull(skill)), player);
        return true;
    }

    private static boolean reject(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.murimblock.techniques.rejected"), true);
        return false;
    }
}
