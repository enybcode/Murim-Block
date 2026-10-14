package com.murimblock.integration.epicfight;

import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.martial.LongFist;
import com.murimblock.martial.MartialCombat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.event.EntityEventListener;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.entity.TakeDamageEvent;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.registry.entries.EpicFightSkillDataKeys;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.skill.guard.ParryingSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CapabilityItem.Styles;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;

/**
 * Murim's guard (docs/combat/STYLES.md, "Deux sortes de parade"), in Epic Fight's guard slot instead of its basic
 * guard. Held on the guard key, it faces attacks from the front, hits, arrows, water balls and skills alike:
 * <ul>
 *   <li><b>perfect parry</b>: guard raised at most {@link #PERFECT_WINDOW_TICKS} (0.5 s) before the hit lands. The hit
 *   is cancelled entirely (an arrow glances off), a burst of grey specks flies where the blade meets it, and a melee
 *   attacker is thrown off balance;</li>
 *   <li><b>normal parry</b>: guard raised earlier and held. Half the damage goes through, without the hit reaction.</li>
 * </ul>
 * Built on Epic Fight's parrying skill, for its parry and guard animations and its record of when the guard went up
 * (which also stops spamming the key for a perfect parry). What a passive changes (reduction, riposte...) comes later.
 */
public class MurimGuardSkill extends ParryingSkill {
    /** The perfect parry window: the guard went up at most this many ticks before the hit (0.5 s). */
    public static final int PERFECT_WINDOW_TICKS = 10;
    /** Share of the damage a normal parry lets through. */
    public static final float NORMAL_PARRY_DAMAGE = 0.5F;

    /** Players whose current hit was taken on a normal parry (by tick): their damage is halved, their flinch skipped. */
    private static final Map<UUID, Integer> GUARDED_HIT = new ConcurrentHashMap<>();
    /** Players who stopped their current hit with a perfect parry (by tick): its side effects are skipped too. */
    private static final Map<UUID, Integer> PARRIED_HIT = new ConcurrentHashMap<>();

    public MurimGuardSkill(GuardSkill.Builder builder) {
        super(builder);
    }

    /**
     * Epic Fight's guard animations, with its parrying animations for the perfect parry; and bare hands (Epic Fight's
     * fist category) guard with the forearms, only for a player whose main style is the Long Fist (otherwise no guard
     * motion, so no guard, as before).
     */
    public static GuardSkill.Builder createMurimGuardBuilder() {
        return GuardSkill.createGuardBuilder(MurimGuardSkill::new)
                .addGuardMotion(WeaponCategories.FIST, (item, player) -> LongFist.guardsBareHanded(player.getOriginal()) ? MurimAnimations.LONG_FIST_GUARD_HIT : null)
                .addAdvancedGuardMotion(WeaponCategories.FIST, (item, player) -> LongFist.guardsBareHanded(player.getOriginal())
                        ? List.of(MurimAnimations.LONG_FIST_GUARD_HIT) : null)
                .addAdvancedGuardMotion(WeaponCategories.SWORD, (item, player) -> item.getStyle(player) == Styles.ONE_HAND
                        ? List.of(Animations.SWORD_GUARD_ACTIVE_HIT1, Animations.SWORD_GUARD_ACTIVE_HIT2)
                        : List.of(Animations.SWORD_GUARD_ACTIVE_HIT2, Animations.SWORD_GUARD_ACTIVE_HIT3))
                .addAdvancedGuardMotion(WeaponCategories.LONGSWORD, (item, player) -> List.of(Animations.LONGSWORD_GUARD_ACTIVE_HIT1, Animations.LONGSWORD_GUARD_ACTIVE_HIT2))
                .addAdvancedGuardMotion(WeaponCategories.UCHIGATANA, (item, player) -> List.of(Animations.SWORD_GUARD_ACTIVE_HIT1, Animations.SWORD_GUARD_ACTIVE_HIT2))
                .addAdvancedGuardMotion(WeaponCategories.TACHI, (item, player) -> List.of(Animations.LONGSWORD_GUARD_ACTIVE_HIT1, Animations.LONGSWORD_GUARD_ACTIVE_HIT2));
    }

    @Override
    public void guard(SkillContainer container, CapabilityItem itemCapability, ServerPlayerPatch playerPatch, TakeDamageEvent.Income event,
                      float knockback, float impact, boolean advanced) {
        DamageSource source = event.getDamageSource();
        Entity offender = getOffender(source);
        if (offender == null || !perfectParryable(source)) return;
        ServerPlayer player = playerPatch.getOriginal();
        // A guard broken by a riposte (Thousand Drizzle) parries nothing for a while.
        if (MartialCombat.guardBroken(player)) return;
        int raised = container.getDataManager().getDataValue(EpicFightSkillDataKeys.LAST_ACTIVE);
        boolean perfect = player.tickCount - raised <= PERFECT_WINDOW_TICKS;
        // What pierces a guard (some skills, unblockable attacks) only a perfect parry stops.
        if (!perfect && !normalParryable(source)) return;
        playerPatch.playSound(EpicFightSounds.CLASH.get(), -0.05F, 0.1F);
        if (perfect) {
            PARRIED_HIT.put(player.getUUID(), player.tickCount);
            // One perfect parry per raise of the guard.
            container.getDataManager().setData(EpicFightSkillDataKeys.LAST_ACTIVE, 0);
            event.setParried(true);
            perfectParrySpark(player, offender);
            AnimationAccessor<? extends StaticAnimation> motion = this.getGuardMotion(container, playerPatch, itemCapability, BlockType.ADVANCED_GUARD);
            if (motion == null) motion = this.getGuardMotion(container, playerPatch, itemCapability, BlockType.GUARD);
            if (motion != null) playerPatch.playAnimationSynchronized(motion, 0.0F);
            // A melee attacker is thrown off balance (an archer is too far away to be).
            if (!(source.getDirectEntity() instanceof Projectile) && offender instanceof LivingEntity attacker) {
                LivingEntityPatch<?> attackerPatch = EpicFightCapabilities.getEntityPatch(attacker, LivingEntityPatch.class);
                if (attackerPatch != null) attackerPatch.applyStun(StunType.NEUTRALIZE, 0.6F);
            }
            this.dealEvent(playerPatch, event, true);
            // The main style's passive answers the parry (Flowing Cloud Stance: Qi back, a riposte window).
            MartialCombat.onPerfectParry(player, offender);
        } else {
            AnimationAccessor<? extends StaticAnimation> motion = this.getGuardMotion(container, playerPatch, itemCapability, BlockType.GUARD);
            if (motion != null) playerPatch.playAnimationSynchronized(motion, 0.0F);
            playerPatch.knockBackEntity(offender.position(), 0.15F);
            // Not cancelled: the hit goes on, halved (damageShare) and without the flinch (guardedThisTick).
            GUARDED_HIT.put(player.getUUID(), player.tickCount);
        }
    }

