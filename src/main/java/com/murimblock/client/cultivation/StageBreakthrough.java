package com.murimblock.client.cultivation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.client.gui.SystemLayout.Rect;
import com.murimblock.client.gui.SystemTheme;
import com.murimblock.cultivation.CultivationRealms;
import com.murimblock.cultivation.CultivationRules;
import com.murimblock.cultivation.QiParticleOptions;
import com.murimblock.registry.MurimParticles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * A stage breakthrough (mockup "Spiral Breakthrough", Enzo 25 Oct 2026), seen by everyone near: Qi gathers in a ring
 * at the feet, two strands spiral up around the body to above the head, the body straightens like a deep breath,
 * then the spiral bursts into a shower of specks with a chime one pentatonic step higher per stage. The player
 * themself also gets the BREAKTHROUGH System window, and the camera steps behind them for the moment.
 */
public final class StageBreakthrough {
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "breakthrough");
    // Timeline, in ticks.
    private static final int SPIRAL = 12;
    private static final int INHALE = 24;
    private static final int BURST = 36;
    private static final int WINDOW = 38;
    private static final int WINDOW_END = 88;
    private static final int RELAX = 24;
    private static final int CAMERA_END = 72;
    private static final int END = 90;
    /** The spiral climbs from the feet to here; the burst happens here. */
    private static final double TOP = 2.1;
    private static final double LIFT = 0.12;
    /** Semitones of the chime for stages 1-9, a pentatonic scale. */
    private static final int[] SCALE = {0, 2, 4, 7, 9, 12, 14, 16, 19};

    private static final class Effect {
        final int entityId;
        final int realm;
        final int stage;
        int age;

        Effect(int entityId, int realm, int stage) {
            this.entityId = entityId;
            this.realm = realm;
            this.stage = stage;
        }
    }

    private static final List<Effect> EFFECTS = new ArrayList<>();
    /** Set while this mod moved the camera behind the player, so it puts it back. */
    private static CameraType cameraBefore;

    private StageBreakthrough() {
    }

    public static void start(int entityId, int realm, int stage) {
        Minecraft mc = Minecraft.getInstance();
        EFFECTS.removeIf(e -> e.entityId == entityId);
        EFFECTS.add(new Effect(entityId, realm, stage));
        if (mc.player != null && mc.player.getId() == entityId) {
            // The System covers the scene: close it, and step behind the player to see the spiral.
            mc.setScreen(null);
            if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
                cameraBefore = CameraType.FIRST_PERSON;
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        }
    }

    /** Whether the local player's own breakthrough is playing (the breathing lane pauses meanwhile). */
    public static boolean playingOwn() {
        return own(Minecraft.getInstance()) != null;
    }

    /** Ticks since this entity's breakthrough began, or -1 if none is playing. */
    public static int age(int entityId) {
        for (Effect e : EFFECTS) {
            if (e.entityId == entityId) {
                return e.age;
            }
        }
        return -1;
    }

    private static void tick(Minecraft mc) {
        Iterator<Effect> it = EFFECTS.iterator();
        while (it.hasNext()) {
            Effect e = it.next();
            Entity entity = mc.level == null ? null : mc.level.getEntity(e.entityId);
            if (entity == null || e.age > END) {
                it.remove();
                continue;
            }
            play(mc.level, entity, e);
            e.age++;
        }
        Effect own = own(mc);
        if (cameraBefore != null && (own == null || own.age > CAMERA_END)) {
            if (mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
                mc.options.setCameraType(cameraBefore);
            }
            cameraBefore = null;
        }
    }

    private static void play(Level level, Entity entity, Effect e) {
        RandomSource random = level.random;
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        if (e.age < SPIRAL + 4) {
            // Gathering: specks slide in from the ground to a ring at the feet.
            for (int i = 0; i < 3; i++) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double r = 2.6 + random.nextDouble();
                level.addParticle(new QiParticleOptions(MurimParticles.QI_SPARK.get(), 1),
                        x + Math.cos(angle) * r, y + 0.05, z + Math.sin(angle) * r,
                        -Math.cos(angle) * 0.15, 0, -Math.sin(angle) * 0.15);
            }
        } else if (e.age < BURST) {
            // Spiral: two strands, three points a tick for a smooth trail, narrowing as they climb.
            for (int sub = 0; sub < 3; sub++) {
                float k = (e.age - SPIRAL + sub / 3f) / (BURST - SPIRAL);
                double height = smooth(k) * TOP;
                double radius = 1.3 - 0.8 * k;
                for (int strand = 0; strand < 2; strand++) {
                    double angle = k * Math.PI * 7 + strand * Math.PI;
                    double px = x + Math.cos(angle) * radius;
                    double pz = z + Math.sin(angle) * radius;
                    level.addParticle(new QiParticleOptions(MurimParticles.QI_SPARK.get(), 1), px, y + 0.05 + height, pz, 0, 0.005, 0);
                    // A puff of Qi smoke at the head of each strand thickens the band.
                    if (sub == 0) {
                        level.addParticle(new QiParticleOptions(MurimParticles.QI.get(), 0.5f), px, y + 0.05 + height, pz, 0, 0, 0);
                    }
                }
            }
        }
        if (e.age == SPIRAL) {
            sound(level, x, y, z, SoundEvents.BEACON_ACTIVATE, 0.45f, 1.6f);
        }
        if (e.age == BURST) {
            for (int i = 0; i < 110; i++) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double out = 0.12 + random.nextDouble() * 0.22;
                level.addParticle(new QiParticleOptions(MurimParticles.QI_FALL.get(), 1),
                        x, y + TOP, z, Math.cos(angle) * out, 0.05 + random.nextDouble() * 0.15, Math.sin(angle) * out);
            }
            if (entity instanceof Player p) {
                QiAura.burst(p);
            }
            float pitch = 0.5f * (float) Math.pow(2, SCALE[Mth.clamp(e.stage, 1, SCALE.length) - 1] / 12.0);
            sound(level, x, y, z, SoundEvents.NOTE_BLOCK_CHIME.value(), 1f, pitch);
            sound(level, x, y, z, SoundEvents.NOTE_BLOCK_BELL.value(), 0.5f, pitch);
            sound(level, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1f);
        }
    }

    private static void sound(Level level, double x, double y, double z, SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(x, y + 1, z, sound, SoundSource.PLAYERS, volume, pitch, false);
    }

    private static float smooth(float k) {
        k = Mth.clamp(k, 0, 1);
        return k * k * (3 - 2 * k);
    }

    private static Effect own(Minecraft mc) {
        if (mc.player == null) {
            return null;
        }
        for (Effect e : EFFECTS) {
            if (e.entityId == mc.player.getId()) {
                return e;
            }
        }
        return null;
    }

    /** How far the body is lifted, 0-1: rises as the spiral climbs, settles after the burst. */
    private static float lift(Effect e, float partialTick) {
        float t = e.age + partialTick;
        if (t < INHALE) {
            return 0;
        }
        if (t < BURST + 2) {
            return smooth((t - INHALE) / (BURST + 2 - INHALE));
        }
        return 1 - smooth((t - BURST - 2) / RELAX);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Effect e = own(mc);
        if (e == null || mc.options.hideGui) {
            return;
        }
        float t = e.age + delta.getGameTimeDeltaPartialTick(false);
        if (t < WINDOW || t >= WINDOW_END) {
            return;
        }
        Font font = mc.font;
        Component realm = Component.translatable("murimblock.cultivation_realm." + CultivationRealms.realmKey(e.realm));
        Component main = Component.translatable("hud.murimblock.breakthrough.stage", realm, e.stage);
        int bonus = (int) Math.round(CultivationRules.STAGE_MAX_QI_BONUS * 100);
        Component sub = e.stage < CultivationRules.STAGES
                ? Component.translatable("hud.murimblock.breakthrough.next", bonus, CultivationRules.gauge(e.realm, e.stage))
                : Component.translatable("hud.murimblock.breakthrough.peak", bonus);
        int w = Math.max(font.width(main), font.width(sub)) + 40;
        int h = 40;
        // Slides down from above the screen, and back up at the end.
        float in = smooth((t - WINDOW) / 5);
        float out = smooth((WINDOW_END - t) / 5);
        int y = 22 - Math.round((1 - Math.min(in, out)) * (h + 30));
        Rect r = new Rect((g.guiWidth() - w) / 2, y, w, h);
        SystemTheme.window(g, r);
        SystemTheme.titleTag(g, font, Component.translatable("hud.murimblock.breakthrough.title"), r.x() + w / 2, r.y());
        SystemTheme.glowText(g, font, main, r.x() + (w - font.width(main)) / 2, r.y() + 13, SystemTheme.TEXT);
        g.drawString(font, sub, r.x() + (w - font.width(sub)) / 2, r.y() + 26, SystemTheme.TEXT_MUTED, false);
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void registerLayer(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CROSSHAIR, LAYER, StageBreakthrough::render);
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (!mc.isPaused()) {
                tick(mc);
            }
        }

        /** The deep breath: the seated body rises a little. Last in Pre, so nothing cancels after the push. */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void beforePlayer(RenderPlayerEvent.Pre event) {
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            for (Effect e : EFFECTS) {
                if (e.entityId == event.getEntity().getId()) {
                    pose.translate(0, LIFT * lift(e, event.getPartialTick()), 0);
                }
            }
        }

        @SubscribeEvent
        public static void afterPlayer(RenderPlayerEvent.Post event) {
            event.getPoseStack().popPose();
        }
    }
}
