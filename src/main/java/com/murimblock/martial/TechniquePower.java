package com.murimblock.martial;

import com.murimblock.qi.QiService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * How hard a technique strikes (docs/combat/technique-balance.md): the weapon, then the Qi.
 * <ul>
 *   <li><b>The weapon</b>: a technique deals a share of the player's attack, so of the sword in hand (a wooden one 4, an
 *   iron one 6, a netherite one 8, the realm's bonus on top). Bare hands are 1 and the realm's fist bonus: the Long Fist's
 *   techniques add the trained hand's {@link #FIST_TRAINING}, so a beginner's fists strike like a stone sword;</li>
 *   <li><b>the Qi</b>: the deeper the reserve, the harder: +{@link #QI_SCALING} for each doubling of Qi Max above 100
 *   (250: +16 %, 1 250: +44 %, 10 000: +80 %, 45 000: +106 %).</li>
 * </ul>
 * Only the techniques (and what they send: Qi blades, delayed cuts, a landing); the M1 strikes stay the weapon's own.
 * No combo can kill at once whatever the numbers: see {@link com.murimblock.combat.BurstGuard}.
 */
public final class TechniquePower {
    /** What a trained bare hand adds to the Long Fist's techniques. */
    public static final double FIST_TRAINING = 3;
    /** The technique's damage grows by this share for each doubling of Qi Max above 100. */
    public static final double QI_SCALING = 0.12;

    private TechniquePower() {
    }

    /** The Qi's share: 1 at 100 Qi Max (or less), +12 % a doubling. */
    public static double qiFactor(double qiMax) {
        return 1 + QI_SCALING * Math.max(0, Math.log(qiMax / 100.0) / Math.log(2));
    }

    /** The trained hand's share on a bare-hand attack of `attack` (1 for a weapon). */
    public static double fistFactor(double attack, boolean bareHand) {
        return bareHand && attack > 0 ? (attack + FIST_TRAINING) / attack : 1;
    }

    /** All a technique of this art multiplies the player's attack by. */
    public static double factor(ServerPlayer player, net.minecraft.resources.ResourceLocation art) {
        boolean fist = art.equals(MartialArts.LONG_FIST) && player.getMainHandItem().isEmpty();
        return qiFactor(QiService.getQiMax(player)) * fistFactor(player.getAttributeValue(Attributes.ATTACK_DAMAGE), fist);
    }

    /** A share of the player's attack as a technique of this art deals it (for what is not an animation's hit). */
    public static float damage(ServerPlayer player, net.minecraft.resources.ResourceLocation art, double share) {
        return (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * factor(player, art) * share);
    }
}
