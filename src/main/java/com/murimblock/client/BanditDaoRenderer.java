package com.murimblock.client;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.murimblock.Murimblock;
import com.murimblock.item.MurimItems;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * The Bandit Dao in a hand, with its tassel on a swinging cord. The dao's held model (models/item/bandit_dao_held.json)
 * only carries the hand's transforms; this draws the sword (bandit_dao_body) and then its tassel (bandit_dao_tassel)
 * turned about the pommel toward where a {@link TasselPendulum} has swung it. The pendulum is fed the pommel's position
 * each frame, so the tassel lags behind a cut or a turn, swings back and settles hanging straight down. The icon in
 * the inventory, on the ground and in frames stays the flat sprite; those never come here.
 * Models from scripts/animation/bandit-dao-model.cjs, which gives the pivot and the cord below.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class BanditDaoRenderer extends BlockEntityWithoutLevelRenderer {
    static final ModelResourceLocation BODY = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "item/bandit_dao_body"));
    static final ModelResourceLocation TASSEL = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "item/bandit_dao_tassel"));

    /** The pommel's end in the upright sword, in model pixels (the generator's POMMEL_END). */
    private static final float POMMEL_END = -3.5F;
    /** Where the cord hangs: the pommel's end once the sword is turned 45 degrees about the model's centre. */
    private static final Vector3f PIVOT = new Vector3f(8 - (8 - POMMEL_END) / Mth.SQRT_OF_TWO, 8 - (8 - POMMEL_END) / Mth.SQRT_OF_TWO, 8).div(16);
    /** Along the grip, away from the blade: the cord's direction at rest in the model. */
    private static final Vector3f HILT_DOWN = new Vector3f(-1, -1, 0).normalize();
    /** The cord, from the pommel to the middle of the tuft (in model units). */
    private static final float CORD = 3.2F / 16;
    /** The farthest the tassel swings from the grip's line, so it never goes through the grip. */
    private static final float MAX_SWING = (float) Math.toRadians(130);

    private static final Supplier<BanditDaoRenderer> INSTANCE = Suppliers.memoize(() ->
            new BanditDaoRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()));

    /** One pendulum per held dao and per space it is drawn in: the world, the first-person hand, a menu. */
    private final Map<ItemStack, TasselPendulum[]> pendulums = new WeakHashMap<>();

    private BanditDaoRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(TASSEL);
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return INSTANCE.get();
            }
        }, MurimItems.BANDIT_DAO.get());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        var models = Minecraft.getInstance().getModelManager();
        draw(models.getModel(BODY), stack, poseStack, buffers, light, overlay);

        Matrix4f pose = poseStack.last().pose();
        Vector3f pivot = pose.transformPosition(PIVOT, new Vector3f());
        float length = pose.transformDirection(new Vector3f(0, CORD, 0)).length();
        // Entities and the first-person hand are drawn around the camera on the world's axes; a menu draws on the
        // screen's, y pointing down.
        boolean menu = RenderSystem.getVertexSorting() == VertexSorting.ORTHOGRAPHIC_Z;
        int space = menu ? 2 : context.firstPerson() ? 1 : 0;
        TasselPendulum[] spaces = pendulums.computeIfAbsent(stack, s -> new TasselPendulum[3]);
        if (spaces[space] == null) spaces[space] = new TasselPendulum();
        TasselPendulum pendulum = spaces[space];
        if (menu) {
            pendulum.follow(pivot.x, pivot.y, pivot.z, length, 0, 1, 0, Util.getNanos());
        } else {
            Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            pendulum.follow(camera.x + pivot.x, camera.y + pivot.y, camera.z + pivot.z, length, 0, -1, 0, Util.getNanos());
        }

        // Back into the model: which way the cord hangs there.
        double[] d = pendulum.direction();
        Vector3f hang = new Matrix4f(pose).invert().transformDirection(new Vector3f((float) d[0], (float) d[1], (float) d[2])).normalize();
        if (!hang.isFinite()) hang.set(HILT_DOWN);
        float angle = HILT_DOWN.angle(hang);
        if (angle > MAX_SWING) {
            Vector3f axis = new Vector3f(HILT_DOWN).cross(hang);
            if (axis.lengthSquared() < 1.0e-8F) axis.set(0, 0, 1);
            hang.set(HILT_DOWN).rotateAxis(MAX_SWING, axis.normalize().x, axis.y, axis.z);
        }
        poseStack.pushPose();
        poseStack.translate(PIVOT.x, PIVOT.y, PIVOT.z);
        poseStack.mulPose(new Quaternionf().rotationTo(0, -1, 0, hang.x, hang.y, hang.z));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        draw(models.getModel(TASSEL), stack, poseStack, buffers, light, overlay);
        poseStack.popPose();
    }

    private static void draw(BakedModel model, ItemStack stack, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        ItemRenderer items = Minecraft.getInstance().getItemRenderer();
        for (RenderType type : model.getRenderTypes(stack, true))
            items.renderModelLists(model, stack, light, overlay, poseStack, ItemRenderer.getFoilBufferDirect(buffers, type, true, stack.hasFoil()));
    }
}
