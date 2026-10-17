package com.murimblock.bloodsect.ninja.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

// Imported from ninja_sang_murim_bras_raccourcis.bbmodel by import_short_arm_ninja.py.
// Signed Y deformation preserves the original face UVs while shortening each cube.
final class ReferenceNinjaGeometry {
    static final float HAND_OFFSET_Y = -4.95000000F;
    static LayerDefinition create() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        var head = root.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(0,0).addBox(-4.00000000F,-8.00000000F,-4.00000000F,8.00000000F,8.00000000F,8.00000000F)
            .texOffs(0,64).addBox(-4.12000000F,-8.15000000F,-4.08000000F,8.24000000F,1.60000000F,8.20000000F)
            .texOffs(0,64).addBox(-4.10000000F,-6.60000000F,3.74000000F,8.20000000F,7.30000000F,0.55000000F)
            .texOffs(0,64).addBox(-4.10000000F,-6.50000000F,-3.85000000F,0.60000000F,5.00000000F,1.15000000F)
            .texOffs(0,64).addBox(-4.15000000F,-5.70000000F,2.55000000F,0.70000000F,7.20000000F,1.50000000F)
            .texOffs(0,64).addBox(3.50000000F,-6.50000000F,-3.85000000F,0.60000000F,5.00000000F,1.15000000F)
            .texOffs(0,64).addBox(3.45000000F,-5.70000000F,2.55000000F,0.70000000F,7.20000000F,1.50000000F)
            .texOffs(0,64).addBox(-1.50000000F,-10.00000000F,1.10000000F,3.00000000F,2.10000000F,2.80000000F)
            .texOffs(32,32).addBox(-1.53000000F,-8.65000000F,1.06000000F,3.06000000F,0.55000000F,2.88000000F)
            .texOffs(32,32).addBox(-4.23000000F,-6.35000000F,-4.20000000F,8.46000000F,1.05000000F,0.30000000F)
            .texOffs(32,32).addBox(-4.22000000F,-6.35000000F,4.05000000F,8.44000000F,1.05000000F,0.30000000F)
            .texOffs(32,32).addBox(-4.23000000F,-6.35000000F,-4.05000000F,0.26000000F,1.05000000F,8.25000000F)
            .texOffs(32,32).addBox(3.97000000F,-6.35000000F,-4.05000000F,0.26000000F,1.05000000F,8.25000000F)
            .texOffs(96,64).addBox(-3.91000000F,-3.15000000F,-4.14000000F,7.82000000F,3.17000000F,0.31000000F)
            .texOffs(64,96).addBox(-4.28000000F,-5.53000000F,-4.29000000F,8.56000000F,0.20000000F,0.18000000F)
            .texOffs(96,64).addBox(-1.60000000F,-8.40000000F,1.02000000F,3.20000000F,0.21000000F,2.98000000F)
            , PartPose.offsetAndRotation(0.00000000F,0.00000000F,0.00000000F,0.00000000F,0.00000000F,0.00000000F));
        var headband_tail = head.addOrReplaceChild("headband_tail", CubeListBuilder.create()
            .texOffs(32,32).addBox(-0.45000000F,0.00000000F,0.00000000F,0.90000000F,4.40000000F,0.18000000F)
            , PartPose.offsetAndRotation(3.30000000F,-5.80000000F,4.20000000F,0.17453293F,0.00000000F,-0.20943951F));
        var body = root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0,32).addBox(-4.00000000F,0.00000000F,-2.00000000F,8.00000000F,11.50000000F,4.00000000F)
            .texOffs(64,64).addBox(-1.85000000F,-0.40000000F,-2.13000000F,3.70000000F,1.40000000F,0.32000000F)
            .texOffs(32,32).addBox(-2.10000000F,-0.60000000F,-2.30000000F,4.20000000F,1.50000000F,4.60000000F)
            .texOffs(32,32).addBox(-4.14000000F,9.00000000F,-2.14000000F,8.28000000F,2.50000000F,4.28000000F)
            .texOffs(96,64).addBox(-4.18000000F,9.15000000F,-2.22000000F,8.36000000F,0.35000000F,0.18000000F)
            .texOffs(32,32).addBox(2.40000000F,9.35000000F,-2.60000000F,1.90000000F,1.70000000F,0.80000000F)
            .texOffs(0,96).addBox(-3.60000000F,0.55000000F,1.94000000F,7.20000000F,1.00000000F,0.32000000F)
            .texOffs(64,96).addBox(-4.20000000F,10.75000000F,-2.32000000F,8.40000000F,0.30000000F,0.30000000F)
            , PartPose.offsetAndRotation(0.00000000F,0.00000000F,0.00000000F,0.00000000F,0.00000000F,0.00000000F));
        var left_lapel = body.addOrReplaceChild("left_lapel", CubeListBuilder.create()
            .texOffs(64,64).addBox(-0.48000000F,0.00000000F,-0.06000000F,0.96000000F,7.10000000F,0.25000000F)
            .texOffs(0,96).addBox(-0.72000000F,0.15000000F,-0.28000000F,1.44000000F,6.80000000F,0.25000000F)
            .texOffs(32,96).addBox(-0.76000000F,0.12000000F,-0.38000000F,0.18000000F,6.86000000F,0.20000000F)
            .texOffs(64,64).addBox(0.54000000F,0.27000000F,-0.35000000F,0.16000000F,6.60000000F,0.15000000F)
            , PartPose.offsetAndRotation(-2.10000000F,0.30000000F,-2.16000000F,0.00000000F,0.00000000F,-0.36651914F));
        var right_lapel = body.addOrReplaceChild("right_lapel", CubeListBuilder.create()
            .texOffs(64,64).addBox(-0.40000000F,0.00000000F,-0.07000000F,0.80000000F,5.70000000F,0.25000000F)
            .texOffs(0,96).addBox(-0.64000000F,0.15000000F,-0.28000000F,1.28000000F,5.40000000F,0.25000000F)
            .texOffs(32,96).addBox(-0.68000000F,0.12000000F,-0.38000000F,0.18000000F,5.46000000F,0.20000000F)
            .texOffs(64,64).addBox(0.46000000F,0.27000000F,-0.35000000F,0.16000000F,5.20000000F,0.15000000F)
            , PartPose.offsetAndRotation(2.15000000F,0.30000000F,-2.17000000F,0.00000000F,0.00000000F,0.41887902F));
        var scarf_front = body.addOrReplaceChild("scarf_front", CubeListBuilder.create()
            .texOffs(32,32).addBox(-0.60000000F,0.00000000F,0.00000000F,1.20000000F,6.00000000F,0.20000000F)
            .texOffs(96,64).addBox(-0.60000000F,0.15000000F,-0.13000000F,0.22000000F,5.45000000F,0.18000000F)
            , PartPose.offsetAndRotation(-1.40000000F,0.70000000F,-2.35000000F,0.03490659F,0.00000000F,0.00000000F));
        var scarf_tail = body.addOrReplaceChild("scarf_tail", CubeListBuilder.create()
            .texOffs(32,32).addBox(-0.65000000F,0.00000000F,-0.10000000F,1.30000000F,5.00000000F,0.22000000F)
            .texOffs(64,96).addBox(0.30000000F,0.20000000F,0.12000000F,0.20000000F,4.50000000F,0.15000000F)
            , PartPose.offsetAndRotation(2.10000000F,0.10000000F,2.20000000F,0.24434610F,0.00000000F,0.20943951F));
        var scarf_tip = scarf_tail.addOrReplaceChild("scarf_tip", CubeListBuilder.create()
            .texOffs(32,32).addBox(-0.55000000F,0.00000000F,-0.08000000F,1.10000000F,3.40000000F,0.18000000F)
            , PartPose.offsetAndRotation(0.00000000F,5.00000000F,0.00000000F,-0.17453293F,0.00000000F,0.00000000F));
        var sash_tail = body.addOrReplaceChild("sash_tail", CubeListBuilder.create()
            .texOffs(32,32).addBox(-0.45000000F,0.00000000F,0.00000000F,0.90000000F,6.30000000F,0.22000000F)
            .texOffs(64,96).addBox(-0.45000000F,0.15000000F,-0.10000000F,0.16000000F,5.95000000F,0.16000000F)
            , PartPose.offsetAndRotation(3.10000000F,10.60000000F,-2.55000000F,0.00000000F,0.00000000F,-0.06981317F));
        var right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
            .texOffs(0,32).addBox(-1.95000000F,-2.45766666F,-2.05000000F,3.90000000F,7.10000000F,4.10000000F, new CubeDeformation(0, -0.80766666F, 0))
            .texOffs(64,64).addBox(-1.99000000F,3.17037038F,-2.09000000F,3.98000000F,0.45000000F,4.18000000F, new CubeDeformation(0, -0.06666665F, 0))
            .texOffs(0,96).addBox(-2.06000000F,-1.82000000F,-2.16000000F,4.12000000F,0.45000000F,4.32000000F)
            .texOffs(32,96).addBox(-2.05000000F,3.25629628F,-2.15000000F,4.10000000F,0.25000000F,4.30000000F, new CubeDeformation(0, -0.03703703F, 0))
            , PartPose.offsetAndRotation(-5.50000000F,2.00000000F,0.00000000F,0.00000000F,0.00000000F,0.08726646F));
        var right_forearm = right_arm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
            .texOffs(0,32).addBox(-1.80000000F,-0.70277780F,-1.90000000F,3.60000000F,3.90000000F,3.80000000F, new CubeDeformation(0, -0.59722221F, 0))
            .texOffs(64,32).addBox(-1.86000000F,1.94166667F,-1.96000000F,3.72000000F,0.45000000F,3.92000000F, new CubeDeformation(0, -0.06900000F, 0))
            .texOffs(96,64).addBox(-1.84000000F,2.36293334F,-1.94000000F,3.68000000F,0.30000000F,3.88000000F, new CubeDeformation(0, -0.04293333F, 0))
            .texOffs(96,32).addBox(-1.68000000F,2.60000000F,-1.70000000F,3.36000000F,2.25000000F,3.40000000F)
            .texOffs(32,32).addBox(-1.73000000F,2.70000000F,-1.75000000F,3.46000000F,0.70000000F,3.50000000F)
            .texOffs(64,64).addBox(-1.94000000F,1.85213333F,-2.04000000F,3.88000000F,0.22000000F,4.08000000F, new CubeDeformation(0, -0.03373334F, 0))
            .texOffs(64,32).addBox(-1.92000000F,2.24653333F,-2.02000000F,3.84000000F,0.18000000F,4.04000000F, new CubeDeformation(0, -0.02760000F, 0))
            .texOffs(96,64).addBox(-1.91000000F,2.68000000F,-0.45000000F,0.42000000F,0.75000000F,0.90000000F)
            , PartPose.offsetAndRotation(0.00000000F,3.80000000F,0.00000000F,0.00000000F,0.00000000F,0.00000000F));
        var right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
            .texOffs(0,32).addBox(-1.82000000F,0.00000000F,-1.86000000F,3.64000000F,6.15000000F,3.72000000F)
            , PartPose.offsetAndRotation(-1.90000000F,12.00000000F,0.00000000F,0.00000000F,0.00000000F,0.00000000F));
        var right_shin = right_leg.addOrReplaceChild("right_shin", CubeListBuilder.create()
            .texOffs(0,32).addBox(-1.76000000F,0.00000000F,-1.80000000F,3.52000000F,4.15000000F,3.60000000F)
            .texOffs(32,64).addBox(-1.79000000F,3.50000000F,-1.93000000F,3.58000000F,2.50000000F,3.90000000F)
            .texOffs(0,64).addBox(-1.82000000F,5.62000000F,-2.00000000F,3.64000000F,0.38000000F,4.02000000F)
            .texOffs(96,64).addBox(-1.83000000F,3.70000000F,-1.96000000F,3.66000000F,0.35000000F,3.96000000F)
            .texOffs(0,96).addBox(-1.88000000F,3.45000000F,-2.01000000F,3.76000000F,0.32000000F,4.06000000F)
            .texOffs(32,64).addBox(-1.55000000F,4.80000000F,-2.07000000F,3.10000000F,0.70000000F,0.23000000F)
            .texOffs(32,32).addBox(-1.90000000F,3.60000000F,-0.60000000F,0.34000000F,0.65000000F,1.00000000F)
            , PartPose.offsetAndRotation(0.00000000F,6.00000000F,0.00000000F,0.00000000F,0.00000000F,0.00000000F));
        var tabard_right = body.addOrReplaceChild("tabard_right", CubeListBuilder.create()
            .texOffs(0,32).addBox(-1.80000000F,0.00000000F,-0.07000000F,3.60000000F,6.50000000F,0.25000000F)
            .texOffs(64,64).addBox(-1.83000000F,6.10000000F,-0.12000000F,3.66000000F,0.40000000F,0.30000000F)
            .texOffs(64,64).addBox(-1.83000000F,0.15000000F,-0.23000000F,0.22000000F,6.00000000F,0.18000000F)
            .texOffs(0,96).addBox(0.65000000F,0.30000000F,-0.23000000F,0.30000000F,5.70000000F,0.20000000F)
            , PartPose.offsetAndRotation(-2.00000000F,11.00000000F,-2.10000000F,-0.03490659F,0.00000000F,0.00000000F));
        var coat_back_right = body.addOrReplaceChild("coat_back_right", CubeListBuilder.create()
            .texOffs(0,32).addBox(-1.80000000F,0.00000000F,-0.06000000F,3.60000000F,7.20000000F,0.25000000F)
            .texOffs(64,64).addBox(-1.83000000F,6.80000000F,-0.10000000F,3.66000000F,0.40000000F,0.30000000F)
            .texOffs(64,64).addBox(-1.83000000F,0.15000000F,0.24000000F,0.22000000F,6.70000000F,0.18000000F)
            .texOffs(0,96).addBox(0.65000000F,0.30000000F,0.24000000F,0.30000000F,6.40000000F,0.20000000F)
            , PartPose.offsetAndRotation(-2.00000000F,11.00000000F,2.02000000F,0.03490659F,0.00000000F,0.00000000F));
        var left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
            .texOffs(0,32).addBox(-1.95000000F,-2.45766666F,-2.05000000F,3.90000000F,7.10000000F,4.10000000F, new CubeDeformation(0, -0.80766666F, 0))
            .texOffs(64,64).addBox(-1.99000000F,3.17037038F,-2.09000000F,3.98000000F,0.45000000F,4.18000000F, new CubeDeformation(0, -0.06666665F, 0))
            .texOffs(0,96).addBox(-2.06000000F,-1.82000000F,-2.16000000F,4.12000000F,0.45000000F,4.32000000F)
            .texOffs(32,96).addBox(-2.05000000F,3.25629628F,-2.15000000F,4.10000000F,0.25000000F,4.30000000F, new CubeDeformation(0, -0.03703703F, 0))
            , PartPose.offsetAndRotation(5.50000000F,2.00000000F,0.00000000F,0.00000000F,0.00000000F,-0.08726646F));
        var left_forearm = left_arm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
            .texOffs(0,32).addBox(-1.80000000F,-0.70277780F,-1.90000000F,3.60000000F,3.90000000F,3.80000000F, new CubeDeformation(0, -0.59722221F, 0))
            .texOffs(64,32).addBox(-1.86000000F,1.94166667F,-1.96000000F,3.72000000F,0.45000000F,3.92000000F, new CubeDeformation(0, -0.06900000F, 0))
            .texOffs(96,64).addBox(-1.84000000F,2.36293334F,-1.94000000F,3.68000000F,0.30000000F,3.88000000F, new CubeDeformation(0, -0.04293333F, 0))
            .texOffs(96,32).addBox(-1.68000000F,2.60000000F,-1.70000000F,3.36000000F,2.25000000F,3.40000000F)
            .texOffs(32,32).addBox(-1.73000000F,2.70000000F,-1.75000000F,3.46000000F,0.70000000F,3.50000000F)
            .texOffs(64,64).addBox(-1.94000000F,1.85213333F,-2.04000000F,3.88000000F,0.22000000F,4.08000000F, new CubeDeformation(0, -0.03373334F, 0))
            .texOffs(64,32).addBox(-1.92000000F,2.24653333F,-2.02000000F,3.84000000F,0.18000000F,4.04000000F, new CubeDeformation(0, -0.02760000F, 0))
            .texOffs(96,64).addBox(1.49000000F,2.68000000F,-0.45000000F,0.42000000F,0.75000000F,0.90000000F)
            , PartPose.offsetAndRotation(0.00000000F,3.80000000F,0.00000000F,0.00000000F,0.00000000F,0.00000000F));
        var left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
            .texOffs(0,32).addBox(-1.82000000F,0.00000000F,-1.86000000F,3.64000000F,6.15000000F,3.72000000F)
            , PartPose.offsetAndRotation(1.90000000F,12.00000000F,0.00000000F,0.00000000F,0.00000000F,0.00000000F));
        var left_shin = left_leg.addOrReplaceChild("left_shin", CubeListBuilder.create()
            .texOffs(0,32).addBox(-1.76000000F,0.00000000F,-1.80000000F,3.52000000F,4.15000000F,3.60000000F)
            .texOffs(32,64).addBox(-1.79000000F,3.50000000F,-1.93000000F,3.58000000F,2.50000000F,3.90000000F)
            .texOffs(0,64).addBox(-1.82000000F,5.62000000F,-2.00000000F,3.64000000F,0.38000000F,4.02000000F)
            .texOffs(96,64).addBox(-1.83000000F,3.70000000F,-1.96000000F,3.66000000F,0.35000000F,3.96000000F)
            .texOffs(0,96).addBox(-1.88000000F,3.45000000F,-2.01000000F,3.76000000F,0.32000000F,4.06000000F)
            .texOffs(32,64).addBox(-1.55000000F,4.80000000F,-2.07000000F,3.10000000F,0.70000000F,0.23000000F)
            .texOffs(32,32).addBox(1.56000000F,3.60000000F,-0.60000000F,0.34000000F,0.65000000F,1.00000000F)
            , PartPose.offsetAndRotation(0.00000000F,6.00000000F,0.00000000F,0.00000000F,0.00000000F,0.00000000F));
        var tabard_left = body.addOrReplaceChild("tabard_left", CubeListBuilder.create()
            .texOffs(0,32).addBox(-1.80000000F,0.00000000F,-0.07000000F,3.60000000F,6.50000000F,0.25000000F)
            .texOffs(64,64).addBox(-1.83000000F,6.10000000F,-0.12000000F,3.66000000F,0.40000000F,0.30000000F)
            .texOffs(64,64).addBox(-1.83000000F,0.15000000F,-0.23000000F,0.22000000F,6.00000000F,0.18000000F)
            .texOffs(0,96).addBox(0.65000000F,0.30000000F,-0.23000000F,0.30000000F,5.70000000F,0.20000000F)
            , PartPose.offsetAndRotation(2.00000000F,11.00000000F,-2.10000000F,-0.03490659F,0.00000000F,0.00000000F));
        var coat_back_left = body.addOrReplaceChild("coat_back_left", CubeListBuilder.create()
            .texOffs(0,32).addBox(-1.80000000F,0.00000000F,-0.06000000F,3.60000000F,7.20000000F,0.25000000F)
            .texOffs(64,64).addBox(-1.83000000F,6.80000000F,-0.10000000F,3.66000000F,0.40000000F,0.30000000F)
            .texOffs(64,64).addBox(-1.83000000F,0.15000000F,0.24000000F,0.22000000F,6.70000000F,0.18000000F)
            .texOffs(0,96).addBox(0.65000000F,0.30000000F,0.24000000F,0.30000000F,6.40000000F,0.20000000F)
            , PartPose.offsetAndRotation(2.00000000F,11.00000000F,2.02000000F,0.03490659F,0.00000000F,0.00000000F));
        var mask_cheek_right = head.addOrReplaceChild("mask_cheek_right", CubeListBuilder.create()
            .texOffs(96,64).addBox(-1.87000000F,-1.35000000F,-0.12000000F,3.74000000F,2.70000000F,0.26000000F)
            , PartPose.offsetAndRotation(-2.00000000F,-1.60000000F,-4.16000000F,0.00000000F,0.13962634F,-0.06981317F));
        var mask_pleat_right0 = head.addOrReplaceChild("mask_pleat_right0", CubeListBuilder.create()
            .texOffs(64,96).addBox(-1.65000000F,0.00000000F,-0.10000000F,3.30000000F,0.19000000F,0.23000000F)
            , PartPose.offsetAndRotation(-1.72000000F,-2.65000000F,-4.46000000F,0.00000000F,0.10471976F,-0.08726646F));
        var mask_pleat_right1 = head.addOrReplaceChild("mask_pleat_right1", CubeListBuilder.create()
            .texOffs(96,64).addBox(-1.65000000F,0.00000000F,-0.10000000F,3.30000000F,0.19000000F,0.23000000F)
            , PartPose.offsetAndRotation(-1.72000000F,-1.86000000F,-4.43500000F,0.00000000F,0.10471976F,-0.08726646F));
        var mask_pleat_right2 = head.addOrReplaceChild("mask_pleat_right2", CubeListBuilder.create()
            .texOffs(96,64).addBox(-1.65000000F,0.00000000F,-0.10000000F,3.30000000F,0.19000000F,0.23000000F)
            , PartPose.offsetAndRotation(-1.72000000F,-1.07000000F,-4.41000000F,0.00000000F,0.10471976F,-0.08726646F));
        var mask_cheek_left = head.addOrReplaceChild("mask_cheek_left", CubeListBuilder.create()
            .texOffs(96,64).addBox(-1.87000000F,-1.35000000F,-0.12000000F,3.74000000F,2.70000000F,0.26000000F)
            , PartPose.offsetAndRotation(2.00000000F,-1.60000000F,-4.16000000F,0.00000000F,-0.13962634F,0.06981317F));
        var mask_pleat_left0 = head.addOrReplaceChild("mask_pleat_left0", CubeListBuilder.create()
            .texOffs(64,96).addBox(-1.65000000F,0.00000000F,-0.10000000F,3.30000000F,0.19000000F,0.23000000F)
            , PartPose.offsetAndRotation(1.72000000F,-2.65000000F,-4.46000000F,0.00000000F,-0.10471976F,0.08726646F));
        var mask_pleat_left1 = head.addOrReplaceChild("mask_pleat_left1", CubeListBuilder.create()
            .texOffs(96,64).addBox(-1.65000000F,0.00000000F,-0.10000000F,3.30000000F,0.19000000F,0.23000000F)
            , PartPose.offsetAndRotation(1.72000000F,-1.86000000F,-4.43500000F,0.00000000F,-0.10471976F,0.08726646F));
        var mask_pleat_left2 = head.addOrReplaceChild("mask_pleat_left2", CubeListBuilder.create()
            .texOffs(96,64).addBox(-1.65000000F,0.00000000F,-0.10000000F,3.30000000F,0.19000000F,0.23000000F)
            , PartPose.offsetAndRotation(1.72000000F,-1.07000000F,-4.41000000F,0.00000000F,-0.10471976F,0.08726646F));
        var mask_bridge = head.addOrReplaceChild("mask_bridge", CubeListBuilder.create()
            .texOffs(96,64).addBox(-0.63000000F,-0.25000000F,-0.37000000F,1.26000000F,1.12000000F,0.51000000F)
            , PartPose.offsetAndRotation(0.00000000F,-3.00000000F,-4.24000000F,-0.20943951F,0.00000000F,0.00000000F));
        var band_knot = head.addOrReplaceChild("band_knot", CubeListBuilder.create()
            .texOffs(32,32).addBox(-0.65000000F,-0.40000000F,-0.16000000F,1.30000000F,0.80000000F,0.55000000F)
            , PartPose.offsetAndRotation(3.65000000F,-5.90000000F,4.35000000F,0.08726646F,0.00000000F,-0.31415927F));
        var standing_collar_right = body.addOrReplaceChild("standing_collar_right", CubeListBuilder.create()
            .texOffs(64,64).addBox(-0.32000000F,0.00000000F,-0.26000000F,0.64000000F,2.05000000F,0.52000000F)
            , PartPose.offsetAndRotation(-1.70000000F,-0.70000000F,-1.65000000F,-0.13962634F,-0.31415927F,-0.20943951F));
        var standing_collar_left = body.addOrReplaceChild("standing_collar_left", CubeListBuilder.create()
            .texOffs(64,64).addBox(-0.32000000F,0.00000000F,-0.26000000F,0.64000000F,2.05000000F,0.52000000F)
            , PartPose.offsetAndRotation(1.70000000F,-0.70000000F,-1.65000000F,-0.13962634F,0.31415927F,0.20943951F));
        var scarf_overlap = body.addOrReplaceChild("scarf_overlap", CubeListBuilder.create()
            .texOffs(64,96).addBox(-2.02000000F,0.00000000F,-0.13000000F,4.04000000F,0.50000000F,0.30000000F)
            , PartPose.offsetAndRotation(0.00000000F,-0.33000000F,-2.48000000F,0.00000000F,0.00000000F,0.08726646F));
        var chest_fold = body.addOrReplaceChild("chest_fold", CubeListBuilder.create()
            .texOffs(0,96).addBox(-1.80000000F,0.00000000F,-0.22000000F,3.60000000F,0.35000000F,0.30000000F)
            , PartPose.offsetAndRotation(0.50000000F,6.60000000F,-2.12000000F,0.00000000F,0.00000000F,0.29670597F));
        var waist_fold = body.addOrReplaceChild("waist_fold", CubeListBuilder.create()
            .texOffs(0,96).addBox(-2.60000000F,0.00000000F,-0.19000000F,5.20000000F,0.27000000F,0.27000000F)
            , PartPose.offsetAndRotation(-0.60000000F,8.20000000F,-2.11000000F,0.00000000F,0.00000000F,-0.13962634F));
        var back_pleat_n1 = body.addOrReplaceChild("back_pleat_n1", CubeListBuilder.create()
            .texOffs(0,96).addBox(-0.13000000F,0.00000000F,0.00000000F,0.26000000F,6.60000000F,0.26000000F)
            , PartPose.offsetAndRotation(-2.50000000F,2.00000000F,2.01000000F,0.00000000F,0.00000000F,-0.06981317F));
        var back_pleat_1 = body.addOrReplaceChild("back_pleat_1", CubeListBuilder.create()
            .texOffs(0,96).addBox(-0.13000000F,0.00000000F,0.00000000F,0.26000000F,6.60000000F,0.26000000F)
            , PartPose.offsetAndRotation(2.50000000F,2.00000000F,2.01000000F,0.00000000F,0.00000000F,0.06981317F));
        var sash_diagonal = body.addOrReplaceChild("sash_diagonal", CubeListBuilder.create()
            .texOffs(96,64).addBox(-2.90000000F,0.00000000F,-0.11000000F,5.80000000F,0.28000000F,0.24000000F)
            , PartPose.offsetAndRotation(-0.80000000F,9.65000000F,-2.27000000F,0.00000000F,0.00000000F,-0.08726646F));
        var sash_loop_right = body.addOrReplaceChild("sash_loop_right", CubeListBuilder.create()
            .texOffs(64,96).addBox(-1.15000000F,-0.34000000F,-0.25000000F,1.35000000F,0.68000000F,0.48000000F)
            , PartPose.offsetAndRotation(3.25000000F,10.10000000F,-2.56000000F,0.00000000F,-0.13962634F,-0.47123890F));
        var sash_loop_left = body.addOrReplaceChild("sash_loop_left", CubeListBuilder.create()
            .texOffs(64,96).addBox(-0.20000000F,-0.34000000F,-0.25000000F,1.35000000F,0.68000000F,0.48000000F)
            , PartPose.offsetAndRotation(3.25000000F,10.10000000F,-2.56000000F,0.00000000F,0.13962634F,0.47123890F));
        var sash_knot_cross = body.addOrReplaceChild("sash_knot_cross", CubeListBuilder.create()
            .texOffs(96,64).addBox(-0.34000000F,-0.73000000F,-0.20000000F,0.68000000F,1.46000000F,0.40000000F)
            , PartPose.offsetAndRotation(3.22000000F,10.16000000F,-2.97000000F,0.00000000F,0.00000000F,-0.31415927F));
        var short_sash_end = body.addOrReplaceChild("short_sash_end", CubeListBuilder.create()
            .texOffs(32,32).addBox(-0.34000000F,0.00000000F,-0.10000000F,0.68000000F,3.60000000F,0.22000000F)
            , PartPose.offsetAndRotation(2.85000000F,10.75000000F,-2.72000000F,0.05235988F,0.00000000F,0.27925268F));
        var right_shoulder_front = right_arm.addOrReplaceChild("right_shoulder_front", CubeListBuilder.create()
            .texOffs(0,96).addBox(-2.08000000F,-0.04444445F,-0.12000000F,4.16000000F,1.80000000F,0.24000000F, new CubeDeformation(0, -0.04444445F, 0))
            , PartPose.offsetAndRotation(0.00000000F,-1.50000000F,-2.11000000F,-0.15707963F,0.00000000F,-0.08726646F));
        var right_shoulder_back = right_arm.addOrReplaceChild("right_shoulder_back", CubeListBuilder.create()
            .texOffs(0,96).addBox(-2.08000000F,-0.04444445F,-0.12000000F,4.16000000F,1.80000000F,0.24000000F, new CubeDeformation(0, -0.04444445F, 0))
            , PartPose.offsetAndRotation(0.00000000F,-1.50000000F,2.11000000F,0.15707963F,0.00000000F,-0.08726646F));
        var right_sleeve_fold_0 = right_arm.addOrReplaceChild("right_sleeve_fold_0", CubeListBuilder.create()
            .texOffs(0,96).addBox(-1.65000000F,-0.04296296F,-0.12000000F,3.30000000F,0.29000000F,0.26000000F, new CubeDeformation(0, -0.04296296F, 0))
            , PartPose.offsetAndRotation(-0.12000000F,0.91481483F,-2.13000000F,0.00000000F,0.00000000F,-0.15707963F));
        var right_sleeve_fold_1 = right_arm.addOrReplaceChild("right_sleeve_fold_1", CubeListBuilder.create()
            .texOffs(0,96).addBox(-1.65000000F,-0.04296296F,-0.12000000F,3.30000000F,0.29000000F,0.26000000F, new CubeDeformation(0, -0.04296296F, 0))
            , PartPose.offsetAndRotation(-0.12000000F,2.07592593F,-2.13000000F,0.00000000F,0.00000000F,0.12217305F));
        var right_elbow_fold = right_forearm.addOrReplaceChild("right_elbow_fold", CubeListBuilder.create()
            .texOffs(0,96).addBox(-1.50000000F,-0.03986667F,-0.12000000F,3.00000000F,0.26000000F,0.24000000F, new CubeDeformation(0, -0.03986667F, 0))
            , PartPose.offsetAndRotation(0.00000000F,0.52000000F,-1.96000000F,0.00000000F,0.00000000F,0.17453293F));
        var right_wrist_binding = right_forearm.addOrReplaceChild("right_wrist_binding", CubeListBuilder.create()
            .texOffs(64,96).addBox(-1.65000000F,0.00000000F,-0.10000000F,3.30000000F,0.28000000F,0.20000000F)
            , PartPose.offsetAndRotation(0.00000000F,2.85000000F,-1.82000000F,0.00000000F,0.00000000F,-0.13962634F));
        var right_shin_wrap_0 = right_shin.addOrReplaceChild("right_shin_wrap_0", CubeListBuilder.create()
            .texOffs(64,64).addBox(-1.56000000F,0.00000000F,-0.10000000F,3.12000000F,0.40000000F,0.21000000F)
            , PartPose.offsetAndRotation(0.00000000F,0.75000000F,-1.86000000F,0.00000000F,0.00000000F,-0.15707963F));
        var right_shin_wrap_1 = right_shin.addOrReplaceChild("right_shin_wrap_1", CubeListBuilder.create()
            .texOffs(64,64).addBox(-1.56000000F,0.00000000F,-0.10000000F,3.12000000F,0.40000000F,0.21000000F)
            , PartPose.offsetAndRotation(0.00000000F,2.05000000F,-1.86000000F,0.00000000F,0.00000000F,-0.15707963F));
        var left_shoulder_front = left_arm.addOrReplaceChild("left_shoulder_front", CubeListBuilder.create()
            .texOffs(0,96).addBox(-2.08000000F,-0.04444445F,-0.12000000F,4.16000000F,1.80000000F,0.24000000F, new CubeDeformation(0, -0.04444445F, 0))
            , PartPose.offsetAndRotation(0.00000000F,-1.50000000F,-2.11000000F,-0.15707963F,0.00000000F,0.08726646F));
        var left_shoulder_back = left_arm.addOrReplaceChild("left_shoulder_back", CubeListBuilder.create()
            .texOffs(0,96).addBox(-2.08000000F,-0.04444445F,-0.12000000F,4.16000000F,1.80000000F,0.24000000F, new CubeDeformation(0, -0.04444445F, 0))
            , PartPose.offsetAndRotation(0.00000000F,-1.50000000F,2.11000000F,0.15707963F,0.00000000F,0.08726646F));
        var left_sleeve_fold_0 = left_arm.addOrReplaceChild("left_sleeve_fold_0", CubeListBuilder.create()
            .texOffs(0,96).addBox(-1.65000000F,-0.04296296F,-0.12000000F,3.30000000F,0.29000000F,0.26000000F, new CubeDeformation(0, -0.04296296F, 0))
            , PartPose.offsetAndRotation(0.12000000F,0.91481483F,-2.13000000F,0.00000000F,0.00000000F,0.15707963F));
        var left_sleeve_fold_1 = left_arm.addOrReplaceChild("left_sleeve_fold_1", CubeListBuilder.create()
            .texOffs(0,96).addBox(-1.65000000F,-0.04296296F,-0.12000000F,3.30000000F,0.29000000F,0.26000000F, new CubeDeformation(0, -0.04296296F, 0))
            , PartPose.offsetAndRotation(0.12000000F,2.07592593F,-2.13000000F,0.00000000F,0.00000000F,-0.12217305F));
        var left_elbow_fold = left_forearm.addOrReplaceChild("left_elbow_fold", CubeListBuilder.create()
            .texOffs(0,96).addBox(-1.50000000F,-0.03986667F,-0.12000000F,3.00000000F,0.26000000F,0.24000000F, new CubeDeformation(0, -0.03986667F, 0))
            , PartPose.offsetAndRotation(0.00000000F,0.52000000F,-1.96000000F,0.00000000F,0.00000000F,-0.17453293F));
        var left_wrist_binding = left_forearm.addOrReplaceChild("left_wrist_binding", CubeListBuilder.create()
            .texOffs(64,96).addBox(-1.65000000F,0.00000000F,-0.10000000F,3.30000000F,0.28000000F,0.20000000F)
            , PartPose.offsetAndRotation(0.00000000F,2.85000000F,-1.82000000F,0.00000000F,0.00000000F,0.13962634F));
        var left_shin_wrap_0 = left_shin.addOrReplaceChild("left_shin_wrap_0", CubeListBuilder.create()
            .texOffs(64,64).addBox(-1.56000000F,0.00000000F,-0.10000000F,3.12000000F,0.40000000F,0.21000000F)
            , PartPose.offsetAndRotation(0.00000000F,0.75000000F,-1.86000000F,0.00000000F,0.00000000F,0.15707963F));
        var left_shin_wrap_1 = left_shin.addOrReplaceChild("left_shin_wrap_1", CubeListBuilder.create()
            .texOffs(64,64).addBox(-1.56000000F,0.00000000F,-0.10000000F,3.12000000F,0.40000000F,0.21000000F)
            , PartPose.offsetAndRotation(0.00000000F,2.05000000F,-1.86000000F,0.00000000F,0.00000000F,0.15707963F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }
}
