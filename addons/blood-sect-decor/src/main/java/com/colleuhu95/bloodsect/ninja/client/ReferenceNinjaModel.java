package com.colleuhu95.bloodsect.ninja.client;

import com.colleuhu95.bloodsect.BloodSectDecor;
import com.colleuhu95.bloodsect.ninja.BloodNinja;
import com.colleuhu95.bloodsect.ninja.DaoAttackMotion;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public final class ReferenceNinjaModel extends HumanoidModel<BloodNinja> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(BloodSectDecor.ID, "blood_ninja"), "main");
    private final ModelPart root;
    private final ModelPart rightForearm, leftForearm, rightShin, leftShin;
    private final ModelPart scarfTail, scarfTip, tabardLeft, tabardRight;

    public ReferenceNinjaModel(ModelPart root) {
        super(root);
        this.root = root;
        this.rightForearm = this.rightArm.getChild("right_forearm");
        this.leftForearm = this.leftArm.getChild("left_forearm");
        this.rightShin = this.rightLeg.getChild("right_shin");
        this.leftShin = this.leftLeg.getChild("left_shin");
        this.scarfTail = this.body.getChild("scarf_tail");
        this.scarfTip = this.scarfTail.getChild("scarf_tip");
        this.tabardLeft = this.body.getChild("tabard_left");
        this.tabardRight = this.body.getChild("tabard_right");
    }

    public static LayerDefinition createBodyLayer() { return ReferenceNinjaGeometry.create(); }

    @Override
    public void setupAnim(BloodNinja ninja, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        this.head.yRot = yaw * Mth.DEG_TO_RAD;
        this.head.xRot = pitch * Mth.DEG_TO_RAD;
        float phase = limbSwing * .6662F;
        float amount = Math.min(1, limbAmount);
        float stride = Mth.cos(phase) * amount;
        float breath = Mth.sin(age * .085F);
        this.body.xRot = .015F + amount * .055F;
        this.body.zRot = stride * .025F;
        this.rightLeg.xRot = stride * .65F;
        this.leftLeg.xRot = -stride * .65F;
        this.rightShin.xRot = .035F + Math.max(0, -Mth.sin(phase)) * amount * .85F;
        this.leftShin.xRot = .035F + Math.max(0, Mth.sin(phase)) * amount * .85F;
        this.rightArm.xRot = -stride * .44F + breath * .012F;
        this.leftArm.xRot = stride * .44F - breath * .012F;
        // Positive Z opens the right arm outward in the model's downward Y axis.
        this.rightArm.zRot = .0873F + breath * .004F;
        this.leftArm.zRot = -.0873F - breath * .004F;
        this.rightForearm.xRot = -Math.max(0, stride) * .05F;
        this.leftForearm.xRot = -Math.max(0, -stride) * .05F;
        this.scarfTail.xRot = .12F + Mth.sin(age * .11F) * .06F + amount * .4F;
        this.scarfTail.zRot = Mth.sin(age * .075F) * .07F;
        this.scarfTip.xRot = Mth.sin(age * .13F - 1.1F) * .13F + amount * .25F;
        this.tabardLeft.xRot = -.06F - Math.max(0, -stride) * .55F;
        this.tabardRight.xRot = -.06F - Math.max(0, stride) * .55F;
        if (this.riding) {
            this.rightLeg.xRot = this.leftLeg.xRot = -1.3F;
            this.rightShin.xRot = this.leftShin.xRot = 1.3F;
        }
        float daoAge = ninja.getDaoAttackAge(Mth.clamp(age - ninja.tickCount, 0, 1));
        if (daoAge < DaoAttackMotion.duration(ninja.getDaoAttackVariant())) {
            applyDaoPose(daoAge,ninja.getDaoAttackVariant());
        }
    }

    public void applyDaoPose(float age,int variant) {
        var p=DaoAttackMotion.sample(age,variant);
        float d=Mth.DEG_TO_RAD;
        this.rightArm.setRotation(p.armX()*d,p.armY()*d,p.armZ()*d);
        this.rightForearm.setRotation(p.elbowX()*d,0,0);
        this.leftArm.setRotation(p.guardX()*d,0,p.guardZ()*d);
        this.leftForearm.setRotation(p.guardElbowX()*d,0,0);
        var torso=torsoTranslation(p);
        this.body.setPos(torso.x,torso.y,torso.z);
        this.body.setRotation(p.torsoX()*d,p.torsoY()*d,0);
        this.head.setRotation(-p.torsoX()*d*.85F,-p.torsoY()*d*.8F,0);
        this.rightLeg.y=this.leftLeg.y=12+p.crouch();
        this.rightLeg.setRotation(p.rightThigh()*d,0,.04F);
        this.leftLeg.setRotation(p.leftThigh()*d,0,-.04F);
        this.rightShin.setRotation(p.rightKnee()*d,0,0);
        this.leftShin.setRotation(p.leftKnee()*d,0,0);
        this.scarfTail.xRot=variant==DaoAttackMotion.M1 ? .15F+Math.abs(p.torsoY())*.009F : .15F+(age>=DaoAttackMotion.WINDUP && age<19 ? .85F : .15F);
        this.scarfTip.xRot=.3F;
        this.tabardLeft.xRot=this.tabardRight.xRot=-p.crouch()*.12F;
    }

    private static Vector3f torsoTranslation(DaoAttackMotion.Pose p) {
        // Lean about the hips, keeping torso and legs joined while the knees bend.
        var hip=new Quaternionf().rotationZYX(0,p.torsoY()*Mth.DEG_TO_RAD,p.torsoX()*Mth.DEG_TO_RAD)
            .transform(new Vector3f(0,12,0));
        return new Vector3f(-hip.x,12+p.crouch()-hip.y,-hip.z);
    }

    /**
     * The hand, from the forearm's pivot: Murimblock's cultist has forearms of full length (the hand 10 pixels below the
     * shoulder), the imported ninja shorter ones (ReferenceNinjaGeometry.HAND_OFFSET_Y).
     */
    private static float handOffsetY() {
        return CultistMesh.get() != null ? -3.8F : ReferenceNinjaGeometry.HAND_OFFSET_Y;
    }

    /**
     * Draws the Blood Cultist's look (CultistMesh) on this model's parts, posed as they are: the cloth with `armour` 0, or
     * one of the four armours (1 to 4). Head and arms follow the body, as in the ninja's own render.
     */
    public void renderCultist(CultistMesh mesh, int armour, PoseStack stack, VertexConsumer buffer, int light, int overlay, int color) {
        java.util.function.Function<String, float[][]> quads = part -> armour == 0 ? mesh.part(part) : mesh.armour(armour, part);
        stack.pushPose();
        this.body.translateAndRotate(stack);
        CultistMesh.draw(quads.apply("body"), stack, buffer, light, overlay, color);
        stack.pushPose();
        this.head.translateAndRotate(stack);
        CultistMesh.draw(quads.apply("head"), stack, buffer, light, overlay, color);
        stack.popPose();
        limb(quads, "right_arm", this.rightArm, "right_forearm", this.rightForearm, stack, buffer, light, overlay, color);
        limb(quads, "left_arm", this.leftArm, "left_forearm", this.leftForearm, stack, buffer, light, overlay, color);
        stack.popPose();
        limb(quads, "right_leg", this.rightLeg, "right_shin", this.rightShin, stack, buffer, light, overlay, color);
        limb(quads, "left_leg", this.leftLeg, "left_shin", this.leftShin, stack, buffer, light, overlay, color);
    }

    private static void limb(java.util.function.Function<String, float[][]> quads, String upper, ModelPart upperPart, String lower, ModelPart lowerPart,
                             PoseStack stack, VertexConsumer buffer, int light, int overlay, int color) {
        stack.pushPose();
        upperPart.translateAndRotate(stack);
        CultistMesh.draw(quads.apply(upper), stack, buffer, light, overlay, color);
        lowerPart.translateAndRotate(stack);
        CultistMesh.draw(quads.apply(lower), stack, buffer, light, overlay, color);
        stack.popPose();
    }

    @Override
    public void renderToBuffer(PoseStack stack, VertexConsumer buffer, int light, int overlay, int color) {
        CultistMesh mesh = CultistMesh.get();
        if (mesh != null) {
            renderCultist(mesh, 0, stack, buffer, light, overlay, color);
            return;
        }
        this.body.render(stack, buffer, light, overlay, color);
        stack.pushPose();
        this.body.translateAndRotate(stack);
        this.head.render(stack, buffer, light, overlay, color);
        this.rightArm.render(stack, buffer, light, overlay, color);
        this.leftArm.render(stack, buffer, light, overlay, color);
        stack.popPose();
        this.rightLeg.render(stack, buffer, light, overlay, color);
        this.leftLeg.render(stack, buffer, light, overlay, color);
    }

    @Override
    public void translateToHand(HumanoidArm side, PoseStack stack) {
        this.body.translateAndRotate(stack);
        this.getArm(side).translateAndRotate(stack);
        (side == HumanoidArm.RIGHT ? this.rightForearm : this.leftForearm).translateAndRotate(stack);
        // Keep the grip aligned with the imported shorter forearm and unchanged hand.
        stack.translate(side == HumanoidArm.RIGHT ? .0625F : -.0625F, handOffsetY() / 16, 1.8F / 16);
    }

    /** Evaluate an earlier hand pose without mutating the live model, for the blade's swept ribbon. */
    public void translateToDaoHandAt(PoseStack stack, float age, int variant) {
        var cut = DaoAttackMotion.sample(age, variant);
        var torso=torsoTranslation(cut);
        stack.translate(torso.x/16,torso.y/16,torso.z/16);
        stack.mulPose(new Quaternionf().rotationZYX(0,cut.torsoY()*Mth.DEG_TO_RAD,cut.torsoX()*Mth.DEG_TO_RAD));
        translatePart(stack, this.rightArm, cut.armX()*Mth.DEG_TO_RAD, cut.armY()*Mth.DEG_TO_RAD, cut.armZ()*Mth.DEG_TO_RAD);
        translatePart(stack, this.rightForearm, cut.elbowX()*Mth.DEG_TO_RAD, 0, 0);
        stack.translate(.0625F, handOffsetY()/16, 1.8F/16);
    }

    private static void translatePart(PoseStack stack, ModelPart part, float x, float y, float z) {
        stack.translate(part.x/16, part.y/16, part.z/16);
        stack.mulPose(new Quaternionf().rotationZYX(z,y,x));
    }
}
