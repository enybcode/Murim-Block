package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.MountainBanditPatch;
import com.murimblock.mob.MountainBanditEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;

/**
 * The Mountain Bandit: a player-shaped body (the player's 64x64 skin layout, its outer layer worn as hood, turban,
 * pelt and robe skirt), one skin per outfit (textures/entity/mountain_bandit/<outfit>.png, painted by
 * scripts/worldgen/BanditSkins.java). Epic Fight draws it on its biped mesh with the dao in its hand.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MountainBanditRenderer extends HumanoidMobRenderer<MountainBanditEntity, PlayerModel<MountainBanditEntity>> {
    private static final ResourceLocation[] SKINS = new ResourceLocation[MountainBanditEntity.OUTFITS.length];
    private static boolean patchedRendererHooked;

    static {
        for (int i = 0; i < SKINS.length; i++)
            SKINS[i] = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/mountain_bandit/" + MountainBanditEntity.OUTFITS[i] + ".png");
    }

    public MountainBanditRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override public ResourceLocation getTextureLocation(MountainBanditEntity bandit) {
        return SKINS[bandit.getVariant()];
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.MOUNTAIN_BANDIT.get(), MountainBanditRenderer::new);
        // Epic Fight re-posts this hook on every resource reload; the listener itself is added once.
        if (!patchedRendererHooked) {
            patchedRendererHooked = true;
            EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(added ->
                    added.addPatchedEntityRenderer(MurimEntities.MOUNTAIN_BANDIT.get(), type ->
                            new PHumanoidRenderer<MountainBanditEntity, MountainBanditPatch, PlayerModel<MountainBanditEntity>, MountainBanditRenderer, HumanoidMesh>(
                                    Meshes.BIPED, added.getContext(), type).initLayerLast(added.getContext(), type)));
        }
    }
}
