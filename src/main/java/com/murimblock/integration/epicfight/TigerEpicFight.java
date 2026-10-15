package com.murimblock.integration.epicfight;

import com.murimblock.Murimblock;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.TigerEntity;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationEvent.InTimeEvent;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation.Phase;
import yesman.epicfight.api.animation.types.HitAnimation;
import yesman.epicfight.api.animation.types.LongHitAnimation;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.event.types.registry.EntityPatchRegistryEvent;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.Armatures.ArmatureAccessor;
import yesman.epicfight.gameasset.ColliderPreset;
import yesman.epicfight.world.damagesource.StunType;

/**
 * Epic Fight side of the tiger (docs/mobs/tiger.md): its armature and mesh, its clips (exported from the Blockbench
 * project by scripts/animation/tiger-epicfight.cjs), the colliders of its paws, jaws and body, and its five attacks.
 * Damage is the tiger's 8 times each phase's modifier; every blow can be parried.
 */
public final class TigerEpicFight {
    public static final ArmatureAccessor<TigerArmature> ARMATURE =
            ArmatureAccessor.create(Murimblock.MOD_ID, "entity/tiger", TigerArmature::new);

    // Half extents then centre, in blocks, in the joint's own axes (Blockbench's: x across, y up, z toward the back).
    /** A forepaw and its claws, reaching a little ahead. */
    public static final Collider PAW = ColliderPreset.registerCollider(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "tiger_paw"),
            new OBBCollider(0.45D, 0.4D, 0.75D, 0D, -0.3D, -0.45D));
    /** The heavy blow's paw: bigger. */
    public static final Collider MAUL_PAW = ColliderPreset.registerCollider(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "tiger_maul_paw"),
            new OBBCollider(0.6D, 0.6D, 0.85D, 0D, -0.3D, -0.5D));
    /** The jaws, in front of the head. */
    public static final Collider JAWS = ColliderPreset.registerCollider(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "tiger_jaws"),
            new OBBCollider(0.45D, 0.4D, 0.55D, 0D, -0.25D, -1.0D));
    /** The whole fore end as it lands from the pounce. */
    public static final Collider POUNCE = ColliderPreset.registerCollider(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "tiger_pounce"),
            new OBBCollider(0.8D, 0.75D, 0.95D, 0D, -0.4D, -1.8D));

    public static AnimationAccessor<StaticAnimation> IDLE;
    public static AnimationAccessor<MovementAnimation> WALK;
    public static AnimationAccessor<AttackAnimation> SWIPE;
    public static AnimationAccessor<AttackAnimation> BITE;
    public static AnimationAccessor<AttackAnimation> DOUBLE_SWIPE;
    public static AnimationAccessor<AttackAnimation> POUNCE_ATTACK;
    public static AnimationAccessor<AttackAnimation> MAUL;
    public static AnimationAccessor<HitAnimation> HURT;
    public static AnimationAccessor<LongHitAnimation> DEATH;

    private TigerEpicFight() { }

    static void buildAnimations(AnimationBuilder builder) {
        IDLE = builder.nextAccessor("tiger/idle", a -> new StaticAnimation(true, a, ARMATURE));
        WALK = builder.nextAccessor("tiger/walk", a -> new MovementAnimation(true, a, ARMATURE));
        // Phase: start, antic, preDelay, contact, recovery, end (seconds of the clip); the hit window is preDelay..contact.
        SWIPE = builder.nextAccessor("tiger/swipe", a -> CrabManEpicFight.designedSpeed(new AttackAnimation(0.12F, a, ARMATURE,
                new Phase(0.0F, 0.18F, 0.22F, 0.36F, 0.5F, 0.8F, ARMATURE.get().frontFootRight, PAW))));
        BITE = builder.nextAccessor("tiger/bite", a -> CrabManEpicFight.designedSpeed(new AttackAnimation(0.12F, a, ARMATURE,
                new Phase(0.0F, 0.3F, 0.36F, 0.48F, 0.65F, 0.9F, ARMATURE.get().head, JAWS))));
        DOUBLE_SWIPE = builder.nextAccessor("tiger/double_swipe", a -> CrabManEpicFight.designedSpeed(new AttackAnimation(0.12F, a, ARMATURE,
                new Phase(0.0F, 0.15F, 0.2F, 0.32F, 0.4F, 0.45F, ARMATURE.get().frontFootRight, PAW)
                        .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(0.75F)),
                new Phase(0.45F, 0.45F, 0.5F, 0.66F, 0.85F, 1.2F, ARMATURE.get().frontFootLeft, PAW)
                        .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(0.75F)))));
        // The pounce: the tiger springs at 0.36 s (thrown by the game, the clip leaves the leap out) and strikes as it lands.
        POUNCE_ATTACK = builder.nextAccessor("tiger/pounce", a -> CrabManEpicFight.designedSpeed(new AttackAnimation(0.12F, a, ARMATURE,
                new Phase(0.0F, 0.5F, 0.55F, 0.95F, 1.1F, 1.6F, ARMATURE.get().body, POUNCE)
                        .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(1.25F))
                        .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.LONG)))
                .addEvents(InTimeEvent.create(0.36F, (AnimationEvent.E0) (patch, animation, params) -> {
                    if (patch.getOriginal() instanceof TigerEntity tiger) tiger.leapAt(patch.getTarget());
                }, AnimationEvent.Side.SERVER)));
        // The maul: slow and plain to see, it hits hardest and throws the target back.
        MAUL = builder.nextAccessor("tiger/maul", a -> CrabManEpicFight.designedSpeed(new AttackAnimation(0.15F, a, ARMATURE,
                new Phase(0.0F, 0.8F, 0.84F, 1.0F, 1.2F, 1.8F, ARMATURE.get().frontFootRight, MAUL_PAW)
                        .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(1.5F))
                        .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.LONG)
                        .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.multiplier(2.5F)))));
        HURT = builder.nextAccessor("tiger/hurt", a -> new HitAnimation(0.08F, a, ARMATURE));
        DEATH = builder.nextAccessor("tiger/death", a -> new LongHitAnimation(0.16F, a, ARMATURE));
    }

    static void registerPatch(EntityPatchRegistryEvent event) {
        event.registerEntityPatch(MurimEntities.TIGER.get(), TigerPatch::new);
        Armatures.registerEntityTypeArmature(MurimEntities.TIGER.get(), ARMATURE);
    }
}
