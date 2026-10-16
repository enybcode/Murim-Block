package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.GoldenDragonSagePatch;
import com.murimblock.mob.GoldenDragonSageEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
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
 * The Golden Dragon Sage: Epic Fight draws him with his own 3D model on the biped skeleton (animmodels/entity/golden_dragon_sage,
 * a HumanoidMesh: gold crown, long white hair and beard, deep jade robe edged in white gold, a gold dragon on the back),
 * from scripts/animation/golden-dragon-sage-3d.cjs. Without Epic Fight's renderer the player-shaped fallback shows his atlas.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class GoldenDragonSageRenderer extends HumanoidMobRenderer<GoldenDragonSageEntity, PlayerModel<GoldenDragonSageEntity>> {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/golden_dragon_sage.png");
    static final MeshAccessor<HumanoidMesh> MESH =
            MeshAccessor.create(Murimblock.MOD_ID, "entity/golden_dragon_sage", loader -> loader.loadSkinnedMesh(HumanoidMesh::new));
    private static boolean patchedRendererHooked;

    public GoldenDragonSageRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override public ResourceLocation getTextureLocation(GoldenDragonSageEntity sage) {
        return TEXTURE;
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.GOLDEN_DRAGON_SAGE.get(), GoldenDragonSageRenderer::new);
        // Epic Fight re-posts this hook on every resource reload; the listener itself is added once.
        if (!patchedRendererHooked) {
            patchedRendererHooked = true;
            EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(added ->
                    added.addPatchedEntityRenderer(MurimEntities.GOLDEN_DRAGON_SAGE.get(), type ->
                            new Patched(added.getContext(), type).initLayerLast(added.getContext(), type)));
        }
    }

    /** Epic Fight's humanoid renderer on the sage's mesh. */
    static final class Patched extends PHumanoidRenderer<GoldenDragonSageEntity, GoldenDragonSagePatch, PlayerModel<GoldenDragonSageEntity>, GoldenDragonSageRenderer, HumanoidMesh> {
        Patched(EntityRendererProvider.Context context, EntityType<?> type) {
            super(MESH, context, type);
        }
    }
}
