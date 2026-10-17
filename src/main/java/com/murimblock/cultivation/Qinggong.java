package com.murimblock.cultivation;

import com.murimblock.qi.QiService;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Qinggong (轻功), the lightness skill, the first thing a realm gives that one feels in play (Enzo, 17/10/2026): from Qi
 * Guiding on, a cultivator jumps a little higher and falls further unhurt with each realm (RealmBonuses), and can leap
 * again once in the air, off nothing but their Qi (a double jump, {@link #LEAP_COST} Qi): a gust of air beneath the
 * feet. The client leaps at once (QinggongClient); the server checks the realm and the Qi, takes the Qi, forgives the
 * fall so far and shows the gust to all.
 */
public final class Qinggong {
    /** The realm the lightness skill comes with. */
    public static final CultivationRealm REALM = CultivationRealm.QI_GUIDING;
    /** What the leap in the air costs. */
    public static final double LEAP_COST = 4;
    /** How high the leap throws (blocks a tick upward) and how far it carries on (the move kept, and pushed along). */
    public static final double LEAP_UP = 0.62, LEAP_ALONG = 0.22;
    /** Each realm's jump (added to the jump strength, 0.42 for anyone) and the fall it takes unhurt (blocks added). */
    static final double[] JUMP = {0, 0.06, 0.08, 0.10, 0.12, 0.14, 0.16, 0.18, 0.20};
    static final double[] SAFE_FALL = {0, 3, 4, 5, 6, 8, 10, 12, 15};

    private Qinggong() {
    }

    public static double jump(CultivationRealm realm) {
        return JUMP[realm.ordinal()];
    }

    public static double safeFall(CultivationRealm realm) {
        return SAFE_FALL[realm.ordinal()];
    }

    /** Whether a player may leap in the air now: their realm, enough Qi, not seated, flying, swimming or gliding. */
    public static boolean canLeap(Player player) {
        return CultivationService.getRealm(player).ordinal() >= REALM.ordinal() && QiService.getQi(player) >= LEAP_COST
                && !MeditationService.isMeditating(player) && !player.getAbilities().flying && !player.isFallFlying()
                && !player.isInWater() && !player.isPassenger() && !player.onClimbable();
    }

    /** The server's side of a leap the client made: checked, paid for, the fall forgiven, the gust shown. */
    public static boolean leap(ServerPlayer player) {
        if (player.onGround() || !canLeap(player)) return false;
        QiService.removeQi(player, LEAP_COST);
        player.resetFallDistance();
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            level.sendParticles(ParticleTypes.CLOUD, player.getX() + Math.cos(a) * 0.5, player.getY() + 0.05, player.getZ() + Math.sin(a) * 0.5,
                    0, Math.cos(a), -0.15, Math.sin(a), 0.12);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.6F, 1.6F);
        return true;
    }
}
