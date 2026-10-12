package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.ArmoredCrabPatch;
import com.murimblock.mob.ArmoredCrabEntity;
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
 * The crab has no vanilla model: Epic Fight draws its skinned mesh from RenderLivingEvent.Pre, which only
 * fires for a LivingEntityRenderer. The vanilla renderer below supplies the texture and an empty model.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class ArmoredCrabRenderer extends MobRenderer<ArmoredCrabEntity, ArmoredCrabRenderer.EmptyModel> {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/armored_crab.png");
    static final MeshAccessor<SkinnedMesh> MESH =
            MeshAccessor.create(Murimblock.MOD_ID, "entity/armored_crab", loader -> loader.loadSkinnedMesh(SkinnedMesh::new));
    private static boolean patchedRendererHooked;

    public ArmoredCrabRenderer(EntityRendererProvider.Context context) {
        super(context, new EmptyModel(), 0.45F);
    }

    @Override public ResourceLocation getTextureLocation(ArmoredCrabEntity entity) {
        return TEXTURE;
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.ARMORED_CRAB.get(), ArmoredCrabRenderer::new);
        // Epic Fight re-posts this hook on every resource reload; the listener itself is added once.
        if (!patchedRendererHooked) {
            patchedRendererHooked = true;
            EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(added ->
                    added.addPatchedEntityRenderer(MurimEntities.ARMORED_CRAB.get(),
                            type -> new Patched(added.getContext(), type).initLayerLast(added.getContext(), type)));
        }
    }

    public static final class EmptyModel extends EntityModel<ArmoredCrabEntity> {
        @Override public void setupAnim(ArmoredCrabEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) { }
        @Override public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) { }
    }

    static final class Patched extends PatchedLivingEntityRenderer<ArmoredCrabEntity, ArmoredCrabPatch, EmptyModel, ArmoredCrabRenderer, SkinnedMesh> {
        Patched(EntityRendererProvider.Context context, EntityType<?> type) {
            super(context, type);
        }

        @Override public AssetAccessor<SkinnedMesh> getDefaultMesh() {
            return MESH;
        }
    }
}
