package com.murimblock.bloodsect.ninja.client;

import com.murimblock.bloodsect.BloodSect;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * The Blood Cultists' look, made in Murimblock (scripts/animation/blood-cultist-game.cjs): its faces as quads, by part of
 * the ninja's model, in each part's own space (vanilla pixels), with a free uv each (a vanilla cube's box uv cannot hold
 * them); and the four light armours' quads. Read once from assets/murimblock/models/entity/blood_cultist.json. The Blood
 * Soldiers' (soldier(), scripts/animation/blood-soldier-game.cjs) are read the same way from blood_soldier.json, their
 * armour by the slot each piece of the Blood Lacquer is worn in (head, chest, legs, feet).
 */
public final class CultistMesh {
    private static final Source CULTIST = new Source("blood_cultist"), SOLDIER = new Source("blood_soldier");
    /** The armours, in the order of BloodNinja's armour index (1 to 4). */
    public static final List<String> ARMOURS = List.of("a_single_pad", "b_brigandine", "c_iron_caps", "d_lamellae");

    /** Each part's quads: 4 corners of (x, y, z, u, v), then the normal: 23 floats a quad. */
    private final Map<String, float[][]> parts;
    private final Map<String, Map<String, float[][]>> armours = new HashMap<>();
    public final List<String> outfits;
    /** Its outfits' textures' folder, textures/entity/<name>/. */
    private final String name;

    private CultistMesh(String name, JsonObject json) {
        this.name = name;
        this.parts = parts(json.getAsJsonObject("parts"));
        JsonObject a = json.getAsJsonObject("armour");
        for (String design : a.keySet()) armours.put(design, parts(a.getAsJsonObject(design)));
        this.outfits = json.getAsJsonArray("outfits").asList().stream().map(JsonElement::getAsString).toList();
    }

    private static Map<String, float[][]> parts(JsonObject o) {
        Map<String, float[][]> out = new HashMap<>();
        for (String part : o.keySet()) {
            JsonArray quads = o.getAsJsonArray(part);
            float[][] q = new float[quads.size()][];
            for (int i = 0; i < q.length; i++) {
                JsonArray a = quads.get(i).getAsJsonArray();
                q[i] = new float[a.size()];
                for (int k = 0; k < q[i].length; k++) q[i][k] = a.get(k).getAsFloat();
            }
            out.put(part, q);
        }
        return out;
    }

    /** The cultists' mesh, read the first time it is asked for (null if it cannot be read: the old look is drawn instead). */
    @Nullable public static CultistMesh get() {
        return CULTIST.get();
    }

    /** The soldiers' mesh, read the first time it is asked for (null if it cannot be read). */
    @Nullable public static CultistMesh soldier() {
        return SOLDIER.get();
    }

    /** One mesh file, read once. */
    private static final class Source {
        private final String name;
        private final ResourceLocation file;
        private CultistMesh loaded;
        private boolean failed;

        Source(String name) {
            this.name = name;
            this.file = ResourceLocation.fromNamespaceAndPath(BloodSect.ID, "models/entity/" + name + ".json");
        }

        @Nullable CultistMesh get() {
            if (loaded != null || failed) return loaded;
            try (Reader r = Minecraft.getInstance().getResourceManager().openAsReader(file)) {
                loaded = new CultistMesh(name, JsonParser.parseReader(r).getAsJsonObject());
            } catch (Exception e) {
                failed = true;
                com.mojang.logging.LogUtils.getLogger().error("Could not read the Blood Sect's mesh {}", file, e);
            }
            return loaded;
        }
    }

    /** The texture of an outfit (its blink a moment every few seconds). */
    public ResourceLocation texture(int outfit, boolean blink) {
        String key = outfits.get(Math.floorMod(outfit, outfits.size()));
        return ResourceLocation.fromNamespaceAndPath(BloodSect.ID, "textures/entity/" + name + "/" + key + (blink ? "_blink" : "") + ".png");
    }

    public static ResourceLocation armourTexture(int armour) {
        return ResourceLocation.fromNamespaceAndPath(BloodSect.ID, "textures/entity/blood_cultist/armour_" + ARMOURS.get(armour - 1) + ".png");
    }

    /** The cloth's quads of one part (none: an empty array). */
    public float[][] part(String part) {
        return parts.getOrDefault(part, new float[0][]);
    }

    /** One armour's quads of one part (armour 1 to 4). */
    public float[][] armour(int armour, String part) {
        return armour(ARMOURS.get(armour - 1), part);
    }

    /** The quads of one part of the armour (or piece of armour) of that name. */
    public float[][] armour(String key, String part) {
        Map<String, float[][]> a = armours.get(key);
        return a == null ? new float[0][] : a.getOrDefault(part, new float[0][]);
    }

    /** Draws quads in the pose stack's current space (a part's, after its translateAndRotate). */
    public static void draw(float[][] quads, PoseStack stack, VertexConsumer buffer, int light, int overlay, int color) {
        PoseStack.Pose pose = stack.last();
        for (float[] q : quads) {
            float nx = q[20], ny = q[21], nz = q[22];
            for (int c = 0; c < 4; c++) {
                int o = c * 5;
                buffer.addVertex(pose, q[o] / 16F, q[o + 1] / 16F, q[o + 2] / 16F).setColor(color).setUv(q[o + 3], q[o + 4])
                        .setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
            }
        }
    }
}
