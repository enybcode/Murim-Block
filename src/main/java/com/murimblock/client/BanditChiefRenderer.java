package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.BanditChiefPatch;
import com.murimblock.mob.BanditChiefEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
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
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;

/**
 * The Bandit Chief: Epic Fight draws him with his own 3D model on the biped skeleton (animmodels/entity/bandit_chief, a
 * HumanoidMesh; texture textures/entity/bandit_chief.png), both from scripts/animation/bandit-chief-3d.cjs, 1.3 times a
 * man's size, his axe in his hands. He has no flat skin: without Epic Fight's renderer the player-shaped fallback shows
 * his atlas, not meant for it.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class BanditChiefRenderer extends HumanoidMobRenderer<BanditChiefEntity, PlayerModel<BanditChiefEntity>> {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/bandit_chief.png");
    /** One atlas per face (MobFaces.Face order): bandit_chief.png, then bandit_chief_<face>.png. */
    private static final ResourceLocation[] FACES = new ResourceLocation[MobFaces.Face.values().length];
    /** His face now, picked while Epic Fight draws him. Render thread only. */
    private static MobFaces.Face face = MobFaces.Face.CALM;

    static {
        for (MobFaces.Face f : MobFaces.Face.values())
            FACES[f.ordinal()] = f.suffix.isEmpty() ? TEXTURE : ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/bandit_chief_" + f.suffix + ".png");
    }
    static final MeshAccessor<HumanoidMesh> MESH =
            MeshAccessor.create(Murimblock.MOD_ID, "entity/bandit_chief", loader -> loader.loadSkinnedMesh(HumanoidMesh::new));
    /** How much larger than a man he is drawn (his box is as much larger: MurimEntities.BANDIT_CHIEF). */
    public static final float SCALE = 1.3F;
    private static boolean patchedRendererHooked;

    public BanditChiefRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.8F);
    }

    @Override public ResourceLocation getTextureLocation(BanditChiefEntity chief) {
        return FACES[face.ordinal()];
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.BANDIT_CHIEF.get(), BanditChiefRenderer::new);
        // Epic Fight re-posts this hook on every resource reload; the listener itself is added once.
        if (!patchedRendererHooked) {
            patchedRendererHooked = true;
            EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(added ->
                    added.addPatchedEntityRenderer(MurimEntities.BANDIT_CHIEF.get(), type ->
                            new Patched(added.getContext(), type).initLayerLast(added.getContext(), type)));
        }
    }

    /** Epic Fight's humanoid renderer on the chief's mesh, scaled up about his feet. */
    static final class Patched extends PHumanoidRenderer<BanditChiefEntity, BanditChiefPatch, PlayerModel<BanditChiefEntity>, BanditChiefRenderer, HumanoidMesh> {
        Patched(EntityRendererProvider.Context context, EntityType<?> type) {
            super(MESH, context, type);
        }

        @Override
        public void render(BanditChiefEntity chief, BanditChiefPatch patch, BanditChiefRenderer renderer, MultiBufferSource buffers,
                           PoseStack poseStack, int light, float partialTicks) {
            face = MobFaces.of(chief, patch);
            try {
                super.render(chief, patch, renderer, buffers, poseStack, light, partialTicks);
            } finally {
                face = MobFaces.Face.CALM;
            }
        }

        @Override
        public void mulPoseStack(PoseStack poseStack, Armature armature, BanditChiefEntity chief, BanditChiefPatch patch, float partialTicks) {
            super.mulPoseStack(poseStack, armature, chief, patch, partialTicks);
            poseStack.scale(SCALE, SCALE, SCALE);
        }
    }
}
