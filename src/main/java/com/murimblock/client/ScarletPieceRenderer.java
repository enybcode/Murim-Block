package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.murimblock.Murimblock;
import com.murimblock.martial.ScarletPieceEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.UUID;

/**
 * Scarlet Bloom's pieces (martial/ScarletPieceEntity): the crystals with their own models (geo/scarlet_spike,
 * scarlet_rod, scarlet_shard, scarlet_bloom), on the thrown crystal's texture and glowmask, lit as if they shone (full
 * light); the blood (geo/scarlet_blood, scarlet_puddle) on its own glossy texture; a stone is the ground's own block,
 * spinning as it flies; a limb is that part of the burst player's own body, in their skin and its outer layer. Each is
 * turned by its synced turn and stretched to its width and length.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class ScarletPieceRenderer extends GeoEntityRenderer<ScarletPieceEntity> {
    /** The models' own sizes at scale 1 (blocks): each piece is stretched from these to its width and length. */
    private static final float SPIKE_WIDTH = 0.25F, SPIKE_LENGTH = 1.1F, ROD_WIDTH = 0.075F, ROD_LENGTH = 1.0F, SHARD_LENGTH = 0.75F, BLOOM_SIZE = 1.2F,
            BLOOD_SIZE = 0.4F, PUDDLE_SIZE = 1.0F;
    /** The middle of each limb's box in the player model (pixels, the model's y down): it is drawn centred on it. */
    private static final float[][] LIMB_CENTRE = {{0, -4, 0}, {0, 6, 0}, {-1, 4, 0}, {1, 4, 0}, {0, 6, 0}, {0, 6, 0}};

    private final PlayerModel<Player> wide, slim;

    public ScarletPieceRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        shadowRadius = 0;
        wide = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        slim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        // The crystals' facets shine; the blood does not.
        addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override public void render(PoseStack pose, ScarletPieceEntity piece, BakedGeoModel model, RenderType type, MultiBufferSource buffers,
                                         VertexConsumer buffer, float partial, int light, int overlay) {
                if (crystal(piece)) super.render(pose, piece, model, type, buffers, buffer, partial, light, overlay);
            }
        });
    }

    private static boolean crystal(ScarletPieceEntity piece) {
        byte k = piece.kind();
        return k == ScarletPieceEntity.SPIKE || k == ScarletPieceEntity.ROD || k == ScarletPieceEntity.SHARD || k == ScarletPieceEntity.BLOOM;
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.SCARLET_PIECE.get(), ScarletPieceRenderer::new);
    }

    @Override
    public void render(ScarletPieceEntity piece, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (piece.kind() == ScarletPieceEntity.STONE) {
            // A stone: the ground's block, centred, spinning as it flies.
            float size = piece.width();
            pose.pushPose();
            pose.mulPose(piece.turn());
            pose.mulPose(Axis.XP.rotationDegrees((piece.tickCount + partial) * 23));
            pose.scale(size, size, size);
            pose.translate(-0.5, -0.5, -0.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(piece.blockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
            return;
        }
        if (piece.kind() == ScarletPieceEntity.LIMB) {
            renderLimb(piece, partial, pose, buffers, light);
            return;
        }
        // The crystals shine: drawn at full light.
        super.render(piece, yaw, partial, pose, buffers, crystal(piece) ? LightTexture.FULL_BRIGHT : light);
    }

    /** A limb of the burst body: that part of the player model (and its outer layer) in the player's skin, centred, turned. */
    private void renderLimb(ScarletPieceEntity piece, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        PlayerSkin skin = skin(piece.owner().orElse(null));
        PlayerModel<Player> model = skin.model() == PlayerSkin.Model.SLIM ? slim : wide;
        ModelPart[] parts = switch (piece.part()) {
            case ScarletPieceEntity.HEAD -> new ModelPart[]{model.head, model.hat};
            case ScarletPieceEntity.TORSO -> new ModelPart[]{model.body, model.jacket};
            case ScarletPieceEntity.RIGHT_ARM -> new ModelPart[]{model.rightArm, model.rightSleeve};
            case ScarletPieceEntity.LEFT_ARM -> new ModelPart[]{model.leftArm, model.leftSleeve};
            case ScarletPieceEntity.RIGHT_LEG -> new ModelPart[]{model.rightLeg, model.rightPants};
            default -> new ModelPart[]{model.leftLeg, model.leftPants};
        };
        float[] c = LIMB_CENTRE[Math.min(LIMB_CENTRE.length - 1, Math.max(0, piece.part()))];
        pose.pushPose();
        pose.mulPose(piece.turn());
        pose.scale(-1, -1, 1);
        pose.translate(-c[0] / 16, -c[1] / 16, -c[2] / 16);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(skin.texture()));
        for (ModelPart part : parts) {
            part.resetPose();
            part.x = part.y = part.z = 0;
            part.xRot = part.yRot = part.zRot = 0;
            part.visible = true;
            part.render(pose, buffer, light, OverlayTexture.NO_OVERLAY);
            part.resetPose();
        }
        pose.popPose();
    }

    /** The skin of the player whose body this was (still known from the player list after their death). */
    private static PlayerSkin skin(UUID owner) {
        if (owner == null) return DefaultPlayerSkin.get(new UUID(0, 0));
        var connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection == null ? null : connection.getPlayerInfo(owner);
        return info != null ? info.getSkin() : DefaultPlayerSkin.get(owner);
    }

    @Override
    protected void applyRotations(ScarletPieceEntity piece, PoseStack pose, float age, float yaw, float partial, float nativeScale) {
        pose.mulPose(piece.turn());
        float length = piece.length(partial), width = piece.width();
        switch (piece.kind()) {
            case ScarletPieceEntity.SPIKE -> pose.scale(width / SPIKE_WIDTH, length / SPIKE_LENGTH, width / SPIKE_WIDTH);
            case ScarletPieceEntity.ROD -> pose.scale(width / ROD_WIDTH, length / ROD_LENGTH, width / ROD_WIDTH);
            case ScarletPieceEntity.BLOOM -> {
                float s = length / BLOOM_SIZE;
                pose.mulPose(Axis.YP.rotationDegrees(age * 6));
                pose.scale(s, s, s);
            }
            case ScarletPieceEntity.BLOOD -> {
                float s = length / BLOOD_SIZE;
                pose.scale(s, s, s);
            }
            case ScarletPieceEntity.PUDDLE -> {
                // Spread flat to its width (its length grows it).
                float s = length / PUDDLE_SIZE;
                pose.scale(s, 1, s);
            }
            default -> {
                float s = length / SHARD_LENGTH;
                pose.scale(s, s, s);
            }
        }
    }

    @Override
    public RenderType getRenderType(ScarletPieceEntity piece, ResourceLocation texture, MultiBufferSource buffers, float partial) {
        return RenderType.entityCutoutNoCull(texture);
    }

    private static final class Model extends GeoModel<ScarletPieceEntity> {
        private static ResourceLocation asset(String path) {
            return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
        }

        @Override public ResourceLocation getModelResource(ScarletPieceEntity piece) {
            return asset(switch (piece.kind()) {
                case ScarletPieceEntity.SPIKE -> "geo/scarlet_spike.geo.json";
                case ScarletPieceEntity.ROD -> "geo/scarlet_rod.geo.json";
                case ScarletPieceEntity.BLOOM -> "geo/scarlet_bloom.geo.json";
                case ScarletPieceEntity.BLOOD -> "geo/scarlet_blood.geo.json";
                case ScarletPieceEntity.PUDDLE -> "geo/scarlet_puddle.geo.json";
                default -> "geo/scarlet_shard.geo.json";
            });
        }

        @Override public ResourceLocation getTextureResource(ScarletPieceEntity piece) {
            byte k = piece.kind();
            return asset(k == ScarletPieceEntity.BLOOD || k == ScarletPieceEntity.PUDDLE ? "textures/entity/scarlet_blood.png"
                    : "textures/entity/blood_crystal_shot.png");
        }

        /** No animation of their own (the finish moves them); the thrown crystal's file only to satisfy GeckoLib. */
        @Override public ResourceLocation getAnimationResource(ScarletPieceEntity piece) {
            return asset("animations/blood_crystal_shot.animation.json");
        }
    }
}
