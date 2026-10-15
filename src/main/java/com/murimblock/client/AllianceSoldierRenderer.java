package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.AllianceSoldierPatch;
import com.murimblock.mob.AllianceSoldierEntity;
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
 * The Alliance's soldiers: Epic Fight draws them with their own 3D model on the biped skeleton (animmodels/entity/alliance_soldier,
 * a HumanoidMesh: robe, sashes, wraps and boots, each kind's own pieces), all from scripts/animation/alliance-soldier-3d.cjs.
 * One atlas per kind and face (textures/entity/alliance_soldier/<kind>.png, then <kind>_<face>.png; client/MobFaces).
 * They have no flat skin: without Epic Fight's renderer the player-shaped fallback shows their atlas, not meant for it.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class AllianceSoldierRenderer extends HumanoidMobRenderer<AllianceSoldierEntity, PlayerModel<AllianceSoldierEntity>> {
    /** One atlas per kind and face (Kind, then MobFaces.Face order). */
    private static final ResourceLocation[][] ATLASES = new ResourceLocation[AllianceSoldierEntity.Kind.values().length][MobFaces.Face.values().length];
    /** The face now, picked while Epic Fight draws a soldier. Render thread only. */
    private static MobFaces.Face face = MobFaces.Face.CALM;

    static {
        for (AllianceSoldierEntity.Kind kind : AllianceSoldierEntity.Kind.values())
            for (MobFaces.Face f : MobFaces.Face.values())
                ATLASES[kind.ordinal()][f.ordinal()] = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID,
                        "textures/entity/alliance_soldier/" + kind.model + (f.suffix.isEmpty() ? "" : "_" + f.suffix) + ".png");
    }
    static final MeshAccessor<HumanoidMesh> MESH =
            MeshAccessor.create(Murimblock.MOD_ID, "entity/alliance_soldier", loader -> loader.loadSkinnedMesh(HumanoidMesh::new));
    private static boolean patchedRendererHooked;

    public AllianceSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override public ResourceLocation getTextureLocation(AllianceSoldierEntity soldier) {
        return ATLASES[soldier.kind().ordinal()][face.ordinal()];
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        for (var soldier : MurimEntities.ALLIANCE_SOLDIERS) event.registerEntityRenderer(soldier.get(), AllianceSoldierRenderer::new);
        // Epic Fight re-posts this hook on every resource reload; the listener itself is added once.
        if (!patchedRendererHooked) {
            patchedRendererHooked = true;
            EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(added -> {
                for (var soldier : MurimEntities.ALLIANCE_SOLDIERS)
                    added.addPatchedEntityRenderer(soldier.get(), type -> new Patched(added.getContext(), type).initLayerLast(added.getContext(), type));
            });
        }
    }

    /** Epic Fight's humanoid renderer on the soldiers' mesh, with the face of the moment. */
    static final class Patched extends PHumanoidRenderer<AllianceSoldierEntity, AllianceSoldierPatch, PlayerModel<AllianceSoldierEntity>, AllianceSoldierRenderer, HumanoidMesh> {
        Patched(EntityRendererProvider.Context context, EntityType<?> type) {
            super(MESH, context, type);
        }

        @Override
        public void render(AllianceSoldierEntity soldier, AllianceSoldierPatch patch, AllianceSoldierRenderer renderer, MultiBufferSource buffers,
                           PoseStack poseStack, int light, float partialTicks) {
            face = MobFaces.of(soldier, patch);
            try {
                super.render(soldier, patch, renderer, buffers, poseStack, light, partialTicks);
            } finally {
                face = MobFaces.Face.CALM;
            }
        }
    }
}
