package com.murimblock.client.innerworld;

import com.murimblock.Murimblock;
import com.murimblock.client.gui.SystemLayout.Rect;
import com.murimblock.client.gui.SystemTheme;
import com.murimblock.innerworld.InnerWorld;
import com.murimblock.innerworld.MortalTrial;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Client side of the mortal trial: the wave counter (a System window at the top center), and the graveyard's air,
 * a close blue-grey fog and ash drifting down.
 */
public final class TrialHud {
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "trial");
    private static final int WIDTH = 120;
    private static final float FOG_NEAR = 6;
    private static final float FOG_FAR = 56;
    private static final float[] FOG_COLOR = {0.07f, 0.08f, 0.12f};

    private static MortalTrial.Status status = MortalTrial.Status.NONE;

    private TrialHud() {
    }

    public static void receive(MortalTrial.Status received) {
        status = received;
    }

    private static boolean inTrial() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && InnerWorld.isTrial(mc.level);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!status.active() || mc.options.hideGui || !inTrial()) {
            return;
        }
        Font font = mc.font;
        boolean calm = status.calm() > 0;
        int shown = calm ? status.wave() + 2 : status.wave() + 1;
        Component main = Component.translatable("hud.murimblock.trial.wave", shown, status.waves());
        Component sub = calm
                ? Component.translatable("hud.murimblock.trial.next", status.calm())
                : Component.translatable("hud.murimblock.trial.left", status.left(), status.total());
        // Top center: the top right belongs to toasts.
        Rect r = new Rect((g.guiWidth() - WIDTH) / 2, 14, WIDTH, 44);
        SystemTheme.window(g, r);
        SystemTheme.titleTag(g, font, Component.translatable("hud.murimblock.trial.title"), r.x() + WIDTH / 2, r.y());
        SystemTheme.glowText(g, font, main, r.x() + (WIDTH - font.width(main)) / 2, r.y() + 11, SystemTheme.TEXT);
        float done = calm || status.total() == 0 ? 1 : 1 - (float) status.left() / status.total();
        SystemTheme.bar(g, r.x() + 10, r.y() + 23, WIDTH - 20, 4, done, calm ? SystemTheme.CYAN_SOFT : SystemTheme.CYAN);
        g.drawString(font, sub, r.x() + (WIDTH - font.width(sub)) / 2, r.y() + 31, SystemTheme.TEXT_MUTED, false);
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void registerLayer(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CROSSHAIR, LAYER, TrialHud::render);
        }

        @SubscribeEvent
        public static void onFog(ViewportEvent.RenderFog event) {
            if (inTrial()) {
                event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), FOG_NEAR));
                event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), FOG_FAR));
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onFogColor(ViewportEvent.ComputeFogColor event) {
            if (inTrial()) {
                event.setRed(FOG_COLOR[0]);
                event.setGreen(FOG_COLOR[1]);
                event.setBlue(FOG_COLOR[2]);
            }
        }

        /** Ash drifting down around the player. */
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.isPaused() || mc.player == null || !inTrial()) {
                if (mc.level == null) {
                    status = MortalTrial.Status.NONE;
                }
                return;
            }
            RandomSource random = mc.player.getRandom();
            for (int i = 0; i < 4; i++) {
                mc.level.addParticle(random.nextInt(3) == 0 ? ParticleTypes.ASH : ParticleTypes.WHITE_ASH,
                        mc.player.getX() + (random.nextDouble() - 0.5) * 32, mc.player.getY() + random.nextDouble() * 14,
                        mc.player.getZ() + (random.nextDouble() - 0.5) * 32, 0, -0.02, 0);
            }
        }
    }
}
