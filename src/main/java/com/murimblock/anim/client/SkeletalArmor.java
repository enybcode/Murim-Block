package com.murimblock.anim.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.anim.SkinnedMesh;
import com.murimblock.anim.SkinnedModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.joml.Matrix4f;

/**
 * Worn armor on the skinned biped, the way vanilla's armor layer draws it: each material layer with its texture and
 * dye, the trim from the trims atlas, the enchantment glint. The armor parts of the model follow the skeleton like the
 * body, so a bent arm bends its sleeve of mail too. Armor that brings its own model (other mods) is not drawn yet.
 */
final class SkeletalArmor {
    /** The armor parts each slot shows, as vanilla's armor layer makes them visible. */
    private static final Map<EquipmentSlot, List<String>> PARTS = Map.of(
            EquipmentSlot.HEAD, List.of("armor_head", "armor_hat"),
            EquipmentSlot.CHEST, List.of("armor_body", "armor_arm_r", "armor_arm_l"),
            EquipmentSlot.LEGS, List.of("armor_inner_body", "armor_inner_leg_r", "armor_inner_leg_l"),
            EquipmentSlot.FEET, List.of("armor_leg_r", "armor_leg_l"));
    /** Vanilla's order. */
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.HEAD};

    private SkeletalArmor() {
    }

    /** {@code matrix} is the model's space in pixels. */
    static void render(PlayerRenderer renderer, AbstractClientPlayer player, SkinnedModel model, Matrix4f[] skin,
                       PoseStack.Pose matrix, MultiBufferSource buffers, int light) {
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot
                    || VanillaLayers.customArmor(renderer, player, slot)) {
                continue;
            }
            List<SkinnedMesh> parts = new ArrayList<>();
            for (String name : PARTS.get(slot)) {
                SkinnedMesh part = model.part(name);
                if (part != null) {
                    parts.add(part);
                }
            }
            boolean inner = slot == EquipmentSlot.LEGS;
            ArmorMaterial material = armor.getMaterial().value();
            IClientItemExtensions extensions = IClientItemExtensions.of(stack);
            int fallbackColor = extensions.getDefaultDyeColor(stack);
            for (int i = 0; i < material.layers().size(); i++) {
                ArmorMaterial.Layer layer = material.layers().get(i);
                int color = extensions.getArmorLayerTintColor(stack, player, layer, i, fallbackColor);
                if (color != 0) {
                    ResourceLocation texture = ClientHooks.getArmorTexture(player, stack, layer, inner, slot);
                    draw(parts, skin, matrix, buffers.getBuffer(RenderType.armorCutoutNoCull(texture)), color, light);
                }
            }
            ArmorTrim trim = stack.get(DataComponents.TRIM);
            if (trim != null) {
                TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET)
                        .getSprite(inner ? trim.innerTexture(armor.getMaterial()) : trim.outerTexture(armor.getMaterial()));
                draw(parts, skin, matrix, sprite.wrap(buffers.getBuffer(Sheets.armorTrimsSheet(trim.pattern().value().decal()))),
                        0xFFFFFFFF, light);
            }
            if (stack.hasFoil()) {
                draw(parts, skin, matrix, buffers.getBuffer(RenderType.armorEntityGlint()), 0xFFFFFFFF, light);
            }
        }
    }

    private static void draw(List<SkinnedMesh> parts, Matrix4f[] skin, PoseStack.Pose matrix, VertexConsumer consumer,
                             int color, int light) {
        for (SkinnedMesh part : parts) {
            SkeletalPlayerRenderer.draw(part, skin, matrix, consumer, color, light, OverlayTexture.NO_OVERLAY);
        }
    }
}
