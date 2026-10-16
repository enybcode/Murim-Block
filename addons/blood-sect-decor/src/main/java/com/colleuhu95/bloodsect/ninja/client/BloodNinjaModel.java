package com.colleuhu95.bloodsect.ninja.client;

import com.colleuhu95.bloodsect.BloodSectDecor;
import com.colleuhu95.bloodsect.ninja.BloodNinja;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class BloodNinjaModel extends HumanoidModel<BloodNinja> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(BloodSectDecor.ID, "blood_ninja"), "main");
    private final ModelPart scarfTail;
    private final ModelPart scarfTip;
    private final ModelPart scarfShort;
    private final ModelPart tabardLeft;
    private final ModelPart tabardRight;

    public BloodNinjaModel(ModelPart root) {
        super(root);
        this.scarfTail = this.body.getChild("scarf_tail");
        this.scarfTip = this.scarfTail.getChild("scarf_tip");
        this.scarfShort = this.body.getChild("scarf_short");
        this.tabardLeft = this.body.getChild("tabard_left");
        this.tabardRight = this.body.getChild("tabard_right");
    }

    public static LayerDefinition createBodyLayer() {
        return BloodNinjaGeometry.create();
    }

    @Override
    public void setupAnim(BloodNinja ninja, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        super.setupAnim(ninja, limbSwing, limbAmount, age, yaw, pitch);
        this.leftArm.xRot -= .15F;
        this.rightArm.xRot -= .30F;
        this.leftArm.zRot += .07F;
        this.rightArm.zRot -= .09F;
        float stride = Mth.cos(limbSwing * .6662F) * limbAmount;
        this.scarfTail.xRot = .314F + Mth.sin(age * .11F) * .055F + limbAmount * .55F;
        this.scarfTail.zRot = -.314F + Mth.sin(age * .075F) * .06F;
        this.scarfTip.xRot = -.262F + Mth.sin(age * .13F - 1.1F) * .10F + limbAmount * .18F;
        this.scarfShort.xRot = .524F + Mth.sin(age * .12F + .8F) * .065F + limbAmount * .40F;
        this.scarfShort.zRot = -.611F + Mth.sin(age * .08F) * .07F;
        this.tabardLeft.xRot = -.14F - Math.max(0, -stride) * .60F;
        this.tabardRight.xRot = -.14F - Math.max(0, stride) * .60F;
        if (this.attackTime > 0) {
            float slash = Mth.sin(Mth.sqrt(this.attackTime) * Mth.PI);
            this.rightArm.xRot = -1.35F - slash * 1.15F;
            this.rightArm.yRot = -.4F + slash * .8F;
            this.rightArm.zRot = -.2F - slash * .45F;
            this.leftArm.xRot = -.9F;
            this.body.yRot += slash * .15F;
        }
    }
}
