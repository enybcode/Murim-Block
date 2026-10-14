package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.AzureSenseiPatch;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.mob.AzureSenseiEntity;
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
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;

/**
 * Sensei Yun: Epic Fight draws him with his own 3D model on the biped skeleton (animmodels/entity/azure_sensei, a
 * HumanoidMesh: straw hat, beard, wide sleeves, sash, split skirt, the scabbard on his back; texture
 * textures/entity/azure_sensei.png), both from scripts/animation/sensei-3d.cjs, the Azure Cloud in his hand. One atlas
 * per face (client/MobFaces). He has no flat skin: without Epic Fight's renderer the player-shaped fallback shows his
 * atlas, not meant for it.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class AzureSenseiRenderer extends HumanoidMobRenderer<AzureSenseiEntity, PlayerModel<AzureSenseiEntity>> {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/azure_sensei.png");
    /** One atlas per face (MobFaces.Face order): azure_sensei.png, then azure_sensei_<face>.png. */
    private static final ResourceLocation[] FACES = new ResourceLocation[MobFaces.Face.values().length];
    /** His face now, picked while Epic Fight draws him. Render thread only. */
    private static MobFaces.Face face = MobFaces.Face.CALM;

    static {
        for (MobFaces.Face f : MobFaces.Face.values())
            FACES[f.ordinal()] = f.suffix.isEmpty() ? TEXTURE : ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/azure_sensei_" + f.suffix + ".png");
    }
    static final MeshAccessor<HumanoidMesh> MESH =
            MeshAccessor.create(Murimblock.MOD_ID, "entity/azure_sensei", loader -> loader.loadSkinnedMesh(HumanoidMesh::new));
    private static boolean patchedRendererHooked;

    public AzureSenseiRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override public ResourceLocation getTextureLocation(AzureSenseiEntity sensei) {
        return FACES[face.ordinal()];
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.AZURE_SENSEI.get(), AzureSenseiRenderer::new);
        // Epic Fight re-posts this hook on every resource reload; the listener itself is added once.
        if (!patchedRendererHooked) {
            patchedRendererHooked = true;
            EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(added ->
                    added.addPatchedEntityRenderer(MurimEntities.AZURE_SENSEI.get(), type ->
                            new Patched(added.getContext(), type).initLayerLast(added.getContext(), type)));
        }
    }

    /** Epic Fight's humanoid renderer on the sensei's mesh, with his face of the moment. */
    static final class Patched extends PHumanoidRenderer<AzureSenseiEntity, AzureSenseiPatch, PlayerModel<AzureSenseiEntity>, AzureSenseiRenderer, HumanoidMesh> {
        Patched(EntityRendererProvider.Context context, EntityType<?> type) {
            super(MESH, context, type);
        }

        @Override
        public void render(AzureSenseiEntity sensei, AzureSenseiPatch patch, AzureSenseiRenderer renderer, MultiBufferSource buffers,
                           PoseStack poseStack, int light, float partialTicks) {
            IdlePhase.desync(sensei, patch, MurimAnimations.SENSEI_IDLE);
            face = MobFaces.of(sensei, patch);
            try {
                super.render(sensei, patch, renderer, buffers, poseStack, light, partialTicks);
            } finally {
                face = MobFaces.Face.CALM;
            }
        }
    }
}
