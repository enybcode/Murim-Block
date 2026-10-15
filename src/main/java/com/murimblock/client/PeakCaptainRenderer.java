package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.PeakCaptainPatch;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.PeakCaptainEntity;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
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
 * The Captain of the Peak Guard: Epic Fight draws him with his own 3D model on the biped skeleton
 * (animmodels/entity/peak_captain, a HumanoidMesh, the cloth only), one atlas per face (textures/entity/peak_captain/,
 * client/MobFaces), all from scripts/animation/peak-captain-3d.cjs. His cape, sash and pendant's tassel swing
 * (ClothLayer, cloth/peak_captain.json). The armour he wears is drawn as on a player (Minecraft's armour layer, which
 * Epic Fight turns into each piece's own 3D mesh), the spear in his hands as any held item.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class PeakCaptainRenderer extends HumanoidMobRenderer<PeakCaptainEntity, PlayerModel<PeakCaptainEntity>> {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/peak_captain/peak_captain.png");
    /** One atlas per face (MobFaces.Face order): peak_captain.png, then peak_captain_<face>.png. */
    private static final ResourceLocation[] FACES = new ResourceLocation[MobFaces.Face.values().length];
    /** His face now, picked while Epic Fight draws him. Render thread only. */
    private static MobFaces.Face face = MobFaces.Face.CALM;

    static {
        for (MobFaces.Face f : MobFaces.Face.values())
            FACES[f.ordinal()] = f.suffix.isEmpty() ? TEXTURE
                    : ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/peak_captain/peak_captain_" + f.suffix + ".png");
    }
    static final MeshAccessor<HumanoidMesh> MESH =
            MeshAccessor.create(Murimblock.MOD_ID, "entity/peak_captain", loader -> loader.loadSkinnedMesh(HumanoidMesh::new));
    private static boolean patchedRendererHooked;

    public PeakCaptainRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
    }

    @Override public ResourceLocation getTextureLocation(PeakCaptainEntity captain) {
        return FACES[face.ordinal()];
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.PEAK_CAPTAIN.get(), PeakCaptainRenderer::new);
        // Epic Fight re-posts this hook on every resource reload; the listener itself is added once.
        if (!patchedRendererHooked) {
            patchedRendererHooked = true;
            EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(added ->
                    added.addPatchedEntityRenderer(MurimEntities.PEAK_CAPTAIN.get(), type -> {
                        Patched patched = new Patched(added.getContext(), type);
                        patched.initLayerLast(added.getContext(), type);
                        patched.addCustomLayer(new ClothLayer<>("peak_captain", captain -> TEXTURE));
                        return patched;
                    }));
        }
    }

    /** Epic Fight's humanoid renderer on the captain's mesh, with the face of the moment. */
    static final class Patched extends PHumanoidRenderer<PeakCaptainEntity, PeakCaptainPatch, PlayerModel<PeakCaptainEntity>, PeakCaptainRenderer, HumanoidMesh> {
        Patched(EntityRendererProvider.Context context, EntityType<?> type) {
            super(MESH, context, type);
        }

        @Override
        public void render(PeakCaptainEntity captain, PeakCaptainPatch patch, PeakCaptainRenderer renderer, MultiBufferSource buffers,
                           PoseStack poseStack, int light, float partialTicks) {
            face = MobFaces.of(captain, patch);
            try {
                super.render(captain, patch, renderer, buffers, poseStack, light, partialTicks);
            } finally {
                face = MobFaces.Face.CALM;
            }
        }
    }
}
