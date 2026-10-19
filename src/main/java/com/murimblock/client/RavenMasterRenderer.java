package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.RavenMasterPatch;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.mob.RavenMasterEntity;
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
 * The Raven Master: Epic Fight draws him with his own 3D model on the biped skeleton (animmodels/entity/raven_master, a
 * HumanoidMesh: the crow-beaked lacquer half mask, long white hair, feather pauldrons, the blue-black robe and its
 * tabard, his shadow raven on his left shoulder; texture textures/entity/raven_master.png), both from
 * scripts/animation/raven-master-3d.cjs (game), the iron fan in his hand (client/FanRenderer). One atlas per face
 * (client/MobFaces: under the mask, only the mouth shows them). He has no flat skin: without Epic Fight's renderer the
 * player-shaped fallback shows his atlas, not meant for it.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class RavenMasterRenderer extends HumanoidMobRenderer<RavenMasterEntity, PlayerModel<RavenMasterEntity>> {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/raven_master.png");
    /** One atlas per face (MobFaces.Face order): raven_master.png, then raven_master_<face>.png. */
    private static final ResourceLocation[] FACES = new ResourceLocation[MobFaces.Face.values().length];
    /** His face now, picked while Epic Fight draws him. Render thread only. */
    private static MobFaces.Face face = MobFaces.Face.CALM;

    static {
        for (MobFaces.Face f : MobFaces.Face.values())
            FACES[f.ordinal()] = f.suffix.isEmpty() ? TEXTURE : ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/raven_master_" + f.suffix + ".png");
    }
    static final MeshAccessor<HumanoidMesh> MESH =
            MeshAccessor.create(Murimblock.MOD_ID, "entity/raven_master", loader -> loader.loadSkinnedMesh(HumanoidMesh::new));
    private static boolean patchedRendererHooked;

    public RavenMasterRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override public ResourceLocation getTextureLocation(RavenMasterEntity master) {
        return FACES[face.ordinal()];
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.RAVEN_MASTER.get(), RavenMasterRenderer::new);
        // Epic Fight re-posts this hook on every resource reload; the listener itself is added once.
        if (!patchedRendererHooked) {
            patchedRendererHooked = true;
            EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(added ->
                    added.addPatchedEntityRenderer(MurimEntities.RAVEN_MASTER.get(), type ->
                            new Patched(added.getContext(), type).initLayerLast(added.getContext(), type)));
        }
    }

    /** Epic Fight's humanoid renderer on the master's mesh, with his face of the moment. */
    static final class Patched extends PHumanoidRenderer<RavenMasterEntity, RavenMasterPatch, PlayerModel<RavenMasterEntity>, RavenMasterRenderer, HumanoidMesh> {
        Patched(EntityRendererProvider.Context context, EntityType<?> type) {
            super(MESH, context, type);
        }

        @Override
        public void render(RavenMasterEntity master, RavenMasterPatch patch, RavenMasterRenderer renderer, MultiBufferSource buffers,
                           PoseStack poseStack, int light, float partialTicks) {
            IdlePhase.desync(master, patch, MurimAnimations.RAVEN_MASTER_IDLE);
            face = MobFaces.of(master, patch);
            try {
                super.render(master, patch, renderer, buffers, poseStack, light, partialTicks);
            } finally {
                face = MobFaces.Face.CALM;
            }
        }
    }
}
