package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.ColossusAnimations;
import com.murimblock.integration.epicfight.SonOfTheDemonPatch;
import com.murimblock.mob.SonOfTheDemonEntity;
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
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;

/**
 * The Son of the Demon: Epic Fight draws him with his own 3D model on his own armature (animmodels/entity/son_of_the_demon,
 * a HumanoidMesh, its armature in the same file; texture textures/entity/son_of_the_demon.png), all from
 * scripts/animation/colossus-epicfight.cjs, twice a man's size: some seven blocks tall. His golden body is light: drawn
 * at full brightness, day or night, in a cave or not. No flat skin: without Epic Fight's renderer the fallback shows his atlas.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SonOfTheDemonRenderer extends HumanoidMobRenderer<SonOfTheDemonEntity, PlayerModel<SonOfTheDemonEntity>> {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/son_of_the_demon.png");
    static final MeshAccessor<HumanoidMesh> MESH =
            MeshAccessor.create(Murimblock.MOD_ID, "entity/son_of_the_demon", loader -> loader.loadSkinnedMesh(HumanoidMesh::new));
    /** How much larger than a man he is drawn (his box is as much larger: MurimEntities.SON_OF_THE_DEMON). */
    public static final float SCALE = 2.0F;
    private static boolean patchedRendererHooked;

    public SonOfTheDemonRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 2.0F);
    }

    @Override public ResourceLocation getTextureLocation(SonOfTheDemonEntity warlord) {
        return TEXTURE;
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.SON_OF_THE_DEMON.get(), SonOfTheDemonRenderer::new);
        // Epic Fight re-posts this hook on every resource reload; the listener itself is added once.
        if (!patchedRendererHooked) {
            patchedRendererHooked = true;
            EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(added ->
                    added.addPatchedEntityRenderer(MurimEntities.SON_OF_THE_DEMON.get(), type ->
                            new Patched(added.getContext(), type).initLayerLast(added.getContext(), type)));
        }
    }

    /** Epic Fight's humanoid renderer on his mesh, scaled up about his feet. */
    static final class Patched extends PHumanoidRenderer<SonOfTheDemonEntity, SonOfTheDemonPatch, PlayerModel<SonOfTheDemonEntity>, SonOfTheDemonRenderer, HumanoidMesh> {
        Patched(EntityRendererProvider.Context context, EntityType<?> type) {
            super(MESH, context, type);
        }

        @Override
        public void render(SonOfTheDemonEntity warlord, SonOfTheDemonPatch patch, SonOfTheDemonRenderer renderer, MultiBufferSource buffers,
                           PoseStack poseStack, int light, float partialTicks) {
            IdlePhase.desync(warlord, patch, ColossusAnimations.IDLE);
            super.render(warlord, patch, renderer, buffers, poseStack, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, partialTicks);
        }

        @Override
        public void mulPoseStack(PoseStack poseStack, Armature armature, SonOfTheDemonEntity warlord, SonOfTheDemonPatch patch, float partialTicks) {
            super.mulPoseStack(poseStack, armature, warlord, patch, partialTicks);
            poseStack.scale(SCALE, SCALE, SCALE);
        }
    }
}
