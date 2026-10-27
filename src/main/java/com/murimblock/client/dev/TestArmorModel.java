package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * For the engine test only: the turtle helmet gets a model of its own (vanilla's armor shape, as a separate model),
 * the way other mods give their armor custom models, so the test can check such armor is drawn.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
final class TestArmorModel {
    private static HumanoidModel<LivingEntity> model;

    private TestArmorModel() {
    }

    @SubscribeEvent
    public static void register(RegisterClientExtensionsEvent event) {
        if (!Boolean.getBoolean("murimblock.engineTest")) {
            return;
        }
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot,
                                                          HumanoidModel<?> original) {
                if (model == null) {
                    model = new HumanoidModel<>(Minecraft.getInstance().getEntityModels()
                            .bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
                }
                return model;
            }
        }, Items.TURTLE_HELMET);
    }
}
