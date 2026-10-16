package com.colleuhu95.bloodsect.ninja.client;

import com.colleuhu95.bloodsect.BloodSectDecor;
import com.colleuhu95.bloodsect.ninja.BloodNinja;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public final class BloodNinjaRenderer extends HumanoidMobRenderer<BloodNinja, ReferenceNinjaModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(BloodSectDecor.ID, "textures/entity/blood_ninja_reference.png");

    public BloodNinjaRenderer(EntityRendererProvider.Context context) {
        super(context, new ReferenceNinjaModel(context.bakeLayer(ReferenceNinjaModel.LAYER)), .4F);
        this.layers.removeIf(layer -> layer instanceof ItemInHandLayer);
        this.addLayer(new AnimatedBloodDaoLayer(this, context));
    }

    @Override public ResourceLocation getTextureLocation(BloodNinja ninja) { return TEXTURE; }
}
