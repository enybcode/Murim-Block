package com.murimblock.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * Cloth that swings, on a mob Epic Fight draws: a cape, a sash's tail, a tassel. Each strip is left out of the mob's
 * rigid mesh and described in assets/murimblock/cloth/<name>.json (written with the model, e.g. peak-captain-3d.cjs):
 * its joint, its top edge, its length and rest slant, its two faces in the mob's atlas, the plane of the body it hangs
 * against. Each frame the top edge follows its joint's pose, a {@link ClothStrand} per strip and per mob swings in
 * world space (so walking, turning, a lunge or a fall move it), and the strip is drawn as a band of quads along it,
 * both faces, as thick as it was modelled.
 */
public final class ClothLayer<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends EntityModel<E>>
        extends PatchedLayer<E, T, M, RenderLayer<E, M>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** One strip of cloth (positions in Blockbench pixels, uv in atlas texels). */
    record Strip(String joint, Vector3f left, Vector3f right, float length, float thickness, Vector3f drop, int links,
                 float[] outer, float[] inner, Vector3f planeNormal, float planeAt) { }

    private final ResourceLocation description;
    private final Function<E, ResourceLocation> texture;
    private List<Strip> strips;
    private int textureSize = 64;
    private final Map<E, ClothStrand[]> strands = new WeakHashMap<>();

    /** Cloth from assets/murimblock/cloth/{name}.json, drawn with the mob's atlas. */
    public ClothLayer(String name, Function<E, ResourceLocation> texture) {
        this.description = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "cloth/" + name + ".json");
        this.texture = texture;
    }

    private List<Strip> strips() {
        if (strips != null) return strips;
        List<Strip> read = new ArrayList<>();
        try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(description)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            textureSize = root.get("texture_size").getAsInt();
            for (var e : root.getAsJsonArray("strips")) {
                JsonObject s = e.getAsJsonObject();
                JsonArray top = s.getAsJsonArray("top");
                JsonObject plane = s.getAsJsonObject("plane");
                read.add(new Strip(s.get("joint").getAsString(), vector(top.get(0).getAsJsonArray()), vector(top.get(1).getAsJsonArray()),
                        s.get("length").getAsFloat(), s.get("thickness").getAsFloat(), vector(s.getAsJsonArray("drop")).normalize(),
                        s.get("links").getAsInt(), floats(s.getAsJsonArray("outer")), floats(s.getAsJsonArray("inner")),
                        vector(plane.getAsJsonArray("normal")).normalize(), plane.get("at").getAsFloat()));
            }
        } catch (Exception e) {
            LOGGER.error("Murimblock: cannot read the cloth {}", description, e);
        }
        return strips = read;
    }

    private static Vector3f vector(JsonArray a) {
        return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
    }

    private static float[] floats(JsonArray a) {
        float[] f = new float[a.size()];
        for (int i = 0; i < f.length; i++) f[i] = a.get(i).getAsFloat();
        return f;
    }

    @Override
    protected void renderLayer(T patch, E entity, RenderLayer<E, M> original, PoseStack stack, MultiBufferSource buffers, int light,
                               OpenMatrix4f[] poses, float age, float headYaw, float headPitch, float partial) {
        if (entity.isInvisible()) return;
        List<Strip> list = strips();
        if (list.isEmpty()) return;
        Armature armature = patch.getArmature();
        ClothStrand[] mine = strands.computeIfAbsent(entity, e -> new ClothStrand[list.size()]);
        // The stack holds the model's space (Epic Fight's, in blocks) relative to the camera; the strands live in the world.
        Matrix4f model = new Matrix4f(stack.last().pose()), back = new Matrix4f(model).invert();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        long now = System.nanoTime();
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(texture.apply(entity)));
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0);
        for (int n = 0; n < list.size(); n++) {
            Strip s = list.get(n);
            var joint = armature.searchJointByName(s.joint());
            if (joint == null) continue;
            if (mine[n] == null || mine[n].links() != s.links()) mine[n] = new ClothStrand(s.links());
            OpenMatrix4f bone = OpenMatrix4f.mul(poses[joint.getId()], joint.getToOrigin(), null);
            // The top edge, the slant, the body's plane, posed with the joint (rest space in blocks: pixels / 16).
            Vector3f left = posed(bone, new Vector3f(s.left()).div(16)), right = posed(bone, new Vector3f(s.right()).div(16));
            Vector3f top = new Vector3f(left).add(right).mul(0.5F);
            Vector3f restTop = new Vector3f(s.left()).add(s.right()).mul(0.5F / 16);
            Vector3f bottom = posed(bone, new Vector3f(s.drop()).mul(s.length() / 16).add(restTop));
            Vector3f planePoint = posed(bone, new Vector3f(s.planeNormal()).mul(s.planeAt() / 16));
            Vector3f planeAlong = posed(bone, new Vector3f(s.planeNormal()).mul(s.planeAt() / 16 + 1));
            // Into the world.
            Vector3f wTop = world(model, top), wBottom = world(model, bottom);
            Vector3f wPlane = world(model, planePoint), wNormal = world(model, planeAlong).sub(wPlane).normalize();
            Vector3f slant = new Vector3f(wBottom).sub(wTop);
            float worldLength = slant.length();
            if (worldLength < 1.0e-5F) continue;
            slant.div(worldLength);
            ClothStrand strand = mine[n];
            strand.follow(camera.x + wTop.x, camera.y + wTop.y, camera.z + wTop.z, slant.x, slant.y, slant.z, worldLength / s.links(),
                    camera.x + wPlane.x, camera.y + wPlane.y, camera.z + wPlane.z, wNormal.x, wNormal.y, wNormal.z, now);
            // Back into the model's space, to draw with the stack.
            Vector3f[] middle = new Vector3f[s.links() + 1];
            for (int i = 0; i <= s.links(); i++) {
                double[] j = strand.joint(i);
                middle[i] = back.transformPosition(new Vector3f((float) (j[0] - camera.x), (float) (j[1] - camera.y), (float) (j[2] - camera.z)));
            }
            middle[0] = top;
            Vector3f across = new Vector3f(right).sub(left);
            Vector3f outward = new Vector3f(planeAlong).sub(planePoint).normalize();
            draw(out, stack.last(), s, middle, across, outward, light, overlay);
        }
    }

    private static Vector3f posed(OpenMatrix4f bone, Vector3f rest) {
        Vec3 v = OpenMatrix4f.transform(bone, new Vec3(rest.x, rest.y, rest.z));
        return new Vector3f((float) v.x, (float) v.y, (float) v.z);
    }

    /** A point of the model's space, relative to the camera in the world. */
    private static Vector3f world(Matrix4f model, Vector3f v) {
        return model.transformPosition(new Vector3f(v));
    }

    /** The band along the strand's joints: its outer face, then its inner face, half its thickness either side. */
    private void draw(VertexConsumer out, PoseStack.Pose pose, Strip s, Vector3f[] middle, Vector3f across, Vector3f outward, int light, int overlay) {
        int links = s.links();
        Vector3f[] lefts = new Vector3f[links + 1], rights = new Vector3f[links + 1], normals = new Vector3f[links + 1];
        float half = across.length() / 2;
        for (int i = 0; i <= links; i++) {
            Vector3f along = new Vector3f(middle[Math.min(i + 1, links)]).sub(middle[Math.max(i - 1, 0)]);
            if (along.lengthSquared() < 1.0e-10F) along.set(0, -1, 0);
            along.normalize();
            // Across the strip, square to its length; facing out of the body.
            Vector3f side = new Vector3f(across).sub(new Vector3f(along).mul(across.dot(along)));
            if (side.lengthSquared() < 1.0e-10F) side.set(across);
            side.normalize().mul(half);
            Vector3f normal = new Vector3f(side).cross(along).normalize();
            if (normal.dot(outward) < 0) normal.negate();
            normals[i] = normal;
            lefts[i] = new Vector3f(middle[i]).sub(side);
            rights[i] = new Vector3f(middle[i]).add(side);
        }
        float offset = s.thickness() / 32;
        face(out, pose, lefts, rights, normals, offset, s.outer(), light, overlay);
        face(out, pose, lefts, rights, normals, -offset, s.inner(), light, overlay);
    }

    private void face(VertexConsumer out, PoseStack.Pose pose, Vector3f[] lefts, Vector3f[] rights, Vector3f[] normals, float offset,
                      float[] uv, int light, int overlay) {
        int links = lefts.length - 1;
        float size = textureSize, sign = Math.signum(offset);
        for (int i = 0; i < links; i++) {
            float v0 = (uv[1] + (uv[3] - uv[1]) * i / links) / size, v1 = (uv[1] + (uv[3] - uv[1]) * (i + 1) / links) / size;
            float uL = uv[0] / size, uR = uv[2] / size;
            vertex(out, pose, lefts[i], normals[i], offset, sign, uL, v0, light, overlay);
            vertex(out, pose, lefts[i + 1], normals[i + 1], offset, sign, uL, v1, light, overlay);
            vertex(out, pose, rights[i + 1], normals[i + 1], offset, sign, uR, v1, light, overlay);
            vertex(out, pose, rights[i], normals[i], offset, sign, uR, v0, light, overlay);
        }
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, Vector3f at, Vector3f normal, float offset, float sign,
                               float u, float v, int light, int overlay) {
        out.addVertex(pose, at.x + normal.x * offset, at.y + normal.y * offset, at.z + normal.z * offset)
                .setColor(-1).setUv(u, v).setOverlay(overlay).setLight(light)
                .setNormal(pose, normal.x * sign, normal.y * sign, normal.z * sign);
    }
}
