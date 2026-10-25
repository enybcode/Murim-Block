package com.murimblock.innerworld;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/**
 * A zombie of the mortal trial: it climbs out of a grave and hunts the player. It drops nothing, gives no
 * experience, never burns, never turns into a drowned, stays even in Peaceful, and is never saved: each trial
 * summons its own. The Grave Warden of the last wave is a bigger, tougher one.
 */
public final class TrialZombie extends Zombie {
    // Softer than a vanilla zombie (20 health, 3 damage): a mortal, maybe bare-handed, faces fifteen of them.
    private static final double HEALTH = 10;
    private static final double DAMAGE = 2;
    private static final double SPEED = 0.21;
    private static final double WARDEN_HEALTH = 30;

    public TrialZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ATTACK_DAMAGE, DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, SPEED).add(Attributes.FOLLOW_RANGE, 64);
    }

    /** Makes this one the Grave Warden: 1.6 times as big, three times the health, harder blows, hard to push back. */
    public void makeWarden() {
        getAttribute(Attributes.SCALE).setBaseValue(1.6);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(WARDEN_HEALTH);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.8);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2);
        setHealth((float) WARDEN_HEALTH);
        setCustomName(Component.translatable("entity.murimblock.grave_warden"));
        setCustomNameVisible(true);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
