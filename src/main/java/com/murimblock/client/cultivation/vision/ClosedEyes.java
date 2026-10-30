package com.murimblock.client.cultivation.vision;

import com.murimblock.Murimblock;
import com.murimblock.client.cultivation.ClientCultivation;
import com.murimblock.client.cultivation.StageBreakthrough;
import com.murimblock.client.innerworld.TrialEntry;
import com.murimblock.cultivation.CultivationPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The local player's eyes while meditating: they close by themselves once seated, the meditation key opens and closes
 * them, and they open for the stage breakthrough and the trial's dive. The server hears of each change, since the Calm
 * only rises with closed eyes. Closed, the world goes dark ({@link SpiritVision}); the hand and crosshair are hidden.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class ClosedEyes {
    /** The sit clip's length: the eyes close once the body is seated. */
    private static final int AUTO_CLOSE_TICKS = 24;
    private static final int FADE_TICKS = 20;

    /** Dev tours that film the world while meditating turn this off; the key still closes the eyes. */
    public static boolean autoClose = true;

    private static boolean closed;
    private static boolean autoClosed;
    private static int meditatingTicks;
    private static float amount;
    private static float amountBefore;
    /** Ticks since the dark began to fill, for the reveal wave; reset each time the eyes close again. */
    private static int revealTicks;

    private ClosedEyes() {
    }

    public static void toggle() {
        set(!closed);
    }

    /** Closes or opens the eyes, while meditating (the dev checks call it too). */
    public static void set(boolean shut) {
        if (!ClientCultivation.get().meditating() || closed == shut) {
            return;
        }
        closed = shut;
        autoClosed = true;
        PacketDistributor.sendToServer(new CultivationPayloads.Eyes(shut));
    }

    public static boolean closed() {
        return closed;
    }

    /** How shut the eyes are this frame: 0 open, 1 fully dark. */
    public static float amount(float partialTick) {
        return Mth.lerp(partialTick, amountBefore, amount);
    }

    /** Seconds since the dark began to fill. */
    public static float revealSeconds(float partialTick) {
        return (revealTicks + partialTick) / 20f;
    }

    /** The breakthrough and the trial's dive take the screen: the eyes open meanwhile. */
    private static boolean suspended() {
        return StageBreakthrough.playingOwn() || TrialEntry.ownDive() >= 0 || TrialEntry.arrival() >= 0;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) {
            return;
        }
        amountBefore = amount;
        if (!ClientCultivation.get().meditating() || mc.player == null) {
            // The server forgets the eyes when meditation ends; the dark lifts on its own.
            closed = false;
            autoClosed = false;
            meditatingTicks = 0;
        } else if (++meditatingTicks >= AUTO_CLOSE_TICKS && !autoClosed && autoClose) {
            set(true);
        }
        boolean dark = closed && !suspended();
        amount = Mth.clamp(amount + (dark ? 1f : -1f) / FADE_TICKS, 0, 1);
        revealTicks = dark ? revealTicks + 1 : 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideHand(RenderHandEvent event) {
        if (amount(event.getPartialTick()) > 0.5f) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void hideCrosshair(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)
                && amount(event.getPartialTick().getGameTimeDeltaPartialTick(false)) > 0.5f) {
            event.setCanceled(true);
        }
    }
}
