package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.registry.MurimParticles;
import net.minecraft.client.Minecraft;
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

    /** A good breath: a ring of Qi rushes in at once. */
    public static void burst(Player player) {
        RandomSource random = player.getRandom();
        for (int i = 0; i < 14; i++) {
            spawn(player, random, 0.9 + random.nextDouble() * 0.6);
        }
    }

    private static void spawn(Player player, RandomSource random, double radius) {
        double angle = random.nextDouble() * Math.PI * 2;
        double x = player.getX();
        double y = player.getY() + DANTIAN;
        double z = player.getZ();
        player.level().addParticle(MurimParticles.QI.get(),
                x + Math.cos(angle) * radius, y - 0.4 + random.nextDouble() * 1.6, z + Math.sin(angle) * radius,
                x, y, z);
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void registerProviders(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(MurimParticles.QI.get(), QiParticle.Provider::new);
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.isPaused()) {
                return;
            }
            for (Player player : mc.level.players()) {
                if (ClientCultivation.isMeditating(player) && player.tickCount % 2 == 0) {
                    spawn(player, player.getRandom(), 1.3 + player.getRandom().nextDouble() * 0.9);
                }
            }
        }
    }
}
