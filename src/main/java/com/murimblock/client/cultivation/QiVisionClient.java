package com.murimblock.client.cultivation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.murimblock.Murimblock;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.MeditationService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Qi vision (docs/cultivation/refonte-cultivation.md, a mock-up for Enzo, 18/10/2026): while Qi Sense is open (G held),
 * the world turns to an ink-wash painting (shaders/post/qi_vision: grey-blue ink on pale paper, edges in ink, the sky
 * left as paper) and only the Qi keeps its colour: every living thing within reach shows its aura, the meditation's
 * flames (Enzo, 18/10/2026: "comme quand ils chargent le Qi", AuraClient.volume), in its own colour (a player the art
 * they mastered best, as big as their Qi; the hostile red, beasts green, the rest pale blue); from Qi Liquidation
 * on, currents of Qi run along the ground toward the veins of Qi ({@link #veins}).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QiVisionClient {
    static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "shaders/post/qi_vision.json");
    /** Where the veins of Qi are, that the currents run to (the mock-up's: set by its tour). */
    public static final List<Vec3> veins = new ArrayList<>();
    private static boolean effectOn;

    private QiVisionClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean open = mc.player != null && QiSenseClient.open();
        if (open && !effectOn && mc.gameRenderer.currentEffect() == null) {
            mc.gameRenderer.loadEffect(EFFECT);
            effectOn = true;
        } else if (!open && effectOn) {
            mc.gameRenderer.shutdownEffect();
            effectOn = false;
        }
        // The world's outlines reach as far as the sense, by realm.
        if (effectOn && mc.gameRenderer.currentEffect() != null)
            mc.gameRenderer.currentEffect().setUniform("SeeFar", (float) com.murimblock.cultivation.CultivationRules.farSenseRange(CultivationService.getRealm(mc.player)));
        if (open && !mc.isPaused()) {
            auras(mc);
            if (CultivationService.getRealm(mc.player).ordinal() >= CultivationRealm.QI_LIQUIDATION.ordinal()) currents(mc);
        }
    }

    /**
     * The world's depth, kept for the outlines before Minecraft clears it to draw the hand: copied into the effect's own
     * target (depthcopy), which its pass reads.
     */
    @SubscribeEvent
    public static void keepDepth(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        if (!effectOn || event.getStage() != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        Minecraft mc = Minecraft.getInstance();
        var effect = mc.gameRenderer.currentEffect();
        if (effect == null) return;
        var copy = effect.getTempTarget("depthcopy");
        if (copy == null) return;
        copy.copyDepthFrom(mc.getMainRenderTarget());
        mc.getMainRenderTarget().bindWrite(false);
    }

    /** How many of the nearest living show their aura at once. */
    static final int AURAS = 16;

    /**
     * The aura of every living thing within reach (the nearest {@link #AURAS}): the meditation's flames, in its colour
     * made pure (so the ink wash leaves it its colour); a player's as big as their Qi, a creature's as big as its body.
     */
    static void auras(Minecraft mc) {
        RandomSource random = mc.level.random;
        CultivationRealm own = CultivationService.getRealm(mc.player);
        double reach = com.murimblock.cultivation.CultivationRules.senseRange(own) + 8, far = com.murimblock.cultivation.CultivationRules.farSenseRange(own);
        java.util.Set<LivingEntity> shown = new java.util.HashSet<>();
        mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(reach)).stream()
                .filter(e -> !e.isInvisible() && !(e == mc.player && mc.options.getCameraType().isFirstPerson()))
                .sorted((a, b) -> Double.compare(a.distanceToSqr(mc.player), b.distanceToSqr(mc.player)))
                .limit(AURAS)
                .forEach(shown::add);
        // The powerful, from far: every aura the server let us read beyond our reach.
        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(far)))
            if (QiSenseClient.reading(e.getId()).isPresent()) shown.add(e);
        for (LivingEntity e : shown) {
            float scale = e instanceof AbstractClientPlayer p ? AuraClient.qiScale(MeditationService.aura(p).qi()) : creatureScale(mc, e);
            float[] c = shown(e);
            // Its vital points lit (a perfect parry broke its guard): its Qi scattered, a mere wisp, so that they show.
            if (VitalPointsClient.on(e)) scale *= 0.35F;
            if (e.distanceToSqr(mc.player) <= reach * reach) AuraClient.volume(mc.level, random, e.position(), 0.6F, c, scale);
            if (scale >= COLUMN_FROM && e.distanceToSqr(mc.player) <= reach * reach) vortex(mc, random, e, c, scale);
        }
    }

    /** From this aura's size on, it rises as a column (the spiritual pressure of Bleach). */
    static final float COLUMN_FROM = 1.3F;

    /** The column's width (radius, blocks) and height for an aura's size: a realm above, about 5.8 and 40; far above, about 9.5 and 72. */
    static double columnRadius(float scale) {
        return (scale - 1.1) * 5 + 1;
    }

    static double columnHeight(float scale) {
        return (scale - 1.1) * 40 + 4;
    }

    /**
     * At the column's foot: the air drawn in, spiralling into it from round about and climbing; and every second and a
     * half, a shock wave blown out along the ground.
     */
    static void vortex(Minecraft mc, RandomSource random, LivingEntity e, float[] c, float scale) {
        double r = columnRadius(scale);
        for (int i = 0; i < 2; i++) {
            double a = random.nextDouble() * Math.PI * 2, d = r * (1.2 + random.nextDouble() * 0.8);
            double x = e.getX() + Math.cos(a) * d, z = e.getZ() + Math.sin(a) * d;
            // Inward and round, a little up.
            double in = 0.18 + random.nextDouble() * 0.1, round = 0.22;
            AuraParticle p = AuraClient.spark(mc.level, x, e.getY() + 0.2 + random.nextDouble() * 1.5, z,
                    -Math.cos(a) * in - Math.sin(a) * round, 0.05 + random.nextDouble() * 0.05, -Math.sin(a) * in + Math.cos(a) * round, c, false);
            if (p != null) p.big(0.1F + random.nextFloat() * 0.1F, 18 + random.nextInt(10));
        }
        if ((mc.level.getGameTime() + e.getId()) % 30 == 0) AuraClient.ring(mc.level, e.position().add(0, 0.15, 0), r * 0.8, 48, c, 0.45 + 0.1 * scale);
    }

    /** How many columns the vision's shader draws at most (the strongest auras read). */
    static final int COLUMNS = 3;

    /**
     * The columns of the powerful (Enzo, 18/10/2026, after Bleach's spiritual pressure: "large", then "not a texture,
     * not particles"): drawn by the vision's own shader as wide shafts of light rising from the body to the sky, their
     * edges flickering like flames, a brighter core; seen through anything. Each frame, the strongest auras read are put
     * on the screen (their foot, their top, their width) and handed to the shader.
     */
    @SubscribeEvent
    public static void columns(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !effectOn) return;
        Minecraft mc = Minecraft.getInstance();
        var effect = mc.gameRenderer.currentEffect();
        if (mc.level == null || mc.player == null || effect == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        effect.setUniform("Time", (float) ((mc.level.getGameTime() % 24000) + partial) / 20F);
        Vec3 camera = event.getCamera().getPosition();
        org.joml.Matrix4f toScreen = new org.joml.Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
        org.joml.Vector3f right = event.getCamera().getLeftVector().negate(new org.joml.Vector3f());
        double far = com.murimblock.cultivation.CultivationRules.farSenseRange(CultivationService.getRealm(mc.player));
        List<float[]> shafts = new ArrayList<>();
        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(far))) {
            if (QiSenseClient.reading(e.getId()).isEmpty() || e.isInvisible()) continue;
            float scale = e instanceof AbstractClientPlayer pl ? AuraClient.qiScale(MeditationService.aura(pl).qi()) : creatureScale(mc, e);
            if (scale < COLUMN_FROM) continue;
            Vec3 foot = e.getPosition(partial).subtract(camera);
            float[] a = screen(toScreen, foot), b = screen(toScreen, foot.add(0, columnHeight(scale), 0));
            float[] side = screen(toScreen, foot.add(right.x() * columnRadius(scale), 0, right.z() * columnRadius(scale)));
            if (a == null || b == null || side == null) continue;
            float[] c = shown(e);
            shafts.add(new float[]{a[0], a[1], b[1], Math.max(0.004F, Math.abs(side[0] - a[0])), c[0], c[1], c[2], scale});
        }
        shafts.sort((x, y) -> Float.compare(y[7], x[7]));
        effect.setUniform("Columns", Math.min(COLUMNS, shafts.size()));
        for (int i = 0; i < Math.min(COLUMNS, shafts.size()); i++) {
            float[] f = shafts.get(i);
            String[] names = {"X", "Foot", "Top", "Width", "R", "G", "B"};
            for (int k = 0; k < names.length; k++) effect.setUniform("Col" + i + names[k], f[k]);
        }
    }

    /** A point (relative to the camera) on the screen, 0 to 1 across and up; null behind the camera. */
    private static float[] screen(org.joml.Matrix4f toScreen, Vec3 at) {
        org.joml.Vector4f v = toScreen.transform(new org.joml.Vector4f((float) at.x, (float) at.y, (float) at.z, 1));
        if (v.w <= 0.05F) return null;
        return new float[]{v.x / v.w * 0.5F + 0.5F, v.y / v.w * 0.5F + 0.5F};
    }

    /**
     * A creature's aura: as big as its body, or as its Qi when Qi Sense reads it (its realm's Qi Max; one realm above
     * one's own, read only as stronger; far above, immense).
     */
    static float creatureScale(Minecraft mc, LivingEntity e) {
        float body = Math.max(0.45F, Math.min(1.6F, e.getBbHeight() / 2.2F + e.getBbWidth() * 0.2F));
        var reading = QiSenseClient.reading(e.getId());
        if (reading.isEmpty()) return body;
        var r = reading.get();
        CultivationRealm own = CultivationService.getRealm(mc.player);
        float qi = switch (r.kind()) {
            case 0, 1 -> AuraClient.qiScale((float) com.murimblock.cultivation.CultivationRules.floorOf(
                    new com.murimblock.cultivation.CultivationData(CultivationRealm.values()[Math.max(0, r.realm())], com.murimblock.cultivation.CultivationStage.EARLY)));
            case 2 -> AuraClient.qiScale((float) com.murimblock.cultivation.CultivationRules.floorOf(
                    new com.murimblock.cultivation.CultivationData(CultivationRealm.values()[Math.min(8, own.ordinal() + 1)], com.murimblock.cultivation.CultivationStage.PEAK))) * 1.9F;
            default -> 2.8F;
        };
        return Math.max(body, qi);
    }

    /** The currents: motes of Qi rising out of the ground round the viewer and flowing, winding, toward the nearest vein. */
    static void currents(Minecraft mc) {
        if (veins.isEmpty() || mc.level == null) return;
        RandomSource random = mc.level.random;
        Vec3 me = mc.player.position();
        Vec3 vein = veins.stream().min((x, y) -> Double.compare(x.distanceToSqr(me), y.distanceToSqr(me))).get();
        float[] c = {0.0F, 1.0F, 0.85F};
        for (int i = 0; i < 6; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = 4 + random.nextDouble() * 20;
            Vec3 from = new Vec3(me.x + Math.cos(a) * r, me.y + 0.2, me.z + Math.sin(a) * r);
            Vec3 to = vein.subtract(from);
            if (to.length() < 1) continue;
            Vec3 dir = to.normalize(), side = new Vec3(-dir.z, 0, dir.x);
            double wind = Math.sin(from.x * 0.3 + from.z * 0.2 + mc.level.getGameTime() * 0.05) * 0.06;
            AuraParticle p = AuraClient.spark(mc.level, from.x, mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(from.x), (int) Math.floor(from.z)) + 0.15, from.z,
                    dir.x * 0.25 + side.x * wind, 0.004, dir.z * 0.25 + side.z * wind, c, false);
            if (p != null) p.big(0.16F, 40 + random.nextInt(30));
        }
        // The vein itself: Qi welling up out of the ground.
        for (int i = 0; i < 2; i++) {
            AuraParticle p = AuraClient.spark(mc.level, vein.x + (random.nextDouble() - 0.5) * 1.2, vein.y + 0.1, vein.z + (random.nextDouble() - 0.5) * 1.2,
                    0, 0.08 + random.nextDouble() * 0.06, 0, c, false);
            if (p != null) p.big(0.22F, 30);
        }
    }

    /** A living thing's Qi: a player's art; the hostile red; beasts green; the rest pale blue. */
    static float[] colour(LivingEntity e) {
        if (e instanceof AbstractClientPlayer p) {
            var art = MeditationService.aura(p).auraArt();
            return art.isPresent() ? AuraClient.colour(art) : new float[]{0.35F, 0.75F, 1.0F};
        }
        if (e instanceof Enemy) return new float[]{1.0F, 0.18F, 0.14F};
        if (e instanceof Animal) return new float[]{0.3F, 1.0F, 0.3F};
        return new float[]{0.35F, 0.75F, 1.0F};
    }

    /** The colour made pure (its grey taken out), so the ink wash leaves it its colour. */
    /** The colour an aura is seen in: a player's exactly as when meditating (the art's), a creature's made vivid. */
    static float[] shown(LivingEntity e) {
        return e instanceof AbstractClientPlayer ? colour(e) : vivid(colour(e));
    }

    static float[] vivid(float[] c) {
        float mn = Math.min(c[0], Math.min(c[1], c[2])), mx = Math.max(c[0], Math.max(c[1], c[2]));
        if (mx - mn < 1e-3) return new float[]{0.2F, 0.7F, 1.0F};
        return new float[]{(c[0] - mn) / (mx - mn), (c[1] - mn) / (mx - mn), (c[2] - mn) / (mx - mn)};
    }
}
