package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.network.QiChargeStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QiChargeClientHandler {
    private static boolean lastSentCharging;
    private static boolean visualCharging;

    private QiChargeClientHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean shouldCharge = player != null
                && player.isAlive()
                && !player.isSpectator()
                && minecraft.screen == null
                && MurimblockKeyMappings.CHARGE_QI.isDown();

        setVisualCharging(shouldCharge);
        if (shouldCharge != lastSentCharging && minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(new QiChargeStatePayload(shouldCharge));
            lastSentCharging = shouldCharge;
        }
    }

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        if (!visualCharging || event.getEntity() != Minecraft.getInstance().player) {
            return;
        }

        Input input = event.getInput();
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
    }

    public static boolean isVisualCharging() {
        return visualCharging;
    }

    static void resetForTests() {
        lastSentCharging = false;
        visualCharging = false;
    }

    private static void setVisualCharging(boolean charging) {
        visualCharging = charging;
    }
}
