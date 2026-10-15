package com.murimblock.client;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.murimblock.Murimblock;
import com.murimblock.item.MurimItems;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
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
 * The chain whip in a hand: its handle (scripts/animation/sect-weapon-models.cjs, WHIP) on the 45-degree diagonal of
 * the vanilla swords, and its links hung one after another from the handle's ring on a {@link RopeChain} simulated in
 * the world, the dart on the last. Swung, the chain flies out straight; still, it droops. The icon in the inventory, on
 * the ground and in frames stays the flat sprite.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class ChainWhipRenderer extends BlockEntityWithoutLevelRenderer {
    /** As WHIP in sect-weapon-models.cjs: the ring at the handle's top, one link's length (pixels), how many. */
    private static final float TOP = 4.6F, LINK = 2.6F;
    private static final int LINKS = 10;

    private static final ModelResourceLocation HANDLE = model("chain_whip_handle"), CHAIN_LINK = model("chain_whip_link"),
            DART = model("chain_whip_dart");
    private static final Supplier<ChainWhipRenderer> INSTANCE = Suppliers.memoize(ChainWhipRenderer::new);

    /** The ring and the handle's direction once the handle is laid on the diagonal (model units). */
    private static final Vector3f ANCHOR, AXIS;
    static {
        Quaternionf diagonal = new Quaternionf().rotationZ((float) Math.toRadians(-45));
        ANCHOR = diagonal.transform(new Vector3f(0, TOP / 16 - 0.5F, 0)).add(0.5F, 0.5F, 0.5F);
        AXIS = diagonal.transform(new Vector3f(0, 1, 0));
    }

    /** One chain per held whip and per space it is drawn in: the world, the first-person hand. */
    private final Map<ItemStack, RopeChain[]> chains = new WeakHashMap<>();

    private ChainWhipRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private static ModelResourceLocation model(String path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "item/" + path));
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation m : new ModelResourceLocation[] {HANDLE, CHAIN_LINK, DART}) event.register(m);
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return INSTANCE.get();
            }
        }, MurimItems.CHAIN_WHIP.get());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var models = Minecraft.getInstance().getModelManager();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(-45));
        pose.translate(-0.5F, -0.5F, -0.5F);
        FanRenderer.draw(models.getModel(HANDLE), stack, pose, buffers, light, overlay);
        pose.popPose();

        // The chain, in the world around the camera (entities and the first-person hand are drawn on the world's axes).
        Matrix4f m = pose.last().pose();
        Vector3f anchor = m.transformPosition(ANCHOR, new Vector3f());
        Vector3f axis = m.transformDirection(AXIS, new Vector3f()).normalize();
        float length = m.transformDirection(new Vector3f(0, LINK / 16, 0)).length();
        RopeChain[] spaces = chains.computeIfAbsent(stack, s -> new RopeChain[2]);
        int space = context.firstPerson() ? 1 : 0;
        if (spaces[space] == null) spaces[space] = new RopeChain(LINKS);
        RopeChain chain = spaces[space];
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        chain.follow(camera.x + anchor.x, camera.y + anchor.y, camera.z + anchor.z, axis.x, axis.y, axis.z, length,
                0, -1, 0, Util.getNanos());

        Matrix4f back = new Matrix4f(m).invert();
        Vector3f from = new Vector3f(ANCHOR), dir = new Vector3f(AXIS);
        for (int i = 1; i <= LINKS + 1; i++) {
            if (i <= LINKS) {
                double[] j = chain.joint(i);
                Vector3f to = back.transformPosition(new Vector3f((float) (j[0] - camera.x), (float) (j[1] - camera.y), (float) (j[2] - camera.z)));
                Vector3f step = new Vector3f(to).sub(from);
                if (step.lengthSquared() > 1.0e-10F) dir.set(step).normalize();
                piece(pose, from, dir, models.getModel(CHAIN_LINK), stack, buffers, light, overlay);
                from = to;
            } else {
                piece(pose, from, dir, models.getModel(DART), stack, buffers, light, overlay);
            }
        }
    }

    /** A piece built from (8, 8, 8) along +y, set at `at` (model units) pointing along `dir`. */
    private static void piece(PoseStack pose, Vector3f at, Vector3f dir, BakedModel model, ItemStack stack,
                              MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(at.x, at.y, at.z);
        pose.mulPose(new Quaternionf().rotationTo(0, 1, 0, dir.x, dir.y, dir.z));
        pose.translate(-0.5F, -0.5F, -0.5F);
        FanRenderer.draw(model, stack, pose, buffers, light, overlay);
        pose.popPose();
    }
}