    /** Grey of the perfect parry spark, and how many specks it throws. */
    private static final DustParticleOptions SPARK = new DustParticleOptions(new Vector3f(0.62F, 0.62F, 0.66F), 1.1F);
    private static final int SPARK_COUNT = 14;

    /**
     * A small burst of grey specks where the blade meets the blow (or the arrow): between the player and what hit
     * them, at chest height. Sent by the server, so everyone around sees it.
     */
    static void perfectParrySpark(ServerPlayer player, Entity offender) {
        Vec3 chest = player.position().add(0, player.getBbHeight() * 0.65, 0);
        Vec3 toward = offender.position().add(0, offender.getBbHeight() * 0.5, 0).subtract(chest);
        Vec3 at = toward.lengthSqr() < 1e-4 ? chest : chest.add(toward.normalize().scale(0.6));
        player.serverLevel().sendParticles(SPARK, at.x, at.y, at.z, SPARK_COUNT, 0.15, 0.15, 0.15, 0.02);
    }

    /**
     * Every attack has its perfect parry frame: hits, arrows and other projectiles (the crab man's water balls), skills,
     * even those made to pierce a guard. Not what is no attack (falls, burning, drowning: no attacker), what nothing
     * stops (the void, /kill), a blast, or a finish.
     */
    static boolean perfectParryable(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.is(DamageTypeTags.IS_EXPLOSION)
                && !source.is(KnockdownService.EXECUTION);
    }

    /** A normal parry is narrower: not what pierces a guard, nor magic, fire or what goes through armour (but arrows). */
    static boolean normalParryable(DamageSource source) {
        return perfectParryable(source) && !source.is(EpicFightDamageTypeTags.UNBLOCKALBE) && !source.is(EpicFightDamageTypeTags.GUARD_PUNCTURE)
                && !source.is(DamageTypes.MAGIC) && !source.is(DamageTypeTags.IS_FIRE)
                && (!source.is(DamageTypeTags.BYPASSES_ARMOR) || source.is(DamageTypeTags.IS_PROJECTILE));
    }

    /**
     * Epic Fight's guard lets guard-piercing attacks straight through, before its parry is even asked. This second
     * listener gives them their perfect parry frame: same conditions (guard held, attack from the front).
     */
    @Override
    public void onInitiate(SkillContainer container, EntityEventListener listener) {
        super.onInitiate(container, listener);
        listener.registerEvent(EpicFightEventHooks.Entity.TAKE_DAMAGE_INCOME, event -> {
            if (!event.getDamageSource().is(EpicFightDamageTypeTags.GUARD_PUNCTURE) || event.isCanceled()) return;
            if (!container.isActivated() || container.getExecutor().getHoldingSkill() != this) return;
            if (!(container.getExecutor() instanceof ServerPlayerPatch patch) || !fromTheFront(patch.getOriginal(), event.getDamageSource())) return;
            this.guard(container, patch.getPrimaryItemCapability(), patch, event, 0.25F, 0.5F, true);
        }, this, 1);
    }

    /** Whether an attack comes from in front of the player (where the guard faces). */
    static boolean fromTheFront(ServerPlayer player, DamageSource source) {
        Vec3 from = source.getSourcePosition();
        if (from == null) return false;
        Vec3 view = player.getViewVector(1.0F);
        view = view.subtract(0, view.y, 0).normalize();
        return from.subtract(player.position()).normalize().dot(view) > 0.0;
    }

    /** Whether this player stopped the current hit with a perfect parry (this tick). */
    public static boolean parriedThisTick(LivingEntity entity) {
        Integer tick = PARRIED_HIT.get(entity.getUUID());
        return tick != null && tick == entity.tickCount;
    }

    /** Whether this player stopped a hit with a perfect parry in the last {@code ticks} ticks. */
    public static boolean parriedWithin(LivingEntity entity, int ticks) {
        Integer tick = PARRIED_HIT.get(entity.getUUID());
        return tick != null && entity.tickCount - tick <= ticks;
    }

    /** Whether this player took the current hit on a normal parry (this tick). */
    public static boolean guardedThisTick(LivingEntity entity) {
        Integer tick = GUARDED_HIT.get(entity.getUUID());
        return tick != null && tick == entity.tickCount;
    }

    /** The share of this hit's damage that goes through: halved on a normal parry this tick. */
    static float damageShare(ServerPlayer player) {
        Integer tick = GUARDED_HIT.get(player.getUUID());
        return tick != null && tick == player.tickCount ? NORMAL_PARRY_DAMAGE : 1.0F;
    }


}
