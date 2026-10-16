package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.CrabKingAnimations;
import com.murimblock.integration.epicfight.CrabKingPatch;
import com.murimblock.mob.CrabKingEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.model.Meshes.MeshAccessor;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;

/**
 * The Crab King: Epic Fight draws him with his own 3D model on the biped skeleton (animmodels/entity/crab_king, a
 * HumanoidMesh; texture textures/entity/crab_king.png), both from scripts/animation/crab-king-3d.cjs, twice a man's
 * size: four blocks tall. He has no flat skin: without Epic Fight's renderer the player-shaped fallback shows his atlas, not meant for it.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CrabKingRenderer extends HumanoidMobRenderer<CrabKingEntity, PlayerModel<CrabKingEntity>> {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/crab_king.png");
    static final MeshAccessor<HumanoidMesh> MESH =
            MeshAccessor.create(Murimblock.MOD_ID, "entity/crab_king", loader -> loader.loadSkinnedMesh(HumanoidMesh::new));
    /** How much larger than a man he is drawn (his box is as much larger: MurimEntities.CRAB_KING). */
    public static final float SCALE = 2.0F;
    private static boolean patchedRendererHooked;

    public CrabKingRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 1.3F);
    }

    @Override public ResourceLocation getTextureLocation(CrabKingEntity king) {
        return TEXTURE;
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.CRAB_KING.get(), CrabKingRenderer::new);
        // Epic Fight re-posts this hook on every resource reload; the listener itself is added once.
        if (!patchedRendererHooked) {
            patchedRendererHooked = true;
            EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(added ->
                    added.addPatchedEntityRenderer(MurimEntities.CRAB_KING.get(), type ->
                            new Patched(added.getContext(), type).initLayerLast(added.getContext(), type)));
        }
    }

    /** Epic Fight's humanoid renderer on his mesh, scaled up about his feet. */
    static final class Patched extends PHumanoidRenderer<CrabKingEntity, CrabKingPatch, PlayerModel<CrabKingEntity>, CrabKingRenderer, HumanoidMesh> {
        Patched(EntityRendererProvider.Context context, EntityType<?> type) {
            super(MESH, context, type);
        }

        @Override
        public void render(CrabKingEntity king, CrabKingPatch patch, CrabKingRenderer renderer, MultiBufferSource buffers,
                           PoseStack poseStack, int light, float partialTicks) {
            // Gone into his mist (CrabKingEntity): unseen, but by spectators.
            var viewer = net.minecraft.client.Minecraft.getInstance().player;
            if (king.isInvisible() && (viewer == null || !viewer.isSpectator())) return;
            IdlePhase.desync(king, patch, CrabKingAnimations.IDLE);
            super.render(king, patch, renderer, buffers, poseStack, light, partialTicks);
        }

        @Override
        public void mulPoseStack(PoseStack poseStack, Armature armature, CrabKingEntity king, CrabKingPatch patch, float partialTicks) {
            super.mulPoseStack(poseStack, armature, king, patch, partialTicks);
            poseStack.scale(SCALE, SCALE, SCALE);
        }
    }
}
