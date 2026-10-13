package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.mob.MurimEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** A blade of Qi has no model: it is drawn only by the specks it scatters as it flies (QiBladeEntity). */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QiBladeRenderer {
    private QiBladeRenderer() {
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.QI_BLADE.get(), NoopRenderer::new);
    }
}
