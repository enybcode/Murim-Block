package com.murimblock.client;

import com.lowdragmc.photon.client.light.DynamicLight;
import com.lowdragmc.photon.client.light.PhotonLights;
import com.mojang.blaze3d.systems.RenderSystem;
import com.murimblock.Murimblock;
import com.murimblock.mob.SonOfTheDemonEntity;
import com.murimblock.mob.majin.MajinFist;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * The One as the players near the Son of the Demon see it (MajinFist): the sky and the light turned gold round the
 * screen while it lasts, its title as it begins ("THE ONE", in gold, small), and a great warm Photon light shining from
 * him over the arena.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class TheOneClient {
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/gui/cinematic/glow.png");
    @Nullable private static DynamicLight light;

    private TheOneClient() { }

    /** The Son of the Demon in The One nearest the player (within 96 blocks), or null. */
    @Nullable private static SonOfTheDemonEntity one(Minecraft mc) {
        if (mc.level == null || mc.player == null) return null;
        SonOfTheDemonEntity best = null;
        for (SonOfTheDemonEntity s : mc.level.getEntitiesOfClass(SonOfTheDemonEntity.class, mc.player.getBoundingBox().inflate(96)))
            if (s.theOneTicks() > 0 && (best == null || s.distanceToSqr(mc.player) < best.distanceToSqr(mc.player))) best = s;
        return best;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        SonOfTheDemonEntity s = one(mc);
        if (s == null || !PhotonLights.isEnabled()) {
            if (light != null) { PhotonLights.remove(light); light = null; }
            return;
        }
        if (light == null) light = PhotonLights.add(new DynamicLight().point().color(1.0F, 0.78F, 0.36F));
        float k = Mth.clamp(s.theOneTicks() / (float) MajinFist.ONE_RISE, 0, 1);
        light.at(s.getX(), s.getY() + 9, s.getZ()).intensity(1 + 3 * k).range(10 + 30 * k).sourceRadius(2)
                .volumetric(PhotonLights.isVolumetricEnabled() ? 0.4F * k : 0);
    }

    @SubscribeEvent
    public static void gui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        SonOfTheDemonEntity s = one(mc);
        if (s == null || TransformationCinematic.active()) return;
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight(), t = s.theOneTicks();
        float in = Mth.clamp(t / (float) MajinFist.ONE_RISE, 0, 1);
        float out = t > MajinFist.ONE_LENGTH + MajinFist.ONE_FALL ? 0 : 1;
        // The gold round the edges of the view: the glow, drawn large and inverted by its placing at the four corners.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 0.72F, 0.25F, 0.35F * in * out);
        int r = Math.max(w, h);
        g.blit(GLOW, -r / 2, -r / 2, r, r, 0, 0, 128, 128, 128, 128);
        g.blit(GLOW, w - r / 2, -r / 2, r, r, 0, 0, 128, 128, 128, 128);
        g.blit(GLOW, -r / 2, h - r / 2, r, r, 0, 0, 128, 128, 128, 128);
        g.blit(GLOW, w - r / 2, h - r / 2, r, r, 0, 0, 128, 128, 128, 128);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.fill(0, 0, w, h, (Math.round(28 * in * out) << 24) | 0xFFC040);
        // Its title, as it begins.
        int from = MajinFist.ONE_RISE - 25, to = MajinFist.ONE_RISE + 50;
        if (t >= from && t <= to) {
            float a = Mth.clamp((t - from) / 8F, 0, 1) * Mth.clamp((to - t) / 10F, 0, 1);
            Component title = Component.translatable("majin.murimblock.the_one").withStyle(ChatFormatting.BOLD);
            var pose = g.pose();
            pose.pushPose();
            pose.translate(w / 2F, h * 0.28F, 0);
            pose.scale(2.4F, 2.4F, 1);
            g.drawCenteredString(mc.font, title, 0, 0, (Math.max(4, Math.round(a * 255)) << 24) | 0xFFD45A);
            pose.popPose();
        }
        RenderSystem.disableBlend();
    }
}
