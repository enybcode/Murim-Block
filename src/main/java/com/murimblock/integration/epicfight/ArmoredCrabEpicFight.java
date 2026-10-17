package com.murimblock.integration.epicfight;

import com.murimblock.Murimblock;
import com.murimblock.mob.MurimEntities;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.AnimationManager.AnimationRegistryEvent;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation.Phase;
import yesman.epicfight.api.animation.types.HitAnimation;
import yesman.epicfight.api.animation.types.LongHitAnimation;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.Armatures.ArmatureAccessor;
import yesman.epicfight.gameasset.ColliderPreset;

/**
 * Epic Fight registration of the armored crab: armature, the six clips exported from
 * C:\Users\enzob\crabe\armored-crab-enzo-animated (already at the 60 % in-game scale), claw colliders
 * and the entity patch. Clip times below come from that variant's README.
 */
public final class ArmoredCrabEpicFight {
    public static final ArmatureAccessor<ArmoredCrabArmature> ARMATURE =
            ArmatureAccessor.create(Murimblock.MOD_ID, "entity/armored_crab", ArmoredCrabArmature::new);

    // Half extents then centre, in blocks, in the palm joint's own axes. Measured at contact time: each palm
    // is only ~0.4 block ahead of the crab and ~0.4 to the side; local -Z points forward for both palms,
    // local -X (right) and -Y (left) point inward. Centres are pushed forward and inward so the claws reach
    // about 1.5 blocks ahead of the crab instead of 0.9 (Enzo: it could only hit when glued to the player).
    public static final Collider RIGHT_CLAW = ColliderPreset.registerCollider(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "armored_crab_right_claw"),
            new OBBCollider(0.5D, 0.4D, 0.55D, -0.2D, 0D, -0.55D));
    public static final Collider LEFT_CLAW = ColliderPreset.registerCollider(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "armored_crab_left_claw"),
            new OBBCollider(0.45D, 0.45D, 0.5D, 0D, -0.2D, -0.55D));

    // Null until Epic Fight runs the animation registry during mod construction.
    public static AnimationAccessor<StaticAnimation> IDLE;
    public static AnimationAccessor<MovementAnimation> WALK;
    public static AnimationAccessor<AttackAnimation> ATTACK_RIGHT;
    public static AnimationAccessor<AttackAnimation> ATTACK_LEFT;
    public static AnimationAccessor<HitAnimation> HURT;
    public static AnimationAccessor<LongHitAnimation> DEATH;

    private ArmoredCrabEpicFight() { }

    public static void register(IEventBus modBus) {
        // Epic Fight allows one animation builder per namespace: every Murimblock creature registers here.
        modBus.addListener(AnimationRegistryEvent.class, event -> event.newBuilder(Murimblock.MOD_ID, builder -> {
            buildAnimations(builder);
            CrabManEpicFight.buildAnimations(builder);
            TigerEpicFight.buildAnimations(builder);
            MurimAnimations.buildAnimations(builder);
            ScarletGuardianAnimations.build(builder);
        }));
        EpicFightEventHooks.Registry.ENTITY_PATCH.registerEvent(event -> {
            event.registerEntityPatch(MurimEntities.ARMORED_CRAB.get(), ArmoredCrabPatch::new);
            // Runs once on the main thread during common setup; the mapping is not cleared on reload.
            Armatures.registerEntityTypeArmature(MurimEntities.ARMORED_CRAB.get(), ARMATURE);
            CrabManEpicFight.registerPatch(event);
            TigerEpicFight.registerPatch(event);
            // The bandit: a humanoid on the engine's own biped skeleton.
            event.registerEntityPatch(MurimEntities.MOUNTAIN_BANDIT.get(), MountainBanditPatch::new);
            Armatures.registerEntityTypeArmature(MurimEntities.MOUNTAIN_BANDIT.get(), Armatures.BIPED);
            // Their chief: the same skeleton, drawn larger.
            event.registerEntityPatch(MurimEntities.BANDIT_CHIEF.get(), BanditChiefPatch::new);
            Armatures.registerEntityTypeArmature(MurimEntities.BANDIT_CHIEF.get(), Armatures.BIPED);
            // Their warlord: the same skeleton, drawn twice as large, his fists his weapons.
            event.registerEntityPatch(MurimEntities.BANDIT_WARLORD.get(), BanditWarlordPatch::new);
            Armatures.registerEntityTypeArmature(MurimEntities.BANDIT_WARLORD.get(), Armatures.BIPED);
            // The Son of the Demon: his own armature, a humanoid's joints where his colossal body bends.
            event.registerEntityPatch(MurimEntities.SON_OF_THE_DEMON.get(), SonOfTheDemonPatch::new);
            Armatures.registerEntityTypeArmature(MurimEntities.SON_OF_THE_DEMON.get(), ColossusAnimations.ARMATURE);
            // The Crab King: the biped skeleton too, drawn larger, his pincers on its arms.
            event.registerEntityPatch(MurimEntities.CRAB_KING.get(), CrabKingPatch::new);
            Armatures.registerEntityTypeArmature(MurimEntities.CRAB_KING.get(), Armatures.BIPED);
            // The Heart Demon: a copy of its owner, on the player's own skeleton.
            event.registerEntityPatch(MurimEntities.HEART_DEMON.get(), HeartDemonPatch::new);
            Armatures.registerEntityTypeArmature(MurimEntities.HEART_DEMON.get(), Armatures.BIPED);
            // Sensei Yun: the same skeleton, his own model.
            event.registerEntityPatch(MurimEntities.AZURE_SENSEI.get(), AzureSenseiPatch::new);
            Armatures.registerEntityTypeArmature(MurimEntities.AZURE_SENSEI.get(), Armatures.BIPED);
            // The Alliance Merchant: the same skeleton, his own model, no fighting.
            event.registerEntityPatch(MurimEntities.ALLIANCE_MERCHANT.get(), AllianceMerchantPatch::new);
            Armatures.registerEntityTypeArmature(MurimEntities.ALLIANCE_MERCHANT.get(), Armatures.BIPED);
            // The Golden Bell monk: the same skeleton, his own model, his bare fists.
            event.registerEntityPatch(MurimEntities.SCARLET_GUARDIAN.get(), ScarletGuardianPatch::new);
            Armatures.registerEntityTypeArmature(MurimEntities.SCARLET_GUARDIAN.get(), Armatures.BIPED);
            event.registerEntityPatch(MurimEntities.GOLDEN_BELL_MONK.get(), GoldenBellMonkPatch::new);
            Armatures.registerEntityTypeArmature(MurimEntities.GOLDEN_BELL_MONK.get(), Armatures.BIPED);
            event.registerEntityPatch(MurimEntities.GOLDEN_DRAGON_SAGE.get(), GoldenDragonSagePatch::new);
            Armatures.registerEntityTypeArmature(MurimEntities.GOLDEN_DRAGON_SAGE.get(), Armatures.BIPED);
            // The Captain of the Peak Guard: the same skeleton, his own model, his armour and spear.
            event.registerEntityPatch(MurimEntities.PEAK_CAPTAIN.get(), PeakCaptainPatch::new);
            Armatures.registerEntityTypeArmature(MurimEntities.PEAK_CAPTAIN.get(), Armatures.BIPED);
            // The Alliance's soldiers: the same skeleton, their own model, each with his weapon.
            for (var soldier : MurimEntities.ALLIANCE_SOLDIERS) {
                event.registerEntityPatch(soldier.get(), AllianceSoldierPatch::new);
                Armatures.registerEntityTypeArmature(soldier.get(), Armatures.BIPED);
            }
        });
    }

    private static void buildAnimations(AnimationBuilder builder) {
        IDLE = builder.nextAccessor("armored_crab/idle", a -> new StaticAnimation(true, a, ARMATURE));
        WALK = builder.nextAccessor("armored_crab/walk", a -> new MovementAnimation(true, a, ARMATURE));
        // start, antic, preDelay, contact, recovery, end: the hit window is preDelay..contact.
        ATTACK_RIGHT = builder.nextAccessor("armored_crab/attack_right", a -> CrabManEpicFight.designedSpeed(new AttackAnimation(0.15F, a, ARMATURE,
                new Phase(0.0F, 0.45F, 0.50F, 0.62F, 0.85F, 1.25F, ARMATURE.get().clawRightPalm, RIGHT_CLAW))));
        ATTACK_LEFT = builder.nextAccessor("armored_crab/attack_left", a -> CrabManEpicFight.designedSpeed(new AttackAnimation(0.10F, a, ARMATURE,
                new Phase(0.0F, 0.30F, 0.34F, 0.44F, 0.60F, 22F / 24F, ARMATURE.get().clawLeftPalm, LEFT_CLAW))));
        HURT = builder.nextAccessor("armored_crab/hurt", a -> new HitAnimation(0.08F, a, ARMATURE));
        DEATH = builder.nextAccessor("armored_crab/death", a -> new LongHitAnimation(0.16F, a, ARMATURE));
    }
}
