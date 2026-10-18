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
    /**
     * What a blood-hardened hand adds to the Sanguine Art's techniques: a high art, learnt at Qi Crystallization, whose
     * bare hands (1 + the realm's 4, +20 %: 6) then strike like a netherite blade (6 + 4 = 10, then the Qi's x1.6 at 3 000).
     */
    public static final double BLOOD_TRAINING = 4;
    /** What a hand hardened on stone adds to the Mountain-Shattering Fist's techniques (an art of Qi Condensation). */
    public static final double MOUNTAIN_TRAINING = 4;
    /** What a hand burning with the sun adds to the Majin Fist's techniques (a high art, of Qi Liquidation). */
    public static final double MAJIN_TRAINING = 5;
    /** What the kagune adds to the Devouring Demon Art's techniques (a high art, of Foundation Establishment). */
    public static final double DEMON_TRAINING = 5;
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
        return fistFactor(attack, bareHand, FIST_TRAINING);
    }
    /** As above, with the given training. */
    public static double fistFactor(double attack, boolean bareHand, double training) {
        return bareHand && attack > 0 ? (attack + training) / attack : 1;
    }

    /** All a technique of this art multiplies the player's attack by. */
    public static double factor(ServerPlayer player, net.minecraft.resources.ResourceLocation art) {
        boolean bare = player.getMainHandItem().isEmpty();
        double training = art.equals(MartialArts.LONG_FIST) ? FIST_TRAINING : art.equals(MartialArts.SANGUINE_ART) ? BLOOD_TRAINING
                : art.equals(MartialArts.MOUNTAIN_SHATTERING_FIST) ? MOUNTAIN_TRAINING : art.equals(MartialArts.MAJIN_FIST) ? MAJIN_TRAINING : art.equals(MartialArts.DEVOURING_DEMON) ? DEMON_TRAINING : 0;
        return qiFactor(QiService.getQiMax(player)) * fistFactor(player.getAttributeValue(Attributes.ATTACK_DAMAGE), bare && training > 0, training);
    }

    /** A share of the player's attack as a technique of this art deals it (for what is not an animation's hit). */
    public static float damage(ServerPlayer player, net.minecraft.resources.ResourceLocation art, double share) {
        return (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * factor(player, art) * share);
    }
}
