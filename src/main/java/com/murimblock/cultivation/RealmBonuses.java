package com.murimblock.cultivation;

import com.murimblock.Murimblock;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * What a cultivation gives the body ({@link CultivationRules#bonuses}): more health, more damage, a little more speed,
 * armour, and harder bare hands, growing with each stage. Transient attribute modifiers with stable ids, put back on
 * login, on respawn and at each new stage; the bare-hand one only while the main hand is empty. And the weak against
 * the strong ({@link #shield}): blows from below land lighter.
 */
public final class RealmBonuses {
    static final ResourceLocation HEALTH = id("realm_health"), DAMAGE = id("realm_damage"), SPEED = id("realm_speed"), FIST = id("realm_fist"),
            ARMOR = id("realm_armor"), TOUGHNESS = id("realm_toughness"), JUMP = id("realm_jump"), SAFE_FALL = id("realm_safe_fall");

    private RealmBonuses() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
    }

    /** Puts the bonuses of the player's realm and stage on them (replacing any others). */
    public static void apply(ServerPlayer player) {
        CultivationRules.Bonuses b = CultivationRules.bonuses(CultivationService.getCultivation(player));
        float healthBefore = player.getHealth();
        set(player, Attributes.MAX_HEALTH, HEALTH, b.health(), AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.ATTACK_DAMAGE, DAMAGE, b.damage(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        set(player, Attributes.MOVEMENT_SPEED, SPEED, b.speed(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(player, Attributes.ARMOR, ARMOR, b.armor(), AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS, b.toughness(), AttributeModifier.Operation.ADD_VALUE);
        // Qinggong: a higher jump, a longer fall unhurt.
        CultivationRealm realm = CultivationService.getRealm(player);
        set(player, Attributes.JUMP_STRENGTH, JUMP, Qinggong.jump(realm), AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.SAFE_FALL_DISTANCE, SAFE_FALL, Qinggong.safeFall(realm), AttributeModifier.Operation.ADD_VALUE);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        else if (healthBefore > 0 && player.getHealth() < healthBefore) player.setHealth(healthBefore);
        updateFist(player);
    }

    /** The bare-hand bonus, on while the main hand is empty (called every few ticks). */
    public static void updateFist(ServerPlayer player) {
        double fist = player.getMainHandItem().isEmpty() ? CultivationRules.bonuses(CultivationService.getCultivation(player)).fist() : 0;
        set(player, Attributes.ATTACK_DAMAGE, FIST, fist, AttributeModifier.Operation.ADD_VALUE);
    }

    /**
     * A blow on a cultivator from someone weaker (a player of a lower stage, a creature of a lower cultivation, any other
     * creature counting as the very first stage) lands lighter: see {@link CultivationRules#shield}. Blows with no one
     * behind them (a fall, fire, the void) are untouched.
     */
    public static float shield(ServerPlayer player, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !(source.getEntity() instanceof LivingEntity attacker) || attacker == player) {
            return amount;
        }
        CultivationData theirs = QiSense.cultivationOf(attacker).orElse(CultivationData.initial());
        return (float) (amount * (1 - CultivationRules.shield(CultivationService.getCultivation(player), theirs)));
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
