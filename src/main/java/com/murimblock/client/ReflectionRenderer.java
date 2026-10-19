package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.murimblock.Murimblock;
import com.murimblock.martial.ReflectionEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

/**
 * A reflection of water (martial/ReflectionEntity): its owner's own body, in the very pose they hold now (Epic Fight's
 * animation of the owner, as the Azure Cloud Sword's afterimages), turned to face where the reflection faces, drawn in
 * translucent water-blue light that ripples slowly; a faint brighter rim when it has just appeared.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class ReflectionRenderer extends EntityRenderer<ReflectionEntity> {
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/afterimage.png");
    private static boolean drawing;

    public ReflectionRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.REFLECTION.get(), ReflectionRenderer::new);
    }

    @Override
    public void render(ReflectionEntity reflection, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        Player owner = reflection.ownerPlayer();
        if (owner == null || drawing) return;
        @SuppressWarnings("unchecked")
        EntityRenderer<Player> renderer = (EntityRenderer<Player>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(owner);
        // Water light: a slow ripple of brightness, brighter as it appears.
        float age = reflection.tickCount + partial;
        float b = 0.62F + 0.12F * Mth.sin(age * 0.25F) + Math.max(0, 0.4F * (1 - age / 8F));
        int r = (int) (70 * b), g = (int) (165 * b), bl = (int) (255 * b);
        RenderType glow = RenderType.eyes(WHITE);
        MultiBufferSource tinted = type -> new CloudStepEffects.Tinted(buffers.getBuffer(glow), r, g, bl);
        // The owner's body turns by their own body's yaw: undo it, then turn to the reflection's.
        float ownerBody = Mth.rotLerp(partial, owner.yBodyRotO, owner.yBodyRot), mine = Mth.rotLerp(partial, reflection.yRotO, reflection.getYRot());
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(ownerBody - mine));
        drawing = true;
        try {
            renderer.render(owner, Mth.lerp(partial, owner.yRotO, owner.getYRot()), partial, pose, tinted, LightTexture.FULL_BRIGHT);
        } finally {
            drawing = false;
        }
        pose.popPose();
    }

    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        if (drawing) event.setCanRender(TriState.FALSE);
    }

    @Override
    public ResourceLocation getTextureLocation(ReflectionEntity reflection) {
        return WHITE;
    }
}
