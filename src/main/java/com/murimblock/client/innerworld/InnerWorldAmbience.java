package com.murimblock.client.innerworld;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.QiParticleOptions;
import com.murimblock.registry.MurimParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * The air of the inner world: a sea of clouds drifting below the island, and specks of Qi that rise all around and
 * fade, more of them near the player.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class InnerWorldAmbience {
    /** The dimension type's {@code effects}. */
    public static final ResourceLocation EFFECTS = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "inner_world");
    /** Below the island's lowest rock (about Y 73) and above the void. */
    private static final float CLOUD_LEVEL = 64;
    private static final int MOTE_RANGE = 20;
    private static final float MOTE_POWER = 0.35f;

    private InnerWorldAmbience() {
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(EFFECTS, new Effects());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused() || !InnerWorldClient.inside()) {
            return;
        }
        RandomSource random = level.random;
        for (int i = 0; i < 2; i++) {
            double x = mc.player.getX() + (random.nextDouble() * 2 - 1) * MOTE_RANGE;
            double z = mc.player.getZ() + (random.nextDouble() * 2 - 1) * MOTE_RANGE;
            double y = mc.player.getY() - 1 + random.nextDouble() * 6;
            level.addParticle(new QiParticleOptions(MurimParticles.QI_SPARK.get(), MOTE_POWER), x, y, z,
                    (random.nextDouble() - 0.5) * 0.01, 0.005 + random.nextDouble() * 0.01, (random.nextDouble() - 0.5) * 0.01);
        }
    }

    /** Like the overworld's sky and fog, with the clouds moved below the island. */
    private static final class Effects extends DimensionSpecialEffects {
        Effects() {
            super(CLOUD_LEVEL, true, SkyType.NORMAL, false, false);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
            return fogColor.multiply(brightness * 0.94f + 0.06f, brightness * 0.94f + 0.06f, brightness * 0.91f + 0.09f);
        }

        @Override
        public boolean isFoggyAt(int x, int y) {
            return false;
        }
    }
}
