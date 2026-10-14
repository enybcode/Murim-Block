package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.MountainBanditPatch;
import com.murimblock.mob.MountainBanditEntity;
import com.murimblock.mob.MurimEntities;
import com.mojang.blaze3d.vertex.PoseStack;
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
import yesman.epicfight.api.client.model.Meshes.MeshAccessor;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;

/**
 * The Mountain Bandit. Epic Fight draws it with its own 3D model on the biped skeleton (animmodels/entity/mountain_bandit,
 * a HumanoidMesh: collar, sash and its tails, robe skirt, sleeves, wraps, and each outfit's pieces), one atlas per
 * outfit (textures/entity/mountain_bandit/3d/<outfit>.png), both from scripts/animation/bandit-3d.cjs; the dao in its
 * hand. Without Epic Fight's renderer it falls back to a player-shaped body with a 64x64 skin per outfit
 * (textures/entity/mountain_bandit/<outfit>.png, painted by scripts/worldgen/BanditSkins.java).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MountainBanditRenderer extends HumanoidMobRenderer<MountainBanditEntity, PlayerModel<MountainBanditEntity>> {
    private static final ResourceLocation[] SKINS = new ResourceLocation[MountainBanditEntity.OUTFITS.length];
    private static final ResourceLocation[] ATLASES = new ResourceLocation[MountainBanditEntity.OUTFITS.length];
    static final MeshAccessor<HumanoidMesh> MESH =
            MeshAccessor.create(Murimblock.MOD_ID, "entity/mountain_bandit", loader -> loader.loadSkinnedMesh(HumanoidMesh::new));
    private static boolean patchedRendererHooked;
    /** Set while Epic Fight draws a bandit: its 3D model takes the atlas, not the skin. Render thread only. */
    private static boolean drawingMesh;

    static {
        for (int i = 0; i < SKINS.length; i++) {
            SKINS[i] = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/mountain_bandit/" + MountainBanditEntity.OUTFITS[i] + ".png");
            ATLASES[i] = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/mountain_bandit/3d/" + MountainBanditEntity.OUTFITS[i] + ".png");
        }
    }

    public MountainBanditRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override public ResourceLocation getTextureLocation(MountainBanditEntity bandit) {
        return (drawingMesh ? ATLASES : SKINS)[bandit.getVariant()];
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.MOUNTAIN_BANDIT.get(), MountainBanditRenderer::new);
        // Epic Fight re-posts this hook on every resource reload; the listener itself is added once.
        if (!patchedRendererHooked) {
            patchedRendererHooked = true;
            EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(added ->
                    added.addPatchedEntityRenderer(MurimEntities.MOUNTAIN_BANDIT.get(), type ->
                            new Patched(added.getContext(), type).initLayerLast(added.getContext(), type)));
        }
    }

    /** Epic Fight's humanoid renderer on the bandit's own mesh; it marks the draw so the atlas is used. */
    static final class Patched extends PHumanoidRenderer<MountainBanditEntity, MountainBanditPatch, PlayerModel<MountainBanditEntity>, MountainBanditRenderer, HumanoidMesh> {
        Patched(EntityRendererProvider.Context context, EntityType<?> type) {
            super(MESH, context, type);
        }

        @Override
        public void render(MountainBanditEntity bandit, MountainBanditPatch patch, MountainBanditRenderer renderer, MultiBufferSource buffers,
                           PoseStack poseStack, int light, float partialTicks) {
            drawingMesh = true;
            try {
                super.render(bandit, patch, renderer, buffers, poseStack, light, partialTicks);
            } finally {
                drawingMesh = false;
            }
        }
    }
}
