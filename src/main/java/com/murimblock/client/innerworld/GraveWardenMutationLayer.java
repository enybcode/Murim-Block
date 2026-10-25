package com.murimblock.client.innerworld;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.innerworld.TrialZombie;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * The Grave Warden's growths, following its body and right arm as they move: three bone spikes down the spine, a
 * hunch between the shoulders, a bone knot with a spur on the right shoulder, and a swollen right forearm. Textured
 * from the bottom half of {@code grave_warden.png}.
 */
public final class GraveWardenMutationLayer extends RenderLayer<TrialZombie, ZombieModel<TrialZombie>> {
    private final ResourceLocation texture;
    private final ModelPart back;
    private final ModelPart arm;

    public GraveWardenMutationLayer(RenderLayerParent<TrialZombie, ZombieModel<TrialZombie>> parent,
                                    ResourceLocation texture) {
        super(parent);
        this.texture = texture;
        ModelPart root = create();
        back = root.getChild("back");
        arm = root.getChild("arm");
    }

    /** In the body's and the right arm's own space (the zombie faces -z, y down, the back at +z). */
    private static ModelPart create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition back = root.addOrReplaceChild("back", CubeListBuilder.create()
                .texOffs(40, 32).addBox(-3, 0.5f, 2, 6, 3, 2), PartPose.ZERO);
        // Spikes point up and back, leaning less the lower they grow.
        float[][] spikes = {{4, 0.5f}, {7, 0.4f}, {10, 0.3f}};
        for (int i = 0; i < spikes.length; i++) {
            back.addOrReplaceChild("spike" + i, CubeListBuilder.create().texOffs(0, 32).addBox(-0.5f, -4, -0.5f, 1, 4, 1),
                    PartPose.offsetAndRotation(0, spikes[i][0], 2.2f, -(spikes[i][1] + 1.0f), 0, 0));
        }
        back.addOrReplaceChild("knot", CubeListBuilder.create().texOffs(8, 32).addBox(-1, -1, -1, 2, 2, 2),
                PartPose.offset(-4.5f, 0.2f, 0.5f));
        back.addOrReplaceChild("spur", CubeListBuilder.create().texOffs(0, 32).addBox(-0.5f, -4, -0.5f, 1, 4, 1),
                PartPose.offsetAndRotation(-4.8f, -0.4f, 0.8f, 0.3f, 0, -0.5f));
        root.addOrReplaceChild("arm", CubeListBuilder.create().texOffs(16, 32).addBox(-3.5f, 4.5f, -2.5f, 5, 5, 5),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64).bakeRoot();
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, TrialZombie zombie, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!zombie.isWarden() || zombie.isInvisible()) {
            return;
        }
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        int overlay = LivingEntityRenderer.getOverlayCoords(zombie, 0);
        ZombieModel<TrialZombie> model = getParentModel();
        pose.pushPose();
        model.body.translateAndRotate(pose);
        back.render(pose, buffer, light, overlay);
        pose.popPose();
        pose.pushPose();
        model.rightArm.translateAndRotate(pose);
        arm.render(pose, buffer, light, overlay);
        pose.popPose();
    }
}
