package com.murimblock.client.innerworld;

import com.murimblock.Murimblock;
import com.murimblock.innerworld.OldMaster;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** The Old Master's GeckoLib model ({@code scripts/old-master.cjs}); the head bone follows his gaze. */
public final class OldMasterRenderer extends GeoEntityRenderer<OldMaster> {
    public OldMasterRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "old_master"), true));
        shadowRadius = 0.5f;
    }
}
