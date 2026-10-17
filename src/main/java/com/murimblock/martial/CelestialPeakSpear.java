package com.murimblock.martial;

import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/**
 * The Celestial Peak Spear in play (docs/combat/styles/celestial-peak-spear.md), the Orthodox Alliance's spear, an art of
 * the start of the game. A spear in hand (an Epic Fight spear: the Alliance's, the Peak Captain's, Epic Fight's own). Its
 * M1 are Epic Fight's spear strikes; its passive, Long Reach, adds a fifth to a hit landed at the point, 3 blocks out or
 * more, and a little push ({@link MartialCombat}). Every technique picks its variant on the server, then plays its
 * animation (scripts/animation/celestial-spear.cjs), whose boxes deal the damage:
 * <ul>
 *   <li><b>Piercing Dragon</b>: a lunge of 4 blocks (6 evolved: Soaring Dragon) through all in its line; from the air a
 *   dive, point first, that pins the target down;</li>
 *   <li><b>Plum Blossom Thrusts</b>: five quick thrusts fanned out, then one that pushes (eight evolved: Plum Rain); from
 *   the air three thrusts down and the shaft brought down;</li>
 *   <li><b>Sweep of a Thousand Soldiers</b>: a whole turn at the knees, all round, knocking down (two turns and a wave
 *   evolved: Sweep of Ten Thousand); from the air the spear planted as the body lands, a wave round it.</li>
 * </ul>
 * No secret move (Enzo, 15/10/2026).
 */
public final class CelestialPeakSpear {
    /** Long Reach: a hit this far out (blocks, body to body) is at the point. */
    public static final double REACH = 3.0;
    /** What a hit at the point adds, and how far it pushes (blocks). */
    public static final float REACH_BONUS = 1.2F;
    public static final double REACH_PUSH = 0.5;

    private CelestialPeakSpear() {
    }

    static MartialArts.Art art() {
        return MartialArts.art(MartialArts.CELESTIAL_PEAK_SPEAR);
    }

    /** Whether this player fights with Long Reach: the Celestial Peak Spear is their main style. */
    public static boolean stanceActive(Player player) {
        if (player.level().isClientSide)
            return com.murimblock.cultivation.MeditationService.aura(player).mainArt().filter(MartialArts.CELESTIAL_PEAK_SPEAR::equals).isPresent();
        return MartialService.data(player).mainArt().filter(MartialArts.CELESTIAL_PEAK_SPEAR::equals).isPresent();
    }

