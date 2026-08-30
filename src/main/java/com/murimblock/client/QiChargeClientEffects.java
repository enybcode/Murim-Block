package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.qi.charge.QiChargeVisuals;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QiChargeClientEffects {
    private static final RandomSource RANDOM = RandomSource.create();
    private static int foregroundCooldown;

    private QiChargeClientEffects() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || !QiChargeClientHandler.isVisualCharging()) {
            resetForegroundCooldown();
            return;
        }

        if (!minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        if (foregroundCooldown > 0) {
            foregroundCooldown--;
            return;
        }

        spawnForegroundParticle(minecraft, level);
        resetForegroundCooldown();
    }

    static int nextForegroundCooldown(RandomSource random) {
        return QiChargeVisuals.QI_CHARGE_FOREGROUND_INTERVAL_MIN_TICKS
                + random.nextInt(QiChargeVisuals.QI_CHARGE_FOREGROUND_INTERVAL_RANGE_TICKS);
    }

    private static void spawnForegroundParticle(Minecraft minecraft, ClientLevel level) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 position = camera.getPosition();
        Vec3 look = new Vec3(camera.getLookVector());
        Vec3 right = new Vec3(camera.getLeftVector()).scale(-1.0);
        Vec3 up = new Vec3(camera.getUpVector());

        double distance = QiChargeVisuals.QI_CHARGE_FOREGROUND_DISTANCE_MIN
                + RANDOM.nextDouble() * QiChargeVisuals.QI_CHARGE_FOREGROUND_DISTANCE_RANGE;
        double side = (RANDOM.nextBoolean() ? 1.0 : -1.0)
                * (QiChargeVisuals.QI_CHARGE_FOREGROUND_HORIZONTAL_MIN
                + RANDOM.nextDouble() * QiChargeVisuals.QI_CHARGE_FOREGROUND_HORIZONTAL_RANGE);
        double vertical = QiChargeVisuals.QI_CHARGE_FOREGROUND_VERTICAL_MIN
                + RANDOM.nextDouble() * QiChargeVisuals.QI_CHARGE_FOREGROUND_VERTICAL_RANGE;

        Vec3 spawn = position
                .add(look.scale(distance))
                .add(right.scale(side))
                .add(up.scale(vertical));

        double drift = randomSigned(QiChargeVisuals.QI_CHARGE_FOREGROUND_HORIZONTAL_DRIFT);
        Vec3 speed = right.scale(drift).add(up.scale(QiChargeVisuals.QI_CHARGE_FOREGROUND_UPWARD_SPEED));

        level.addParticle(QiChargeVisuals.QI_DUST_PARTICLE, spawn.x, spawn.y, spawn.z, speed.x, speed.y, speed.z);
    }

    private static void resetForegroundCooldown() {
        foregroundCooldown = nextForegroundCooldown(RANDOM);
    }

    private static double randomSigned(double range) {
        return (RANDOM.nextDouble() * 2.0 - 1.0) * range;
    }
}
