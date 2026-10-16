package com.murimblock.bloodsect.ninja.client;

import com.murimblock.bloodsect.ninja.BloodNinja;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * The Blood Soldier (BloodSoldier): the cultists' model and moves with the soldiers' look (CultistMesh.soldier), and the
 * pieces of the Blood Lacquer it wears drawn in 3D over it (SoldierArmourLayer) in place of Minecraft's flat armour.
 */
public final class BloodSoldierRenderer extends HumanoidMobRenderer<BloodNinja, ReferenceNinjaModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(com.murimblock.bloodsect.BloodSect.ID, "textures/entity/blood_ninja_reference.png");

    public BloodSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new ReferenceNinjaModel(context.bakeLayer(ReferenceNinjaModel.LAYER), CultistMesh::soldier), .4F);
        this.layers.removeIf(layer -> layer instanceof ItemInHandLayer || layer instanceof HumanoidArmorLayer);
        this.addLayer(new SoldierArmourLayer(this));
        this.addLayer(new AnimatedBloodDaoLayer(this, context));
    }

    /** Its outfit's atlas, a blink for three ticks every four seconds or so. */
    @Override public ResourceLocation getTextureLocation(BloodNinja soldier) {
        CultistMesh mesh = CultistMesh.soldier();
        if (mesh == null) return TEXTURE;
        boolean blink = Math.floorMod(soldier.tickCount + soldier.getId() * 37, 83) < 3;
        return mesh.texture(soldier.getOutfit(), blink);
    }
}
