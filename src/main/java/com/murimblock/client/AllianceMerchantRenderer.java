package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.AllianceMerchantPatch;
import com.murimblock.mob.AllianceMerchantEntity;
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
 * The Alliance Merchant: Epic Fight draws him with his own 3D model on the biped skeleton (animmodels/entity/alliance_merchant,
 * a HumanoidMesh: gauze cap, brocaded robe, wide sleeves, sash and purse; his face at 16 x 16 on a plate over the head),
 * all from scripts/animation/merchant-3d.cjs. One atlas per face (client/MobFaces). He has no flat skin: without Epic
 * Fight's renderer the player-shaped fallback shows his atlas, not meant for it.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class AllianceMerchantRenderer extends HumanoidMobRenderer<AllianceMerchantEntity, PlayerModel<AllianceMerchantEntity>> {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/alliance_merchant.png");
    /** One atlas per face (MobFaces.Face order): alliance_merchant.png, then alliance_merchant_<face>.png. */
    private static final ResourceLocation[] FACES = new ResourceLocation[MobFaces.Face.values().length];
    /** His face now, picked while Epic Fight draws him. Render thread only. */
    private static MobFaces.Face face = MobFaces.Face.CALM;

    static {
        for (MobFaces.Face f : MobFaces.Face.values())
            FACES[f.ordinal()] = f.suffix.isEmpty() ? TEXTURE : ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/alliance_merchant_" + f.suffix + ".png");
    }
    static final MeshAccessor<HumanoidMesh> MESH =
            MeshAccessor.create(Murimblock.MOD_ID, "entity/alliance_merchant", loader -> loader.loadSkinnedMesh(HumanoidMesh::new));
    private static boolean patchedRendererHooked;

    public AllianceMerchantRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override public ResourceLocation getTextureLocation(AllianceMerchantEntity merchant) {
        return FACES[face.ordinal()];
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.ALLIANCE_MERCHANT.get(), AllianceMerchantRenderer::new);
        // Epic Fight re-posts this hook on every resource reload; the listener itself is added once.
        if (!patchedRendererHooked) {
            patchedRendererHooked = true;
            EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(added ->
                    added.addPatchedEntityRenderer(MurimEntities.ALLIANCE_MERCHANT.get(), type ->
                            new Patched(added.getContext(), type).initLayerLast(added.getContext(), type)));
        }
    }

    /** Epic Fight's humanoid renderer on the merchant's mesh, with his face of the moment. */
    static final class Patched extends PHumanoidRenderer<AllianceMerchantEntity, AllianceMerchantPatch, PlayerModel<AllianceMerchantEntity>, AllianceMerchantRenderer, HumanoidMesh> {
        Patched(EntityRendererProvider.Context context, EntityType<?> type) {
            super(MESH, context, type);
        }

        @Override
        public void render(AllianceMerchantEntity merchant, AllianceMerchantPatch patch, AllianceMerchantRenderer renderer, MultiBufferSource buffers,
                           PoseStack poseStack, int light, float partialTicks) {
            face = MobFaces.of(merchant, patch);
            try {
                super.render(merchant, patch, renderer, buffers, poseStack, light, partialTicks);
            } finally {
                face = MobFaces.Face.CALM;
            }
        }
    }
}
