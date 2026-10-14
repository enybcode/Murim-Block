package com.murimblock.cultivation;

import com.murimblock.Murimblock;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * What a realm gives the body ({@link CultivationRules#bonuses}): more health, more damage, a little more speed, and
 * harder bare hands. Transient attribute modifiers with stable ids, put back on login, on respawn and at each new realm;
 * the bare-hand one only while the main hand is empty.
 */
public final class RealmBonuses {
    static final ResourceLocation HEALTH = id("realm_health"), DAMAGE = id("realm_damage"), SPEED = id("realm_speed"), FIST = id("realm_fist");

    private RealmBonuses() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
    }

    /** Puts the bonuses of the player's realm on them (replacing those of any other realm). */
    public static void apply(ServerPlayer player) {
        CultivationRules.Bonuses b = CultivationRules.bonuses(CultivationService.getRealm(player));
        float healthBefore = player.getHealth();
        set(player, Attributes.MAX_HEALTH, HEALTH, b.health(), AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.ATTACK_DAMAGE, DAMAGE, b.damage(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        set(player, Attributes.MOVEMENT_SPEED, SPEED, b.speed(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        else if (healthBefore > 0 && player.getHealth() < healthBefore) player.setHealth(healthBefore);
        updateFist(player);
    }

    /** The bare-hand bonus, on while the main hand is empty (called every few ticks). */
    public static void updateFist(ServerPlayer player) {
        double fist = player.getMainHandItem().isEmpty() ? CultivationRules.bonuses(CultivationService.getRealm(player)).fist() : 0;
        set(player, Attributes.ATTACK_DAMAGE, FIST, fist, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void set(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier current = instance.getModifier(id);
        if (amount == 0) {
            if (current != null) instance.removeModifier(id);
            return;
        }
        if (current != null && current.amount() == amount && current.operation() == operation) return;
        if (current != null) instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, amount, operation));
    }
}
