package com.murimblock.martial;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

/**
 * What each technique does, through Epic Fight: the technique plays its attack animation, whose own colliders deal the
 * damage (no second damage loop). Checks that do not depend on the technique are in {@link MartialService#castSlot}.
 */
final class Techniques {
    /** The sword arts need a blade of the sword family in the main hand. */
    private static final Set<WeaponCategory> SWORDS = Set.of(WeaponCategories.SWORD, WeaponCategories.LONGSWORD,
            WeaponCategories.UCHIGATANA, WeaponCategories.TACHI);

    private Techniques() {
    }

    static boolean canUse(ServerPlayer player, MartialArts.Technique technique) {
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (patch == null) return false;
        if (technique.art().equals(MartialArts.AZURE_CLOUD_SWORD)) {
            return SWORDS.contains(patch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory());
        }
        return true;
    }

    /** Starts the technique; false if it cannot be started now (it then costs nothing). */
    static boolean perform(ServerPlayer player, MartialArts.Technique technique) {
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (patch == null || patch.getEntityState().inaction()) return false;
        if (technique == MartialArts.CLOUD_STEP_SLASH) {
            patch.playAnimationSynchronized(MurimAnimations.CLOUD_STEP_SLASH, 0.0F);
            return true;
        }
        // Thousand Drizzle and Heaven-Splitting Crescent come in the next slices.
        return false;
    }
}
