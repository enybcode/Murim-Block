package com.murimblock.shenfa.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.murimblock.shenfa.Pose;
import com.murimblock.shenfa.SkinnedMesh;
import com.murimblock.shenfa.SkinnedModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Draws a mob with its look ({@link SkeletalMobs}) instead of its vanilla model: the skinned model posed by Shenfa,
 * its glowing parts, worn armor (the biped's armor parts, on the same skeleton) and held items at the grip joints.
 * Vanilla's renderer still draws it when our looks are off or the model is missing; the shadow, fire, name tag and
 * hitbox stay vanilla's.
 */
public final class SkeletalMobRenderer<T extends Mob> extends EntityRenderer<T> {
    private static final float PIXEL = 1 / 16f;
    private final EntityRenderer<? super T> vanilla;

    public SkeletalMobRenderer(EntityRendererProvider.Context context, EntityRenderer<? super T> vanilla) {
        super(context);
        this.vanilla = vanilla;
        shadowRadius = 0.5f;
    }

    @Override
    public ResourceLocation getTextureLocation(T mob) {
        SkeletalMobs.Look look = SkeletalMobs.look(mob);
        return look != null ? look.texture() : vanilla.getTextureLocation(mob);
    }

    @Override
    public void render(T mob, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        SkeletalMobs.Look look = SkeletalMobs.look(mob);
        SkinnedModel model = look == null ? null : SkeletalModels.get(look.model());
        if (model == null) {
            vanilla.render(mob, yaw, partialTick, poseStack, buffers, light);
            return;
        }
        float bodyYaw = Mth.rotLerp(partialTick, mob.yBodyRotO, mob.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, mob.yHeadRotO, mob.yHeadRot);
        float pitch = Mth.lerp(partialTick, mob.xRotO, mob.getXRot());
        Pose pose = SkeletalMobs.pose(mob, partialTick, Mth.wrapDegrees(headYaw - bodyYaw), pitch);
        Matrix4f[] skin = pose.toSkinMatrices(model.armature());
        Matrix4f[] joints = pose.toModelMatrices(model.armature());
        boolean spirit = look == SkeletalMobs.ESPRIT;
        if (spirit && mob.deathTime >= SpiritRemains.RELEASE) {
            // The Esprit's armour has fallen from it: nothing is left to draw.
            if (!SpiritRemains.released(mob)) {
                SpiritRemains.release(mob, model, skin, joints, mob.getScale() * mob.getAgeScale(), bodyYaw, partialTick);
            }
            return;
        }
        // Where the dying Esprit's mist has come undone, from the bottom up.
        float cut = spirit ? SpiritRemains.cut(mob, partialTick) : -Float.MAX_VALUE;

        poseStack.pushPose();
        // getScale leaves out a baby's half size: vanilla's models shrink it themselves.
        float scale = mob.getScale() * mob.getAgeScale();
        poseStack.scale(scale, scale, scale);
        if (mob instanceof net.minecraft.world.entity.monster.CaveSpider) {
            // Vanilla's cave spider is a smaller spider (CaveSpiderRenderer).
            poseStack.scale(0.7f, 0.7f, 0.7f);
        }
        if (mob instanceof net.minecraft.world.entity.monster.Creeper creeper) {
            // Vanilla's fuse: the body swells and wobbles as it is about to burst (CreeperRenderer.scale).
            float swell = creeper.getSwelling(partialTick);
            float wobble = 1 + Mth.sin(swell * 100) * swell * 0.01f;
            swell = (float) Math.pow(Mth.clamp(swell, 0, 1), 3);
            poseStack.scale((1 + swell * 0.4f) * wobble, (1 + swell * 0.1f) / wobble, (1 + swell * 0.4f) * wobble);
        }
        if (mob.isFullyFrozen()) {
            bodyYaw += (float) (Math.cos(mob.tickCount * 3.25) * Math.PI * 0.4f);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        if (mob.deathTime > 0 && !SkeletalMobs.diesByClip(mob)) {
            // Vanilla's fall to the side when dying (a look with its own death clip falls by it).
            float fall = Mth.sqrt((mob.deathTime + partialTick - 1) / 20f * 1.6f);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-Math.min(fall, 1) * 90));
        }
        poseStack.translate(0, 0.001f, 0);
        // Cloth, armor and held items hang on the biped's joints: a look on a skeleton of its own has none yet.
        boolean biped = model.armature().find(com.murimblock.shenfa.Biped.CHEST) != null;
        if (biped && !mob.isInvisible() && cut < 2) {
            MobCloth.render(mob, look, poseStack.last().pose(), joints, scale, buffers, light,
                    LivingEntityRenderer.getOverlayCoords(mob, white(mob, partialTick)));
        }

        Minecraft mc = Minecraft.getInstance();
        boolean visible = !mob.isInvisible();
        boolean ghost = !visible && mc.player != null && !mob.isInvisibleTo(mc.player);
        RenderType type = visible ? SkeletalMobs.translucent(look) ? RenderType.entityTranslucent(look.texture())
                : RenderType.entityCutoutNoCull(look.texture())
                : ghost ? RenderType.itemEntityTranslucentCull(look.texture())
                : mc.shouldEntityAppearGlowing(mob) ? RenderType.outline(look.texture()) : null;
        poseStack.pushPose();
        poseStack.scale(PIXEL, PIXEL, PIXEL);
        if (type != null) {
            // The dying Esprit lights up instead of reddening.
            int overlay = spirit && mob.deathTime > 0 ? OverlayTexture.NO_OVERLAY
                    : LivingEntityRenderer.getOverlayCoords(mob, white(mob, partialTick));
            draw(model, skin, poseStack, buffers.getBuffer(type), ghost ? 0x26FFFFFF : 0xFFFFFFFF, light, overlay, cut);
            if (visible && look.glow() != null && SkeletalMobs.glows(mob, look)) {
                draw(model, skin, poseStack, buffers.getBuffer(RenderType.eyes(look.glow())), 0xFFFFFFFF, light,
                        OverlayTexture.NO_OVERLAY, cut);
            }
        }
        SkinnedModel armor = SkeletalModels.get(SkeletalModels.BIPED);
        if (biped && visible && armor != null) {
            SkeletalArmor.render(mob, slot -> false, armor, skin, poseStack.last(), buffers, light, name -> true);
        }
        poseStack.popPose();
        if (biped && visible) {
            if (!SkeletalMobs.handEmpty(mob)) {
                holdItem(mob, HumanoidArm.RIGHT, joints[model.armature().get("Tool_R").index()], poseStack, buffers,
                        light);
            }
            holdItem(mob, HumanoidArm.LEFT, joints[model.armature().get("Tool_L").index()], poseStack, buffers, light);
        }
        poseStack.popPose();
        // The name tag, when the mob has one.
        super.render(mob, yaw, partialTick, poseStack, buffers, light);
    }

    /** Vanilla's white flashes of a creeper's fuse (CreeperRenderer.getWhiteOverlayProgress), else none. */
    private static float white(Mob mob, float partialTick) {
        if (!(mob instanceof net.minecraft.world.entity.monster.Creeper creeper)) {
            return 0;
        }
        float swell = creeper.getSwelling(partialTick);
        return (int) (swell * 10) % 2 == 0 ? 0 : Mth.clamp(swell, 0.5f, 1);
    }

    /** Draws the model's body; a dying Esprit's mist only above {@code cut}, its heart until it bursts. */
    private static void draw(SkinnedModel model, Matrix4f[] skin, PoseStack poseStack, VertexConsumer consumer,
                             int color, int light, int overlay, float cut) {
        boolean dying = cut > -Float.MAX_VALUE;
        for (SkinnedMesh part : model.parts()) {
            if (part.armor() || dying && part.name().equals("heart") && cut > 32) {
                continue;
            }
            if (dying && SpiritRemains.mist(part.name())) {
                SkeletalPlayerRenderer.drawAbove(part, skin, poseStack.last(), consumer, color, light, overlay, cut, 4);
            } else {
                SkeletalPlayerRenderer.draw(part, skin, poseStack.last(), consumer, color, light, overlay);
            }
        }
    }

    private static void holdItem(Mob mob, HumanoidArm arm, Matrix4f tool, PoseStack poseStack,
                                 MultiBufferSource buffers, int light) {
        // The main hand's item always in the right hand, even for a left-handed mob: the clips are made for it.
        ItemStack stack = arm == HumanoidArm.RIGHT ? mob.getMainHandItem() : mob.getOffhandItem();
        if (stack.isEmpty()) {
            return;
        }
        boolean left = arm == HumanoidArm.LEFT;
        poseStack.pushPose();
        Matrix4f joint = new Matrix4f().scale(PIXEL).mul(tool).scale(16);
        poseStack.mulPose(joint.mul(left ? SkeletalPlayerRenderer.LEFT_GRIP : SkeletalPlayerRenderer.RIGHT_GRIP));
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(mob, stack,
                left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                left, poseStack, buffers, light);
        poseStack.popPose();
    }
}
