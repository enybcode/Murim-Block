package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.cultivation.inner.InnerTheme;
import com.murimblock.cultivation.inner.InnerWorldService;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Vector3f;

/**
 * How the inner world looks to its cultivator: no sun, a void whose colour is their Qi (grey and stormy when the reserve
 * is low, the sky of their main style's world when it is full: InnerTheme), a soft mist far off, motes of Qi drifting,
 * and each world's own air (clouds, snow, embers, stars, lightning on the Thunder Saber's rods, the Golden Bell's bell);
 * and a red haze after failed great breakthroughs (an unstable foundation).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class InnerWorldClient {
    private InnerWorldClient() {
    }

    @SubscribeEvent
    public static void onRegisterEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "inner_world"), new Effects());
    }

    /** The player's world here. */
    static InnerTheme theme(Minecraft mc) {
        return InnerTheme.of(MeditationService.aura(mc.player).mainArt());
    }

    /** The void's colour for a reserve this full in this world, reddened by failures. */
    static Vec3 voidColour(InnerTheme theme, float fill, int failures) {
        Vec3 storm = new Vec3(0.20, 0.18, 0.24), clear = new Vec3(theme.red(), theme.green(), theme.blue());
        Vec3 c = storm.lerp(clear, Mth.clamp(fill, 0, 1));
        if (failures > 0) c = c.lerp(new Vec3(0.30, 0.06, 0.08), Math.min(0.5, failures * 0.15));
        return c;
    }

    static final class Effects extends DimensionSpecialEffects {
        Effects() {
            super(Float.NaN, false, SkyType.NONE, false, false);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 fog, float brightness) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return fog;
            return voidColour(theme(mc), MeditationService.aura(mc.player).fill(), CultivationService.journal(mc.player).failures());
        }

        @Override
        public boolean isFoggyAt(int x, int y) {
            return false;
        }
    }

    @SubscribeEvent
    public static void onFogColour(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !InnerWorldService.isInside(mc.player)) return;
        Vec3 c = voidColour(theme(mc), MeditationService.aura(mc.player).fill(), CultivationService.journal(mc.player).failures());
        event.setRed((float) c.x);
        event.setGreen((float) c.y);
        event.setBlue((float) c.z);
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !InnerWorldService.isInside(mc.player)) return;
        event.setNearPlaneDistance(24);
        event.setFarPlaneDistance(90);
        event.setCanceled(true);
    }

    /** Each world's own air, round the cultivator. */
    private static void air(Minecraft mc, ClientLevel level, RandomSource random, Vec3 at, InnerTheme theme) {
        double x = at.x + (random.nextDouble() - 0.5) * 30, y = at.y + random.nextDouble() * 10, z = at.z + (random.nextDouble() - 0.5) * 30;
        switch (theme) {
            case AZURE_CLOUD -> level.addParticle(ParticleTypes.CLOUD, x, at.y - 5 + random.nextDouble() * 2, z, 0.04, 0, 0.01);
            case LONG_FIST, MOUNTAIN_SHATTERING -> level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0, 0, 0);
            case GOLDEN_BELL -> {
                level.addParticle(new DustParticleOptions(new Vector3f(1.0F, 0.8F, 0.25F), 1.2F), x, y, z, 0, 0.01, 0);
                if (level.getGameTime() % 400 == 0) level.playLocalSound(at.x, at.y, at.z, SoundEvents.BELL_BLOCK, SoundSource.AMBIENT, 0.7F, 0.7F, false);
            }
            case CELESTIAL_PEAK -> level.addParticle(ParticleTypes.SNOWFLAKE, x, at.y + 12, z, 0, -0.05, 0);
            case TRAVELLER_STAFF -> level.addParticle(ParticleTypes.CHERRY_LEAVES, x, at.y + 8, z, 0, 0, 0);
            case THUNDER_SABER -> {
                if (random.nextInt(4) == 0) level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0, 0);
                if (random.nextInt(140) == 0) {
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                    if (bolt != null) {
                        bolt.moveTo(at.x + (random.nextDouble() - 0.5) * 50, com.murimblock.cultivation.inner.InnerIsland.GROUND + 1, at.z + (random.nextDouble() - 0.5) * 50);
                        bolt.setVisualOnly(true);
                        level.addEntity(bolt);
                    }
                }
            }
            case SEVEN_STARS -> {
                for (int i = 0; i < 3; i++)
                    level.addAlwaysVisibleParticle(ParticleTypes.END_ROD, true, at.x + (random.nextDouble() - 0.5) * 140, at.y + 25 + random.nextDouble() * 40,
                            at.z + (random.nextDouble() - 0.5) * 140, 0, 0, 0);
            }
            case OVERLORD_SPEAR -> level.addParticle(ParticleTypes.SMALL_FLAME, x, at.y - 1 + random.nextDouble() * 3, z, 0, 0.03, 0);
            case SANGUINE -> level.addParticle(new DustParticleOptions(new Vector3f(0.6F, 0.04F, 0.06F), 2.0F), x, y - 4, z, 0, 0.005, 0);
            case DEVOURING_DEMON -> level.addParticle(ParticleTypes.SCULK_SOUL, x, at.y - 2 + random.nextDouble() * 4, z, 0, 0.02, 0);
            case MAJIN_FIST -> level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y - 3, z, 0, 0.02, 0);
            default -> { }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused() || !InnerWorldService.isInside(mc.player)) return;
        RandomSource random = level.random;
        Vec3 at = mc.player.position();
        // Motes of Qi drifting round, in the main style's colour.
        float[] c = AuraClient.colour(MeditationService.aura(mc.player).mainArt());
        for (int i = 0; i < 3; i++) {
            AuraParticle p = AuraClient.spark(level, at.x + (random.nextDouble() - 0.5) * 30, at.y - 3 + random.nextDouble() * 12, at.z + (random.nextDouble() - 0.5) * 30,
                    (random.nextDouble() - 0.5) * 0.02, 0.01 + random.nextDouble() * 0.01, (random.nextDouble() - 0.5) * 0.02, c, true);
            if (p != null) p.big(0.08F, 80);
        }
        air(mc, level, random, at, theme(mc));
        int failures = CultivationService.journal(mc.player).failures();
        if (failures > 0 && random.nextInt(3) < failures) {
            DustParticleOptions mist = new DustParticleOptions(new Vector3f(0.55F, 0.05F, 0.06F), 2.5F);
            level.addParticle(mist, at.x + (random.nextDouble() - 0.5) * 16, at.y + random.nextDouble() * 3, at.z + (random.nextDouble() - 0.5) * 16, 0, 0.005, 0);
        }
    }
}
