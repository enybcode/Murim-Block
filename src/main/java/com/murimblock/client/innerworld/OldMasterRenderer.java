package com.murimblock.client.innerworld;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.innerworld.OldMaster;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

/**
 * The Old Master's GeckoLib model ({@code scripts/old-master.cjs}), drawn as a ghost: translucent, lit by his own
 * glow even in the dark, his opacity breathing slowly. The head bone follows his gaze.
 */
public final class OldMasterRenderer extends GeoEntityRenderer<OldMaster> {
    private static final float ALPHA = 0.7f;
    private static final float ALPHA_BREATH = 0.08f;

    public OldMasterRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "old_master"), true));
        shadowRadius = 0.25f;
        shadowStrength = 0.4f;
    }

    @Override
    public void render(OldMaster master, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(master, yaw, partialTick, pose, buffers, LightTexture.FULL_BRIGHT);
    }

    @Override
    public RenderType getRenderType(OldMaster master, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public Color getRenderColor(OldMaster master, float partialTick, int light) {
        float breath = Mth.sin((master.tickCount + partialTick) * 0.08f);
        return Color.ofRGBA(1f, 1f, 1f, ALPHA + ALPHA_BREATH * breath);
    }
}
