package com.murimblock.client;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.murimblock.Murimblock;
import com.murimblock.item.MurimItems;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
 * A sword in a hand with its tassel on a swinging cord, as the Bandit Dao's (BanditDaoRenderer), for any sword built the
 * same way: a held model with only the hand's transforms (models/item/<sword>_held.json), the sword without its tassel
 * (<sword>_body) and the tassel alone hanging from the model's centre (<sword>_tassel), all three on the 45-degree
 * diagonal of the vanilla swords. It draws the body, then the tassel turned about the pommel toward where a
 * {@link TasselPendulum} has swung it. The icon in the inventory, on the ground and in frames stays the flat sprite.
 * The Azure Cloud (scripts/animation/azure-cloud-sword-model.cjs) and the Alliance's Snow Plum Jian
 * (snow-plum-jian-model.cjs); each generator gives its pommel's end and its tassel.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class TasselSwordRenderer extends BlockEntityWithoutLevelRenderer {
    /** Along the grip, away from the blade: the cord's direction at rest in the model. */
    private static final Vector3f HILT_DOWN = new Vector3f(-1, -1, 0).normalize();
    /** The farthest the tassel swings from the grip's line, so it never goes through the grip. */
    private static final float MAX_SWING = (float) Math.toRadians(130);

    /** The Azure Cloud: the pommel's ring ends at -3.6 pixels; a long tassel, its tuft about 4.8 pixels below. */
    private static final Supplier<TasselSwordRenderer> AZURE_CLOUD = Suppliers.memoize(() -> new TasselSwordRenderer("azure_cloud_sword", -3.6F, 4.8F));
    /** The Snow Plum Jian: the pommel ends at -3.8 pixels; a long two-strand tassel, its tuft about 5 pixels below. */
    private static final Supplier<TasselSwordRenderer> SNOW_PLUM = Suppliers.memoize(() -> new TasselSwordRenderer("snow_plum_jian", -3.8F, 5.0F));

    private final ModelResourceLocation body;
    private final ModelResourceLocation tassel;
    /** Where the cord hangs: the pommel's end once the sword is turned 45 degrees about the model's centre. */
    private final Vector3f pivot;
    /** The cord, from the pommel to the middle of the tuft (in model units). */
    private final float cord;
    /** One pendulum per held sword and per space it is drawn in: the world, the first-person hand, a menu. */
    private final Map<ItemStack, TasselPendulum[]> pendulums = new WeakHashMap<>();

    /** @param pommelEnd the pommel's end in the upright sword (model pixels); @param cord pommel to tuft (pixels). */
    private TasselSwordRenderer(String sword, float pommelEnd, float cord) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.body = model(sword + "_body");
        this.tassel = model(sword + "_tassel");
        float diagonal = 8 - (8 - pommelEnd) / Mth.SQRT_OF_TWO;
        this.pivot = new Vector3f(diagonal, diagonal, 8).div(16);
        this.cord = cord / 16;
    }

    private static ModelResourceLocation model(String path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "item/" + path));
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        for (String sword : new String[] {"azure_cloud_sword", "snow_plum_jian"}) {
            event.register(model(sword + "_body"));
            event.register(model(sword + "_tassel"));
        }
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return AZURE_CLOUD.get();
            }
        }, MurimItems.AZURE_CLOUD_SWORD.get());
        event.registerItem(new IClientItemExtensions() {
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return SNOW_PLUM.get();
            }
        }, MurimItems.SNOW_PLUM_JIAN.get());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        var models = Minecraft.getInstance().getModelManager();
        draw(models.getModel(body), stack, poseStack, buffers, light, overlay);

        Matrix4f pose = poseStack.last().pose();
        Vector3f at = pose.transformPosition(pivot, new Vector3f());
        float length = pose.transformDirection(new Vector3f(0, cord, 0)).length();
        // Entities and the first-person hand are drawn around the camera on the world's axes; a menu draws on the
        // screen's, y pointing down.
        boolean menu = RenderSystem.getVertexSorting() == VertexSorting.ORTHOGRAPHIC_Z;
        int space = menu ? 2 : context.firstPerson() ? 1 : 0;
        TasselPendulum[] spaces = pendulums.computeIfAbsent(stack, s -> new TasselPendulum[3]);
        if (spaces[space] == null) spaces[space] = new TasselPendulum();
        TasselPendulum pendulum = spaces[space];
        if (menu) {
            pendulum.follow(at.x, at.y, at.z, length, 0, 1, 0, Util.getNanos());
        } else {
            Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            pendulum.follow(camera.x + at.x, camera.y + at.y, camera.z + at.z, length, 0, -1, 0, Util.getNanos());
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
        poseStack.translate(pivot.x, pivot.y, pivot.z);
        poseStack.mulPose(new Quaternionf().rotationTo(0, -1, 0, hang.x, hang.y, hang.z));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        draw(models.getModel(tassel), stack, poseStack, buffers, light, overlay);
        poseStack.popPose();
    }

    private static void draw(BakedModel model, ItemStack stack, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        ItemRenderer items = Minecraft.getInstance().getItemRenderer();
        for (RenderType type : model.getRenderTypes(stack, true))
            items.renderModelLists(model, stack, light, overlay, poseStack, ItemRenderer.getFoilBufferDirect(buffers, type, true, stack.hasFoil()));
    }
}
