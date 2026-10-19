package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.MeditationService;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The aura's pressure seen with the naked eye (AuraPressure, Enzo 18/10/2026): round a player whose aura weighs on those
 * around, the aura Qi vision shows, flames in their art's colour as big as the Qi they hold, and spreading over the
 * ground to the edge of what it weighs on (its reach, sent in the aura every client sees): a low ring of flames there, a
 * few inside. Kept light: some eight motes a tick on the ground. Not drawn while Qi vision is open (it shows its own).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class AuraPressureClient {
    /** How near a player must be for their aura to show (blocks). */
    static final double NEAR = 48;

    private AuraPressureClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        // Its key: released, or held in (the server decides and says so).
        while (mc.player != null && mc.screen == null && com.murimblock.client.MurimblockKeyMappings.AURA_PRESSURE.consumeClick())
            if (mc.getConnection() != null) net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.murimblock.network.CultivationPayloads.TogglePressure());
        if (mc.level == null || mc.player == null || mc.isPaused() || QiSenseClient.open()) return;
        RandomSource random = mc.level.random;
        for (Player p : mc.level.players()) {
            var aura = MeditationService.aura(p);
            float reach = aura.pressure();
            // Not on the sword: its trail stands for the aura (flames left in the air would fill the view behind).
            if (reach <= 0 || p.isSpectator() || p.isInvisible() || p.distanceToSqr(mc.player) > NEAR * NEAR || SwordFlightClient.flying(p)) continue;
            float[] c = aura.auraArt().isPresent() ? AuraClient.colour(aura.auraArt()) : new float[]{0.35F, 0.75F, 1.0F};
            Vec3 feet = p.position();
            // The body's aura, as Qi vision shows it (one's own in first person would only veil the view).
            if (!(p == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()))
                AuraClient.volume(mc.level, random, feet, 0.6F, c, AuraClient.qiScale(aura.qi()));
            // On the ground only (in the air, flying on a sword, there is no ground for it to spread over).
            if (!p.onGround()) continue;
            // Its edge on the ground, where it stops weighing: a low ring of flames all round.
            for (int i = 0; i < 6; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                AuraParticle f = AuraClient.spark(mc.level, feet.x + Math.cos(a) * reach, feet.y + 0.05, feet.z + Math.sin(a) * reach,
                        0, 0.03 + random.nextDouble() * 0.03, 0, c, false);
                if (f != null) f.big(0.24F + random.nextFloat() * 0.1F, 18 + random.nextInt(8));
            }
            // Inside, a few flames low over the ground.
            for (int i = 0; i < 2; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = reach * Math.sqrt(random.nextDouble());
                AuraParticle f = AuraClient.spark(mc.level, feet.x + Math.cos(a) * r, feet.y + 0.05, feet.z + Math.sin(a) * r,
                        0, 0.015 + random.nextDouble() * 0.015, 0, c, false);
                if (f != null) f.big(0.16F + random.nextFloat() * 0.1F, 22 + random.nextInt(10));
            }
        }
    }
}
