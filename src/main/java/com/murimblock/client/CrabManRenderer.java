package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.CrabManPatch;
import com.murimblock.mob.CrabManEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.model.Meshes.MeshAccessor;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.client.renderer.patched.entity.PatchedLivingEntityRenderer;

/**
 * Same approach as ArmoredCrabRenderer: no vanilla model, Epic Fight draws the skinned mesh from
 * RenderLivingEvent.Pre, the vanilla renderer only supplies the texture and an empty model.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CrabManRenderer extends MobRenderer<CrabManEntity, CrabManRenderer.EmptyModel> {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/crab_man.png");
    static final MeshAccessor<SkinnedMesh> MESH =
            MeshAccessor.create(Murimblock.MOD_ID, "entity/crab_man", loader -> loader.loadSkinnedMesh(SkinnedMesh::new));
    private static boolean patchedRendererHooked;

    public CrabManRenderer(EntityRendererProvider.Context context) {
        super(context, new EmptyModel(), 0.5F);
    }

    @Override public ResourceLocation getTextureLocation(CrabManEntity entity) {
        return TEXTURE;
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.CRAB_MAN.get(), CrabManRenderer::new);
        // Epic Fight re-posts this hook on every resource reload; the listener itself is added once.
        if (!patchedRendererHooked) {
            patchedRendererHooked = true;
            EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(added ->
                    added.addPatchedEntityRenderer(MurimEntities.CRAB_MAN.get(),
                            type -> new Patched(added.getContext(), type).initLayerLast(added.getContext(), type)));
        }
    }

    public static final class EmptyModel extends EntityModel<CrabManEntity> {
        @Override public void setupAnim(CrabManEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) { }
        @Override public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) { }
    }

    static final class Patched extends PatchedLivingEntityRenderer<CrabManEntity, CrabManPatch, EmptyModel, CrabManRenderer, SkinnedMesh> {
        Patched(EntityRendererProvider.Context context, EntityType<?> type) {
            super(context, type);
        }

        @Override public AssetAccessor<SkinnedMesh> getDefaultMesh() {
            return MESH;
        }
    }
}
