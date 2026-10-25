package com.murimblock.client.innerworld;

import com.mojang.blaze3d.systems.RenderSystem;
import com.murimblock.Murimblock;
import com.murimblock.client.cultivation.ClientCultivation;
import com.murimblock.cultivation.QiParticleOptions;
import com.murimblock.innerworld.MortalTrial;
import com.murimblock.registry.MurimParticles;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * Going into the mortal trial (Enzo, 25 Oct 2026). The dive: the Qi around the seated player is drawn into the
 * dantian (seen by everyone near), and for the player the screen closes in from the edges to black over a slowing
 * heartbeat. The arrival: a bell, the screen opens on the graveyard, the player is seen from behind getting up from
 * the ground, then the title SURVIVE DEATH.
 */
public final class TrialEntry {
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "trial_entry");
    private static final ResourceLocation VIGNETTE =
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/gui/breath_vignette.png");
    public static final ResourceLocation RISE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "trial_rise");
    /** Heartbeats of the dive, each a little further from the last. */
    private static final int[] HEARTBEATS = {0, 11, 23, 36, 50};
    /** The last ticks of the dive are fully black. */
    private static final int BLACK_FROM = MortalTrial.DIVE_TICKS - 12;
    // The arrival, in ticks.
    // The screen opens on the player still lying; the getting up starts once the graveyard is in view.
    private static final int FADE_IN_START = 4;
    private static final int FADE_IN_END = 20;
    private static final int LIE_DOWN = 2;
    /** The push-up begins (the animation holds its lying pose 0.9 s): earth is thrown up. */
    private static final int RISE_START = 20;
    private static final int RISE_END = 66;
    private static final int TITLE = 66;
    private static final int ARRIVAL_END = 74;
    private static final double DANTIAN = 0.55;

    /** Players diving (ours included), by entity id, with the ticks since the dive began. */
    private static final Map<Integer, Integer> DIVING = new HashMap<>();
    private static int ownDive = -1;
    private static int arrival = -1;
    private static CameraType cameraBefore;

    private TrialEntry() {
    }

    /** Ticks into our own dive, or -1. */
    public static int ownDive() {
        return ownDive;
    }

    /** Ticks into our arrival on the trial island, or -1. */
    public static int arrival() {
        return arrival;
    }

    public static void receive(int entityId, int phase) {
        Minecraft mc = Minecraft.getInstance();
        boolean own = mc.player != null && mc.player.getId() == entityId;
        switch (phase) {
            case MortalTrial.Dive.START -> {
                DIVING.put(entityId, 0);
                if (own) {
                    ownDive = 0;
                }
            }
            case MortalTrial.Dive.CANCEL -> {
                DIVING.remove(entityId);
                if (own) {
                    ownDive = -1;
                }
            }
            default -> {
                DIVING.clear();
                ownDive = -1;
                arrival = 0;
            }
        }
    }

    private static void tick(Minecraft mc) {
        Iterator<Map.Entry<Integer, Integer>> it = DIVING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> e = it.next();
            Entity entity = mc.level == null ? null : mc.level.getEntity(e.getKey());
            if (entity == null || e.getValue() > MortalTrial.DIVE_TICKS + 20) {
                it.remove();
                continue;
            }
            drawIn(entity, e.getValue());
            e.setValue(e.getValue() + 1);
        }
        if (ownDive >= 0) {
            for (int beat : HEARTBEATS) {
                if (ownDive == beat) {
                    sound(SoundEvents.WARDEN_HEARTBEAT, 0.9f - beat * 0.004f, 1.2f);
                }
            }
            ownDive++;
            if (ownDive > MortalTrial.DIVE_TICKS + 20) {
                ownDive = -1;
            }
        }
        if (arrival >= 0) {
            arrive(mc);
        }
    }

    /** Qi specks pulled in from all around toward the dantian, faster as the dive goes on. */
    private static void drawIn(Entity entity, int age) {
        RandomSource random = entity.level().random;
        float k = Math.min(1, age / (float) MortalTrial.DIVE_TICKS);
        int count = 1 + (int) (k * 4);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double r = 2.2 + random.nextDouble() * 1.2;
            double y = random.nextDouble() * 2.2 - 0.6;
            double speed = 0.12 + 0.1 * k;
            entity.level().addParticle(new QiParticleOptions(MurimParticles.QI_SPARK.get(), 1),
                    entity.getX() + Math.cos(angle) * r, entity.getY() + DANTIAN + y, entity.getZ() + Math.sin(angle) * r,
                    -Math.cos(angle) * speed, -y * speed / r, -Math.sin(angle) * speed);
        }
    }

    private static void arrive(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        if (arrival == 0) {
            sound(SoundEvents.BELL_BLOCK, 0.5f, 1);
            mc.level.addParticle(ParticleTypes.SOUL, mc.player.getX(), mc.player.getY() + 0.3, mc.player.getZ(), 0, 0.05, 0);
        }
        if (arrival == LIE_DOWN) {
            // Seen from behind, lying face down (the animation's first pose, held), then back to the player's eyes.
            if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
                cameraBefore = CameraType.FIRST_PERSON;
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            if (PlayerAnimationAccess.getPlayerAnimationLayer(mc.player, ClientCultivation.LAYER)
                    instanceof PlayerAnimationController controller) {
                controller.triggerAnimation(RISE);
            }
        }
        if (arrival == RISE_START) {
            RandomSource random = mc.player.getRandom();
            for (int i = 0; i < 30; i++) {
                mc.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.defaultBlockState()),
                        mc.player.getX() + (random.nextDouble() - 0.5) * 1.6, mc.player.getY() + 0.1,
                        mc.player.getZ() + (random.nextDouble() - 0.5) * 1.6, 0, 0.1, 0);
            }
        }
        if (arrival == RISE_END && cameraBefore != null) {
            if (mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
                mc.options.setCameraType(cameraBefore);
            }
            cameraBefore = null;
        }
        if (arrival == TITLE) {
            mc.gui.setTimes(10, 50, 20);
            mc.gui.setSubtitle(Component.translatable("title.murimblock.trial.subtitle").withStyle(ChatFormatting.GRAY));
            mc.gui.setTitle(Component.translatable("title.murimblock.trial").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        }
        arrival = arrival >= ARRIVAL_END ? -1 : arrival + 1;
    }

    private static void sound(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    /** The dark closing in during the dive (edges first, then all black) and lifting on arrival. */
    private static void render(GuiGraphics g, DeltaTracker delta) {
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float edges = 0;
        float black = 0;
        if (ownDive >= 0) {
            float t = ownDive + partial;
            edges = Math.min(1, t / BLACK_FROM) * 1.4f;
            black = smooth((t - BLACK_FROM) / 8f);
        } else if (arrival >= 0) {
            float t = arrival + partial;
            black = t < FADE_IN_START ? 1 : 1 - smooth((t - FADE_IN_START) / (FADE_IN_END - FADE_IN_START));
            edges = black * 1.4f;
        }
        if (edges > 0.01f) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.setColor(0, 0, 0, Math.min(1, edges));
            g.blit(VIGNETTE, 0, 0, g.guiWidth(), g.guiHeight(), 0, 0, 128, 128, 128, 128);
            g.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
        }
        if (black > 0.01f) {
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), (Math.round(Math.min(1, black) * 255) << 24));
        }
    }

    private static float smooth(float k) {
        k = Mth.clamp(k, 0, 1);
        return k * k * (3 - 2 * k);
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void registerLayer(RegisterGuiLayersEvent event) {
            event.registerAboveAll(LAYER, TrialEntry::render);
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                DIVING.clear();
                ownDive = -1;
                arrival = -1;
                return;
            }
            if (!mc.isPaused()) {
                tick(mc);
            }
        }
    }
}
