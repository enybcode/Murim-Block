package com.murimblock.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.shenfa.client.mob.MobShenfa;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mobs on Shenfa: right after vanilla poses a mob's model for this frame, Shenfa lays its layers on that pose (an
 * attack, a hit reaction), before the model and its layers (armor, held items, eyes) are drawn. No event sits there.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Shadow
    protected EntityModel<?> model;

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",
            shift = At.Shift.AFTER))
    private void murimblock$afterSetupAnim(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack,
                                           MultiBufferSource buffers, int light, CallbackInfo info) {
        MobShenfa.afterSetupAnim(entity, model, partialTick);
    }
}
