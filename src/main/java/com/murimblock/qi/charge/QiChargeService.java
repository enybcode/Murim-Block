package com.murimblock.qi.charge;

import com.murimblock.Murimblock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class QiChargeService {
    static final ResourceLocation MOVEMENT_LOCK_ID = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "qi_charge_movement_lock");
    static final ResourceLocation JUMP_LOCK_ID = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "qi_charge_jump_lock");

    private static final AttributeModifier MOVEMENT_LOCK = new AttributeModifier(
            MOVEMENT_LOCK_ID,
            -1.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );
    private static final AttributeModifier JUMP_LOCK = new AttributeModifier(
            JUMP_LOCK_ID,
            -1.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );
    private static final Map<UUID, QiChargeState> STATES = new ConcurrentHashMap<>();

    private QiChargeService() {
    }

    public static boolean isCharging(Player player) {
        QiChargeState state = STATES.get(player.getUUID());
        return state != null && state.isCharging();
    }

    public static boolean setCharging(ServerPlayer player, boolean charging) {
        if (charging) {
            return startCharging(player);
        }
        return stopCharging(player);
    }

    public static boolean startCharging(ServerPlayer player) {
        if (!canCharge(player)) {
            return stopCharging(player);
        }

        QiChargeState current = STATES.computeIfAbsent(player.getUUID(), ignored -> new QiChargeState());
        if (current.isCharging()) {
            applyMovementLocks(player);
            return false;
        }

        current.start();
        applyMovementLocks(player);
        return true;
    }

    public static boolean stopCharging(ServerPlayer player) {
        QiChargeState state = STATES.remove(player.getUUID());
        removeMovementLocks(player);
        return state != null && state.isCharging();
    }

    public static void stopIfServerPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            stopCharging(serverPlayer);
        }
    }

    public static void keepValid(ServerPlayer player) {
        if (!isCharging(player)) {
            return;
        }

        if (!canCharge(player)) {
            stopCharging(player);
            return;
        }

        player.setSprinting(false);
        applyMovementLocks(player);
    }

    public static boolean canCharge(Player player) {
        return player.isAlive() && !player.isSpectator();
    }

    static int trackedStateCount() {
        return STATES.size();
    }

    static void clearAllForTests() {
        STATES.clear();
    }

    private static void applyMovementLocks(LivingEntity entity) {
        addOrUpdateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_LOCK);
        addOrUpdateModifier(entity.getAttribute(Attributes.JUMP_STRENGTH), JUMP_LOCK);
    }

    private static void removeMovementLocks(LivingEntity entity) {
        removeModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_LOCK_ID);
        removeModifier(entity.getAttribute(Attributes.JUMP_STRENGTH), JUMP_LOCK_ID);
    }

    private static void addOrUpdateModifier(AttributeInstance instance, AttributeModifier modifier) {
        if (instance != null) {
            instance.addOrUpdateTransientModifier(modifier);
        }
    }

    private static void removeModifier(AttributeInstance instance, ResourceLocation modifierId) {
        if (instance != null) {
            instance.removeModifier(modifierId);
        }
    }
}
