package com.murimblock.bloodsect.ninja.client;

import com.murimblock.Murimblock;
import com.murimblock.bloodsect.ninja.BloodNinja;
import com.murimblock.item.MurimItems;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

/**
 * The Blood Lacquer on a Blood Soldier: each piece it wears (a piece it has lost is not drawn), in 3D on the cultists' posed
 * parts, from the same atlas as the player's Epic Fight armour meshes (scripts/animation/blood-soldier-game.cjs).
 */
public final class SoldierArmourLayer extends RenderLayer<BloodNinja, ReferenceNinjaModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/models/armor/blood_lacquer_3d.png");
    /** Each slot, the piece of the set it holds and its key in blood_soldier.json. */
    private static final Map<EquipmentSlot, String> SLOTS = Map.of(EquipmentSlot.HEAD, "head", EquipmentSlot.CHEST, "chest",
            EquipmentSlot.LEGS, "legs", EquipmentSlot.FEET, "feet");

    public SoldierArmourLayer(RenderLayerParent<BloodNinja, ReferenceNinjaModel> parent) {
        super(parent);
    }

    private static Item piece(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> MurimItems.BLOOD_LACQUER_MASK.get();
            case CHEST -> MurimItems.BLOOD_LACQUER_CUIRASS.get();
            case LEGS -> MurimItems.BLOOD_LACQUER_TASSETS.get();
            default -> MurimItems.BLOOD_LACQUER_GREAVES.get();
        };
    }

    @Override
    public void render(PoseStack stack, MultiBufferSource buffers, int light, BloodNinja soldier, float limbSwing, float limbAmount,
                       float partialTick, float age, float yaw, float pitch) {
        CultistMesh mesh = CultistMesh.soldier();
        if (mesh == null || soldier.isInvisible()) return;
        var buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int overlay = LivingEntityRenderer.getOverlayCoords(soldier, 0);
        for (var e : SLOTS.entrySet()) {
            if (!soldier.getItemBySlot(e.getKey()).is(piece(e.getKey()))) continue;
            getParentModel().renderQuads(part -> mesh.armour(e.getValue(), part), stack, buffer, light, overlay, -1);
        }
    }
}
