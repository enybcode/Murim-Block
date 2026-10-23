package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.registry.MurimParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Qi smoke and specks rising from every meditating player, and a ring of them blown out on a good breath. */
public final class QiAura {
    /** Dantian height above the feet of a player seated in lotus. */
    private static final double DANTIAN = 0.55;
    /** Puffs in a good breath's ring. */
    private static final int RING = 20;

    private QiAura() {
    }

    /** A good breath: a flat ring of Qi smoke rolls out from the dantian and lifts. */
    public static void burst(Player player) {
        RandomSource random = player.getRandom();
        for (int i = 0; i < RING; i++) {
            double angle = (i + random.nextDouble() * 0.4) / RING * Math.PI * 2;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double speed = 0.16 + random.nextDouble() * 0.04;
            player.level().addParticle(MurimParticles.QI_RING.get(),
                    player.getX() + cos * 0.4, player.getY() + DANTIAN, player.getZ() + sin * 0.4,
                    cos * speed, 0.01, sin * speed);
            // Specks thrown out with the smoke.
            if (i % 2 == 0) {
                player.level().addParticle(MurimParticles.QI_SPARK.get(),
                        player.getX() + cos * 0.4, player.getY() + DANTIAN, player.getZ() + sin * 0.4,
                        cos * speed * 0.8, 0.03, sin * speed * 0.8);
            }
        }
    }

    /** One puff (or speck) leaving the body somewhere between the lap and the shoulders. */
    private static void rise(Player player, RandomSource random, SimpleParticleType type) {
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = 0.4 + random.nextDouble() * 0.3;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double out = 0.004 + random.nextDouble() * 0.01;
        player.level().addParticle(type,
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
            event.registerSpriteSet(MurimParticles.QI_SPARK.get(), QiSparkParticle.Provider::new);
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
                    rise(player, random, MurimParticles.QI.get());
                    if (random.nextBoolean()) {
                        rise(player, random, MurimParticles.QI.get());
                    }
                    rise(player, random, MurimParticles.QI_SPARK.get());
                }
            }
        }
    }
}
