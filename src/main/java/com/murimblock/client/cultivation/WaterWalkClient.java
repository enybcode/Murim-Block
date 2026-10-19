package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Running on water shown (WaterWalking): a splash at each stride and rings spreading behind, for every player near
 * running over water. Vanilla's own splashes and bubbles: a few a tick.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class WaterWalkClient {
    private WaterWalkClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        RandomSource random = mc.level.random;
        long time = mc.level.getGameTime();
        for (Player p : mc.level.players()) {
            // Held up at the water's surface, the water under the feet (another's ground is guessed from their height).
            if (p.distanceToSqr(mc.player) > 48 * 48 || !p.isSprinting()) continue;
            var under = net.minecraft.core.BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
            double above = p.getY() - under.getY();
            if (!mc.level.getFluidState(under).is(FluidTags.WATER) || above < 0.85 || above > 0.95) continue;
            double x = p.getX(), y = p.getY() - 0.01, z = p.getZ();
            // A splash at each stride, kicked up behind.
            if (time % 3 == 0) for (int i = 0; i < 6; i++)
                mc.level.addParticle(ParticleTypes.SPLASH, x + (random.nextDouble() - 0.5) * 0.5, y, z + (random.nextDouble() - 0.5) * 0.5,
                        -p.getDeltaMovement().x * 0.5, 0.15 + random.nextDouble() * 0.1, -p.getDeltaMovement().z * 0.5);
            // A ring spreading on the water.
            if (time % 4 == 0) for (int i = 0; i < 10; i++) {
                double a = i * Math.PI * 2 / 10;
                mc.level.addParticle(ParticleTypes.BUBBLE_POP, x + Math.cos(a) * 0.4, y, z + Math.sin(a) * 0.4, Math.cos(a) * 0.08, 0, Math.sin(a) * 0.08);
            }
        }
    }
}
