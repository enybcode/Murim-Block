package com.murimblock.integration.epicfight;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import yesman.epicfight.registry.entries.EpicFightAttributes;

/** The engine does not automatically add its humanoid attributes to custom EntityTypes. */
public final class EpicFightMobSupport {
    private EpicFightMobSupport() { }

    public static AttributeSupplier.Builder humanoidAttributes(AttributeSupplier.Builder builder) {
        return builder.add(EpicFightAttributes.WEIGHT)
                .add(EpicFightAttributes.ARMOR_NEGATION)
                .add(EpicFightAttributes.IMPACT, 1.0)
                .add(EpicFightAttributes.MAX_STRIKES, 1.0)
                .add(EpicFightAttributes.STUN_ARMOR)
                .add(EpicFightAttributes.OFFHAND_ATTACK_SPEED)
                .add(EpicFightAttributes.OFFHAND_MAX_STRIKES)
                .add(EpicFightAttributes.OFFHAND_ARMOR_NEGATION)
                .add(EpicFightAttributes.OFFHAND_IMPACT);
    }
}
