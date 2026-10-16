package com.colleuhu95.bloodsect.ninja.client;

import com.colleuhu95.bloodsect.ninja.BloodNinja;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/** The Blood Cultist's light armour (one of four, or none), over its cloth, on the same posed parts. */
public final class CultistArmourLayer extends RenderLayer<BloodNinja, ReferenceNinjaModel> {
    public CultistArmourLayer(RenderLayerParent<BloodNinja, ReferenceNinjaModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack stack, MultiBufferSource buffers, int light, BloodNinja ninja, float limbSwing, float limbAmount,
                       float partialTick, float age, float yaw, float pitch) {
        CultistMesh mesh = CultistMesh.get();
        int armour = ninja.getArmour();
        if (mesh == null || armour == 0 || ninja.isInvisible()) return;
        var buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(CultistMesh.armourTexture(armour)));
        getParentModel().renderCultist(mesh, armour, stack, buffer, light, LivingEntityRenderer.getOverlayCoords(ninja, 0), -1);
    }
}
