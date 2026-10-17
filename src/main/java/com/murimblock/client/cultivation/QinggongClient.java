package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.Qinggong;
import com.murimblock.network.CultivationPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The leap in the air of Qinggong (cultivation/Qinggong): the jump key pressed again once off the ground, from Qi Guiding
 * on, throws the player up and along at once, and tells the server, which takes the Qi and shows the gust. One leap until
 * the feet touch the ground again.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QinggongClient {
    /** Ticks in the air before a leap is heard (so the jump's own press is not one). */
    static final int AIR_TICKS = 3;
    private static boolean wasDown, leapt;
    private static int airTicks;

    private QinggongClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        boolean down = mc.screen == null && mc.options.keyJump.isDown();
        if (player.onGround()) {
            leapt = false;
            airTicks = 0;
        } else {
            airTicks++;
            if (down && !wasDown && !leapt && airTicks >= AIR_TICKS && Qinggong.canLeap(player)) leap(player);
        }
        wasDown = down;
    }

    /** Up, and on along the way one goes (or faces, standing still), at once; the server is told. */
    public static void leap(LocalPlayer player) {
        Vec3 move = player.getDeltaMovement();
        Vec3 along = new Vec3(move.x, 0, move.z);
        if (along.lengthSqr() < 1e-4) along = Vec3.directionFromRotation(0, player.getYRot());
        along = along.normalize().scale(Qinggong.LEAP_ALONG);
        player.setDeltaMovement(move.x * 0.8 + along.x, Qinggong.LEAP_UP, move.z * 0.8 + along.z);
        player.resetFallDistance();
        leapt = true;
        if (Minecraft.getInstance().getConnection() != null) PacketDistributor.sendToServer(new CultivationPayloads.Leap());
    }
}
