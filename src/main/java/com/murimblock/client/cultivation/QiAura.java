package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.QiParticleOptions;
import com.murimblock.registry.MurimParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * Qi smoke and specks rising from every meditating player, and a ring of them blown out on a good breath. All of it
 * grows with the Qi the player holds: a wisp at the start, a thick column for a great reserve.
 */
public final class QiAura {
    /** Dantian height above the feet of a player seated in lotus. */
    private static final double DANTIAN = 0.55;
    /** Qi at which the aura is full; it grows on a log scale, so the first hundreds count most. */
    private static final double FULL_QI = 10_000;

    /** Dev screenshots: Qi to show instead of the real one, or below 0 for the real one. */
    public static double previewQi = -1;

    private QiAura() {
    }

    /** Aura power 0-1 for a Qi amount: 100 Qi (a beginner) ≈ 0.15, 1000 ≈ 0.52, {@link #FULL_QI} = 1. */
    public static float power(double qi) {
        return (float) Mth.clamp(Math.log1p(Math.max(0, qi) / 100) / Math.log1p(FULL_QI / 100), 0, 1);
    }

    private static float powerOf(Player player) {
        return power(previewQi >= 0 ? previewQi : ClientCultivation.qiOf(player));
    }

    /** A good breath: a flat ring of Qi smoke rolls out from the dantian and lifts. */
    public static void burst(Player player) {
        RandomSource random = player.getRandom();
        float power = powerOf(player);
        int puffs = Math.round(6 + 14 * power);
        for (int i = 0; i < puffs; i++) {
            double angle = (i + random.nextDouble() * 0.4) / puffs * Math.PI * 2;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double speed = (0.16 + random.nextDouble() * 0.04) * (0.6 + 0.4 * power);
            player.level().addParticle(new QiParticleOptions(MurimParticles.QI_RING.get(), power),
                    player.getX() + cos * 0.4, player.getY() + DANTIAN, player.getZ() + sin * 0.4,
                    cos * speed, 0.01, sin * speed);
            // Specks thrown out with the smoke.
            if (i % 2 == 0) {
                player.level().addParticle(new QiParticleOptions(MurimParticles.QI_SPARK.get(), power),
                        player.getX() + cos * 0.4, player.getY() + DANTIAN, player.getZ() + sin * 0.4,
                        cos * speed * 0.8, 0.03, sin * speed * 0.8);
            }
        }
    }

    /** Spawns {@code rate} particles this tick on average (fractions by chance). */
    private static void rise(Player player, RandomSource random, ParticleType<QiParticleOptions> type, float power,
            float rate) {
        int count = (int) rate + (random.nextFloat() < rate - (int) rate ? 1 : 0);
        for (int i = 0; i < count; i++) {
            rise(player, random, new QiParticleOptions(type, power));
        }
    }

    /** One puff (or speck) leaving the body somewhere between the lap and the shoulders. */
    private static void rise(Player player, RandomSource random, QiParticleOptions options) {
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = 0.4 + random.nextDouble() * 0.3;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double out = 0.004 + random.nextDouble() * 0.01;
        player.level().addParticle(options,
                player.getX() + cos * radius, player.getY() + 0.1 + random.nextDouble() * 0.7,
                player.getZ() + sin * radius,
                cos * out, 0.02 + random.nextDouble() * 0.02, sin * out);
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void registerProviders(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(MurimParticles.QI.get(), sprites -> new QiParticle.Provider(sprites, false));
            event.registerSpriteSet(MurimParticles.QI_RING.get(), sprites -> new QiParticle.Provider(sprites, true));
            event.registerSpriteSet(MurimParticles.QI_SPARK.get(), sprites -> new QiSparkParticle.Provider(sprites, false));
            event.registerSpriteSet(MurimParticles.QI_FALL.get(), sprites -> new QiSparkParticle.Provider(sprites, true));
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.isPaused()) {
                return;
            }
            for (Player player : mc.level.players()) {
                if (ClientCultivation.isMeditating(player)) {
                    RandomSource random = player.getRandom();
                    float power = powerOf(player);
                    // Per tick: a puff every few ticks and a rare speck at the start, 1.5 puffs and a speck at full.
                    rise(player, random, MurimParticles.QI.get(), power, 0.1f + 1.4f * power);
                    rise(player, random, MurimParticles.QI_SPARK.get(), power, 0.05f + 0.95f * power);
                }
            }
        }
    }
}
