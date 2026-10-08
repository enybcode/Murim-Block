package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.mob.MurimEntities;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Temporary vanilla appearance; the Epic Fight zombie preset supplies its matching animated renderer. */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class TrainingOpponentRenderer {
    private TrainingOpponentRenderer() { }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.TRAINING_OPPONENT.get(), ZombieRenderer::new);
    }
}
