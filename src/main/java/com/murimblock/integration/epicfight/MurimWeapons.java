package com.murimblock.integration.epicfight;

import com.murimblock.Murimblock;
import net.neoforged.bus.api.IEventBus;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.ex_cap.data.Moveset;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.registry.deferred.ItemPresetRegister;
import yesman.epicfight.registry.deferred.MovesetRegister;
import yesman.epicfight.registry.deferred.holders.DeferredMoveset;
import yesman.epicfight.registry.deferred.holders.DeferredWeapon;
import yesman.epicfight.registry.entries.EpicFightParticles;
import yesman.epicfight.registry.entries.EpicFightProviderConditionals;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

/**
 * The Epic Fight weapon presets of the sect weapons (MurimWeaponCategories), built as Epic Fight's own and as addons
 * such as Dawn Day build theirs: a moveset per weapon (its stance, walk, run and guard, its four M1 strikes, running
 * and air attacks, SectWeaponAnimations) and a weapon preset giving its type, hit box, sounds and that moveset in one
 * hand. Each item names its preset in data/murimblock/capabilities/weapons/&lt;item&gt;.json ("type": "murimblock:&lt;preset&gt;").
 */
public final class MurimWeapons {
    private static final MovesetRegister MOVESETS = MovesetRegister.create(Murimblock.MOD_ID);
    private static final ItemPresetRegister PRESETS = ItemPresetRegister.create(Murimblock.MOD_ID);

    public static final DeferredMoveset IRON_FAN_1H = MOVESETS.registerMoveset("iron_fan_1h", () -> Moveset.builder()
            .addComboAttacks(SectWeaponAnimations.FAN_AUTO1, SectWeaponAnimations.FAN_AUTO2, SectWeaponAnimations.FAN_AUTO3,
                    SectWeaponAnimations.FAN_AUTO4, SectWeaponAnimations.FAN_DASH, SectWeaponAnimations.FAN_AIR)
            .addLivingMotionModifier(LivingMotions.IDLE, SectWeaponAnimations.FAN_IDLE)
            .addLivingMotionModifier(LivingMotions.WALK, Animations.BIPED_WALK)
            .addLivingMotionModifier(LivingMotions.RUN, Animations.BIPED_RUN)
            .addLivingMotionModifier(LivingMotions.BLOCK, Animations.SWORD_GUARD));
    public static final DeferredMoveset CHAIN_WHIP_1H = MOVESETS.registerMoveset("chain_whip_1h", () -> Moveset.builder()
            .addComboAttacks(SectWeaponAnimations.WHIP_AUTO1, SectWeaponAnimations.WHIP_AUTO2, SectWeaponAnimations.WHIP_AUTO3,
                    SectWeaponAnimations.WHIP_AUTO4, SectWeaponAnimations.WHIP_DASH, SectWeaponAnimations.WHIP_AIR)
            .addLivingMotionModifier(LivingMotions.IDLE, SectWeaponAnimations.WHIP_IDLE)
            .addLivingMotionModifier(LivingMotions.WALK, Animations.BIPED_WALK)
            .addLivingMotionModifier(LivingMotions.RUN, Animations.BIPED_RUN)
            .addLivingMotionModifier(LivingMotions.BLOCK, Animations.SWORD_GUARD));
    public static final DeferredMoveset FLYING_NEEDLES_1H = MOVESETS.registerMoveset("flying_needles_1h", () -> Moveset.builder()
            .addComboAttacks(SectWeaponAnimations.NEEDLES_AUTO1, SectWeaponAnimations.NEEDLES_AUTO2, SectWeaponAnimations.NEEDLES_AUTO3,
                    SectWeaponAnimations.NEEDLES_AUTO4, SectWeaponAnimations.NEEDLES_DASH, SectWeaponAnimations.NEEDLES_AIR)
            .addLivingMotionModifier(LivingMotions.IDLE, SectWeaponAnimations.NEEDLES_IDLE)
            .addLivingMotionModifier(LivingMotions.WALK, Animations.BIPED_WALK)
            .addLivingMotionModifier(LivingMotions.RUN, Animations.BIPED_RUN)
            .addLivingMotionModifier(LivingMotions.BLOCK, Animations.SWORD_GUARD));

    public static final DeferredWeapon IRON_FAN = PRESETS.registerWeapon("iron_fan", () -> WeaponCapability.builder()
            .category(MurimWeaponCategories.IRON_FAN)
            .collider(new OBBCollider(0.5D, 0.6D, 0.9D, 0.0D, 0.0D, -0.5D))
            .swingSound(EpicFightSounds.WHOOSH_SMALL)
            .hitSound(EpicFightSounds.BLADE_HIT)
            .hitParticle(EpicFightParticles.HIT_BLADE)
            .canBePlacedOffhand(false)
            .addConditionals(EpicFightProviderConditionals.DEFAULT_1H_WIELD_STYLE)
            .addMoveset(CapabilityItem.Styles.ONE_HAND, IRON_FAN_1H));
    public static final DeferredWeapon CHAIN_WHIP = PRESETS.registerWeapon("chain_whip", () -> WeaponCapability.builder()
            .category(MurimWeaponCategories.CHAIN_WHIP)
            .collider(new OBBCollider(0.4D, 0.4D, 2.4D, 0.0D, 0.0D, -2.4D))
            .swingSound(EpicFightSounds.WHOOSH_SHARP)
            .hitSound(EpicFightSounds.BLUNT_HIT)
            .hitParticle(EpicFightParticles.HIT_BLUNT)
            .canBePlacedOffhand(false)
            .addConditionals(EpicFightProviderConditionals.DEFAULT_1H_WIELD_STYLE)
            .addMoveset(CapabilityItem.Styles.ONE_HAND, CHAIN_WHIP_1H));
    public static final DeferredWeapon FLYING_NEEDLES = PRESETS.registerWeapon("flying_needles", () -> WeaponCapability.builder()
            .category(MurimWeaponCategories.FLYING_NEEDLES)
            .collider(new OBBCollider(0.2D, 0.2D, 0.3D, 0.0D, 0.0D, -0.2D))
            .swingSound(EpicFightSounds.WHOOSH_SMALL)
            .hitSound(EpicFightSounds.BLADE_HIT)
            .hitParticle(EpicFightParticles.HIT_BLADE)
            .canBePlacedOffhand(false)
            .addConditionals(EpicFightProviderConditionals.DEFAULT_1H_WIELD_STYLE)
            .addMoveset(CapabilityItem.Styles.ONE_HAND, FLYING_NEEDLES_1H));

    private MurimWeapons() { }

    public static void register(IEventBus modEventBus) {
        MOVESETS.register(modEventBus);
        PRESETS.register(modEventBus);
    }
}