    /** A spear in the main hand: an item Epic Fight fights with as a spear (not the Traveller's Staff, which only does). */
    public static boolean holdsSpear(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || stack.is(com.murimblock.item.MurimItems.TRAVELLER_STAFF.get())) return false;
        CapabilityItem cap = EpicFightCapabilities.getItemStackCapability(stack);
        return cap != null && cap.getWeaponCategory() == CapabilityItem.WeaponCategories.SPEAR;
    }

    // ---------------------------------------------------------------- swings

    /** Epic Fight's spear strikes: one-handed and two-handed, the sprinting and the air attack. */
    static boolean isStrike(AnimationAccessor<? extends StaticAnimation> a) {
        return a == Animations.SPEAR_ONEHAND_AUTO || a == Animations.SPEAR_ONEHAND_AIR_SLASH
                || a == Animations.SPEAR_TWOHAND_AUTO1 || a == Animations.SPEAR_TWOHAND_AUTO2
                || a == Animations.SPEAR_DASH || a == Animations.SPEAR_TWOHAND_AIR_SLASH;
    }

    /** What an animation counts for in this art: the passive (the spear strikes, in the stance) or a technique. */
    @Nullable static ResourceLocation creditOf(Player player, AnimationAccessor<? extends StaticAnimation> a) {
        if (a == null) return null;
        if (isStrike(a)) return stanceActive(player) && holdsSpear(player) ? MartialArts.LONG_REACH.id() : null;
        if (a == MurimAnimations.CELESTIAL_PIERCING_DRAGON || a == MurimAnimations.CELESTIAL_PIERCING_DRAGON_LONG
                || a == MurimAnimations.CELESTIAL_PIERCING_DIVE) return MartialArts.PIERCING_DRAGON.id();
        if (a == MurimAnimations.CELESTIAL_PLUM_BLOSSOM || a == MurimAnimations.CELESTIAL_PLUM_RAIN || a == MurimAnimations.CELESTIAL_PLUM_AIR)
            return MartialArts.PLUM_BLOSSOM_THRUSTS.id();
        if (a == MurimAnimations.CELESTIAL_THOUSAND_SWEEP || a == MurimAnimations.CELESTIAL_TEN_THOUSAND_SWEEP
                || a == MurimAnimations.CELESTIAL_SWEEP_LANDING) return MartialArts.THOUSAND_SOLDIER_SWEEP.id();
        return null;
    }

    /** The art's hits that knock down, ending a combo. */
    static boolean knocksDown(AnimationAccessor<? extends StaticAnimation> a) {
        return a == MurimAnimations.CELESTIAL_PIERCING_DIVE || a == MurimAnimations.CELESTIAL_PLUM_AIR
                || a == MurimAnimations.CELESTIAL_THOUSAND_SWEEP || a == MurimAnimations.CELESTIAL_TEN_THOUSAND_SWEEP;
    }

    // ---------------------------------------------------------------- techniques

    /** Starts one of the art's techniques in the variant the moment calls for. The spear is checked before. */
    static boolean perform(ServerPlayer player, PlayerPatch<?> patch, MartialArts.Technique technique) {
        MartialData data = MartialService.data(player);
        boolean air = AzureCloudSword.airborne(player), evolved = data.evolved(art(), technique.id());
        if (technique == MartialArts.PIERCING_DRAGON) {
            if (air) { play(player, patch, MurimAnimations.CELESTIAL_PIERCING_DIVE, technique); MartialCombat.noFallFor(player, 40); }
            else play(player, patch, evolved ? MurimAnimations.CELESTIAL_PIERCING_DRAGON_LONG : MurimAnimations.CELESTIAL_PIERCING_DRAGON, technique);
            return true;
        }
        if (technique == MartialArts.PLUM_BLOSSOM_THRUSTS) {
            if (air) { play(player, patch, MurimAnimations.CELESTIAL_PLUM_AIR, technique); MartialCombat.noFallFor(player, 40); }
            else play(player, patch, evolved ? MurimAnimations.CELESTIAL_PLUM_RAIN : MurimAnimations.CELESTIAL_PLUM_BLOSSOM, technique);
            return true;
        }
        if (technique == MartialArts.THOUSAND_SOLDIER_SWEEP) {
            if (air) {
                play(player, patch, MurimAnimations.CELESTIAL_SWEEP_LANDING, technique);
                Vec3 m = player.getDeltaMovement();
                player.setDeltaMovement(m.x * 0.2, -1.2, m.z * 0.2);
                player.hurtMarked = true;
                MartialCombat.noFallFor(player, 40);
                MartialCombat.later(player, 5, () -> LongFist.ring(player.serverLevel(), player.position(), 3.0));
            } else {
                play(player, patch, evolved ? MurimAnimations.CELESTIAL_TEN_THOUSAND_SWEEP : MurimAnimations.CELESTIAL_THOUSAND_SWEEP, technique);
                MartialCombat.later(player, 8, () -> LongFist.ring(player.serverLevel(), player.position(), 3.5));
                if (evolved) MartialCombat.later(player, 26, () -> LongFist.ring(player.serverLevel(), player.position(), 4.0));
            }
            return true;
        }
        return false;
    }

    private static void play(ServerPlayer player, PlayerPatch<?> patch, AnimationAccessor<? extends StaticAnimation> animation,
                             MartialArts.Technique technique) {
        MartialCombat.expect(player, animation, technique.id(), false);
        patch.playAnimationSynchronized(animation, 0.0F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
    }

    // ---------------------------------------------------------------- Long Reach

    /** Whether a hit lands at the point: the target 3 blocks or more out, body to body. */
    public static boolean atReach(LivingEntity attacker, LivingEntity target) {
        Vec3 a = attacker.position(), t = target.position();
        double flat = Math.hypot(t.x - a.x, t.z - a.z) - target.getBbWidth() / 2;
        return flat >= REACH;
    }

    /** Whether Long Reach counts for this swing: the stance, a spear in hand. */
    static boolean reaches(ServerPlayer player) {
        return stanceActive(player) && holdsSpear(player);
    }

    /** After a hit: at the point, the little push and the quest; Piercing Dragon's bound for First Line. */
    static void onHit(ServerPlayer player, LivingEntity target, AnimationAccessor<? extends StaticAnimation> animation, boolean enemy) {
        if (MurimGuardSkill.parriedThisTick(target) || MurimGuardSkill.guardedThisTick(target)) return;
        if (reaches(player) && atReach(player, target)) {
            Vec3 away = target.position().subtract(player.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) {
                away = away.normalize().scale(REACH_PUSH * Math.max(0.2, 1 - target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)));
                target.push(away.x, 0.05, away.z);
                target.hurtMarked = true;
            }
            if (enemy) MartialProgress.count(player, MartialArts.HOLD_THE_DISTANCE_HITS, 1);
            ServerLevel level = player.serverLevel();
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.6F, 1.3F);
        }
    }
}
