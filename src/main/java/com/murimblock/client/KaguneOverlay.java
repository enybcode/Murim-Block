package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.martial.KaguneState;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

/**
 * The Devouring Demon Art's kagune on the player's skin, like the Sanguine Art's blood crystals: an overlay carried by
 * the Epic Fight skeleton, no armour and no item (docs/combat/styles/devouring-demon-art.md). The tentacles and the
 * centipedes ride the Torso joint, the mask the Head joint. The model and its animations are the Blockbench mock-up's
 * (KaguneModel): idle_hunger for the four tentacles, transform when the bar fills, then idle_demon.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class KaguneOverlay {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/kagune_forms.png");
    /** Blockbench draws in pixels with the player's right arm at -x; Epic Fight's model space is in blocks, right arm at +x. */
    private static final float PIXEL = 1 / 16F;

    private KaguneOverlay() { }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new Anchor(renderer));
        }
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        EpicFightClientEventHooks.Registry.MODIFY_PATCHED_ENTITY.registerEvent(e -> {
            if (e.get(EntityType.PLAYER) instanceof PPlayerRenderer third)
                third.addPatchedLayer(Anchor.class, new Layer<AbstractClientPlayer, AbstractClientPlayerPatch<AbstractClientPlayer>>());
        }, "murimblock:kagune_overlay", 0);
    }

    /** Epic Fight keys its patched layers on a vanilla layer's class: this one draws nothing by itself. */
    public static final class Anchor extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        Anchor(PlayerRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack stack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                           float limb, float amount, float partial, float age, float headYaw, float headPitch) { }
    }

    /** Which animation plays now, and how far into it (s): the transformation once, then the demon's idle. */
    static String animationOf(KaguneState state, long now, float partial, float[] time) {
        float t = (now - state.since() + partial) / 20F;
        var model = KaguneModel.get();
        return switch (state.form()) {
            case HUNGER -> { time[0] = t; yield "idle_hunger"; }
            case TRANSFORM -> {
                float length = model.length("transform");
                if (t < length) { time[0] = t; yield "transform"; }
                time[0] = t - length;
                yield "idle_demon";
            }
            case DEMON -> { time[0] = t; yield "idle_demon"; }
            default -> null;
        };
    }

    private static final class Layer<E extends AbstractClientPlayer, P extends AbstractClientPlayerPatch<E>>
            extends PatchedLayer<E, P, PlayerModel<E>, RenderLayer<E, PlayerModel<E>>> {
        @Override
        protected void renderLayer(P patch, E player, RenderLayer<E, PlayerModel<E>> original, PoseStack stack,
                                   MultiBufferSource buffers, int light, OpenMatrix4f[] poses, float age, float headYaw, float headPitch, float partial) {
            if (player.isInvisible() || player.isSpectator()) return;
            var state = KaguneState.of(player);
            if (state.form() == KaguneState.Form.NONE) return;
            float[] time = new float[1];
            String animation = animationOf(state, player.level().getGameTime(), partial, time);
            if (animation == null) return;
            var model = KaguneModel.get();
            var out = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            var armature = patch.getArmature();
            // The tentacles and the centipedes, on the Torso joint. A joint's pose is the joint's own frame: times its
            // inverse bind, it maps the bind pose's model space (the player standing, feet at the origin), as the model is drawn.
            var torsoJoint = armature.searchJointByName("Torso");
            var torsoPose = OpenMatrix4f.mulMatrices(poses[torsoJoint.getId()], torsoJoint.getToOrigin());
            for (String root : new String[]{"kagune", "kakuja"}) {
                int bone = model.bone(root);
                if (bone < 0) continue;
                stack.pushPose();
                MathUtils.mulStack(stack, torsoPose);
                stack.scale(-PIXEL, PIXEL, PIXEL);
                model.toParentAtRest(bone, stack);
                model.draw(bone, animation, time[0], stack, out, light, OverlayTexture.NO_OVERLAY);
                stack.popPose();
            }
            // The mask, on the Head joint.
            int mask = model.bone("mask_group");
            if (mask >= 0) {
                stack.pushPose();
                var head = armature.searchJointByName("Head");
                MathUtils.mulStack(stack, OpenMatrix4f.mulMatrices(poses[head.getId()], head.getToOrigin()));
                stack.scale(-PIXEL, PIXEL, PIXEL);
                model.toParentAtRest(mask, stack);
                model.draw(mask, animation, time[0], stack, out, light, OverlayTexture.NO_OVERLAY);
                stack.popPose();
            }
        }
    }
}
