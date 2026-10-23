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

/** Qi motes drawn into the dantian of every meditating player, and a rush of them on a good breath. */
public final class QiAura {
    /** Dantian height above the feet of a player seated in lotus. */
    private static final double DANTIAN = 0.55;

    private QiAura() {
    }

    /** Motes in a good breath's ring. */
    private static final int RING = 32;

    /** A good breath: a flat ring of Qi at dantian height closes in at once. */
    public static void burst(Player player) {
        RandomSource random = player.getRandom();
        for (int i = 0; i < RING; i++) {
            double angle = (i + random.nextDouble() * 0.3) / RING * Math.PI * 2;
            spawn(player, MurimParticles.QI_RING.get(), angle, 1.9 + random.nextDouble() * 0.2,
                    random.nextDouble() * 0.1 - 0.05);
        }
    }

    private static void drift(Player player, RandomSource random) {
        spawn(player, MurimParticles.QI.get(), random.nextDouble() * Math.PI * 2, 1.3 + random.nextDouble() * 0.9,
                random.nextDouble() * 1.6 - 0.4);
    }

    private static void spawn(Player player, SimpleParticleType type, double angle, double radius, double dy) {
        double x = player.getX();
        double y = player.getY() + DANTIAN;
        double z = player.getZ();
        player.level().addParticle(type,
                x + Math.cos(angle) * radius, y + dy, z + Math.sin(angle) * radius, x, y, z);
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void registerProviders(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(MurimParticles.QI.get(), sprites -> new QiParticle.Provider(sprites, false));
            event.registerSpriteSet(MurimParticles.QI_RING.get(), sprites -> new QiParticle.Provider(sprites, true));
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.isPaused()) {
                return;
            }
            for (Player player : mc.level.players()) {
                if (ClientCultivation.isMeditating(player)) {
                    drift(player, player.getRandom());
                    if (player.getRandom().nextBoolean()) {
                        drift(player, player.getRandom());
                    }
                }
            }
        }
    }
}
