package com.murimblock.client.innerworld;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.innerworld.TrialZombie;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Trial zombies look like vanilla zombies; the Grave Warden is a mutant one ({@code scripts/grave-warden.cjs}):
 * rotting skin, bare ribs, red eyes glowing in the dark, and growths ({@link GraveWardenMutationLayer}).
 */
public final class TrialZombieRenderer extends AbstractZombieRenderer<TrialZombie, ZombieModel<TrialZombie>> {
    private static final ResourceLocation WARDEN =
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/grave_warden.png");
    private static final ResourceLocation WARDEN_EYES =
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/grave_warden_eyes.png");

    public TrialZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
                new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)));
        addLayer(new GraveWardenMutationLayer(this, WARDEN));
        addLayer(new RenderLayer<>(this) {
            /** The eyes, full bright: they glow in the graveyard's night. */
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, TrialZombie zombie, float limbSwing,
                               float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                if (zombie.isWarden() && !zombie.isInvisible()) {
                    getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(WARDEN_EYES)), 0xF00000,
                            OverlayTexture.NO_OVERLAY);
                }
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie zombie) {
        return zombie instanceof TrialZombie trial && trial.isWarden() ? WARDEN : super.getTextureLocation(zombie);
    }
}
