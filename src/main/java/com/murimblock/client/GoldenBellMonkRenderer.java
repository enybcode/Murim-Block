package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.GoldenBellMonkPatch;
import com.murimblock.mob.GoldenBellMonkEntity;
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
 * The Golden Bell monk: Epic Fight draws him with his own 3D model on the biped skeleton (animmodels/entity/golden_bell_monk,
 * a HumanoidMesh), all from scripts/animation/golden-bell-monk-3d.cjs. One atlas per face (client/MobFaces), and a second
 * set with his skin gold for the Golden Bell form (GoldenBellMonkEntity.golden(), synced).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class GoldenBellMonkRenderer extends HumanoidMobRenderer<GoldenBellMonkEntity, PlayerModel<GoldenBellMonkEntity>> {
    /** One atlas per face (MobFaces.Face order), his own skin then the golden form's. */
    private static final ResourceLocation[] FACES = atlases("golden_bell_monk"), GOLD_FACES = atlases("golden_bell_monk_gold");
    /** His face now, picked while Epic Fight draws him. Render thread only. */
    private static MobFaces.Face face = MobFaces.Face.CALM;

    static final MeshAccessor<HumanoidMesh> MESH =
            MeshAccessor.create(Murimblock.MOD_ID, "entity/golden_bell_monk", loader -> loader.loadSkinnedMesh(HumanoidMesh::new));
    private static boolean patchedRendererHooked;

    private static ResourceLocation[] atlases(String base) {
        ResourceLocation[] all = new ResourceLocation[MobFaces.Face.values().length];
        for (MobFaces.Face f : MobFaces.Face.values())
            all[f.ordinal()] = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/" + base + (f.suffix.isEmpty() ? "" : "_" + f.suffix) + ".png");
        return all;
    }

    public GoldenBellMonkRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override public ResourceLocation getTextureLocation(GoldenBellMonkEntity monk) {
        return (monk.golden() ? GOLD_FACES : FACES)[face.ordinal()];
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.GOLDEN_BELL_MONK.get(), GoldenBellMonkRenderer::new);
        // Epic Fight re-posts this hook on every resource reload; the listener itself is added once.
        if (!patchedRendererHooked) {
            patchedRendererHooked = true;
            EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(added ->
                    added.addPatchedEntityRenderer(MurimEntities.GOLDEN_BELL_MONK.get(), type ->
                            new Patched(added.getContext(), type).initLayerLast(added.getContext(), type)));
        }
    }

    /** Epic Fight's humanoid renderer on the monk's mesh, with his face and skin of the moment. */
    static final class Patched extends PHumanoidRenderer<GoldenBellMonkEntity, GoldenBellMonkPatch, PlayerModel<GoldenBellMonkEntity>, GoldenBellMonkRenderer, HumanoidMesh> {
        Patched(EntityRendererProvider.Context context, EntityType<?> type) {
            super(MESH, context, type);
        }

        @Override
        public void render(GoldenBellMonkEntity monk, GoldenBellMonkPatch patch, GoldenBellMonkRenderer renderer, MultiBufferSource buffers,
                           PoseStack poseStack, int light, float partialTicks) {
            face = MobFaces.of(monk, patch);
            try {
                super.render(monk, patch, renderer, buffers, poseStack, light, partialTicks);
            } finally {
                face = MobFaces.Face.CALM;
            }
        }
    }
}
