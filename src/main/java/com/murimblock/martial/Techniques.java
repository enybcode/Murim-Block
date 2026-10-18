package com.murimblock.martial;

import com.murimblock.integration.epicfight.EpicFightBridge;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * What each technique does, through Epic Fight: the technique plays its attack animation, whose own colliders deal the
 * damage (no second damage loop). Checks that do not depend on the technique are in {@link MartialService#castSlot};
 * each art's techniques and their variants are in its own class ({@link AzureCloudSword}, {@link LongFist},
 * {@link TravellerStaff}...).
 */
final class Techniques {
    private Techniques() {
    }

    static boolean canUse(ServerPlayer player, MartialArts.Technique technique) {
        if (EpicFightBridge.patch(player) == null) return false;
        if (technique.art().equals(MartialArts.AZURE_CLOUD_SWORD)) return AzureCloudSword.holdsSword(player);
        if (technique.art().equals(MartialArts.LONG_FIST)) return LongFist.bareHanded(player);
        if (technique.art().equals(MartialArts.SANGUINE_ART)) return SanguineArt.bareHanded(player);
        if (technique.art().equals(MartialArts.CELESTIAL_PEAK_SPEAR)) return CelestialPeakSpear.holdsSpear(player);
        if (technique.art().equals(MartialArts.TRAVELLER_STAFF)) return TravellerStaff.holdsStaff(player);
        if (technique.art().equals(MartialArts.MOUNTAIN_SHATTERING_FIST)) return MountainShatteringFist.bareHanded(player);
        if (technique.art().equals(MartialArts.SEVEN_STARS_SWORD)) return SevenStarsSword.holdsFlorett(player);
        if (technique.art().equals(MartialArts.THUNDER_SABER)) return ThunderSaber.holdsSaber(player);
        if (technique.art().equals(MartialArts.MAJIN_FIST)) return MajinFistArt.bareHanded(player);
        if (technique.art().equals(MartialArts.GOLDEN_BELL)) return GoldenBell.bareHanded(player);
        if (technique.art().equals(MartialArts.OVERLORD_SPEAR)) return OverlordSpear.holdsSpear(player);
        return true;
    }

    /**
     * Starts the technique; false if it cannot be started now (it then costs nothing). A riposte (a perfect parry just
     * before) may cut the guard's recoil short.
     */
    static boolean perform(ServerPlayer player, MartialArts.Technique technique, @Nullable MartialCombat.Riposte riposte) {
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (patch == null) return false;
        boolean counter = riposte != null || LongFist.countersParry(player, technique);
        if (patch.getEntityState().inaction() && !(counter && AzureCloudSword.inGuardMotion(patch)) && !AzureCloudSword.aiming(player) && !SanguineArt.charging(player)
                && !(technique == MartialArts.SEVERING_FLASH && ThunderSaberMoves.charging(player))
                && !(technique == MartialArts.TYRANTS_THRUST && OverlordSpear.charging(player))) return false;
        if (technique.art().equals(MartialArts.AZURE_CLOUD_SWORD)) return AzureCloudSword.perform(player, patch, technique, riposte);
        if (technique.art().equals(MartialArts.LONG_FIST)) return LongFist.perform(player, patch, technique);
        if (technique.art().equals(MartialArts.SANGUINE_ART)) return SanguineArt.perform(player, patch, technique);
        if (technique.art().equals(MartialArts.CELESTIAL_PEAK_SPEAR)) return CelestialPeakSpear.perform(player, patch, technique);
        if (technique.art().equals(MartialArts.TRAVELLER_STAFF)) return TravellerStaff.perform(player, patch, technique);
        if (technique.art().equals(MartialArts.MOUNTAIN_SHATTERING_FIST)) return MountainShatteringFist.perform(player, patch, technique);
        if (technique.art().equals(MartialArts.SEVEN_STARS_SWORD)) return SevenStarsSword.perform(player, patch, technique);
        if (technique.art().equals(MartialArts.THUNDER_SABER)) return ThunderSaberMoves.perform(player, patch, technique);
        if (technique.art().equals(MartialArts.MAJIN_FIST)) return MajinFistArt.perform(player, patch, technique);
        if (technique.art().equals(MartialArts.GOLDEN_BELL)) return GoldenBell.perform(player, patch, technique);
        if (technique.art().equals(MartialArts.OVERLORD_SPEAR)) return OverlordSpear.perform(player, patch, technique);
        return false;
    }
}
