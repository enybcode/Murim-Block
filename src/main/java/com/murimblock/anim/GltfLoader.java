package com.murimblock.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Reads a glTF 2.0 file (JSON with separate buffers, as Blender writes it) into a {@link SkinnedModel}: the first
 * skin becomes the armature, every node with a mesh and that skin becomes a part. Only what Blender exports for our
 * models is supported; anything else is refused with a message saying what.
 */
public final class GltfLoader {
    private static final int TRIANGLES = 4;
    private static final float MATRIX_TOLERANCE = 1e-3f;

    private final JsonObject root;
    private final List<ByteBuffer> buffers = new ArrayList<>();

    private GltfLoader(String json, Function<String, byte[]> buffer) {
        root = JsonParser.parseString(json).getAsJsonObject();
        for (JsonElement element : array(root, "buffers")) {
            JsonObject entry = element.getAsJsonObject();
            if (!entry.has("uri") || entry.get("uri").getAsString().startsWith("data:")) {
                throw new IllegalArgumentException("buffers must be separate .bin files");
            }
            byte[] bytes = buffer.apply(entry.get("uri").getAsString());
            buffers.add(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN));
        }
    }

    /**
     * @param json      the .gltf file's text
     * @param buffer    reads a buffer file named in the .gltf (relative to it)
     * @param reference when not null, the loaded skeleton must have its joints and parents, and keeps its order
     */
    public static SkinnedModel load(String json, Function<String, byte[]> buffer, Armature reference) {
        return new GltfLoader(json, buffer).read(reference);
    }

    private SkinnedModel read(Armature reference) {
        JsonArray skins = array(root, "skins");
        if (skins.isEmpty()) {
            throw new IllegalArgumentException("no skin in the file");
        }
        JsonObject skin = skins.get(0).getAsJsonObject();
        JsonArray nodes = array(root, "nodes");
        int[] skinJoints = ints(skin.getAsJsonArray("joints"));
        int[] parents = parentsOf(nodes);

        Map<String, Integer> nodeByName = new HashMap<>();
        Set<Integer> jointNodes = new HashSet<>();
        for (int node : skinJoints) {
            jointNodes.add(node);
            String name = name(nodes, node);
            if (nodeByName.put(name, node) != null) {
                throw new IllegalArgumentException("two joints named " + name);
            }
        }
        List<Integer> order = new ArrayList<>();
        if (reference != null && reference.size() == skinJoints.length
                && reference.joints().stream().allMatch(joint -> nodeByName.containsKey(joint.name()))) {
            reference.joints().forEach(joint -> order.add(nodeByName.get(joint.name())));
        } else {
            for (int node : skinJoints) {
                if (!jointNodes.contains(parents[node])) {
                    addParentFirst(nodes, node, jointNodes, order);
                }
            }
        }

        Armature.Builder builder = Armature.builder();
        Map<Integer, Integer> armatureIndex = new HashMap<>();
        for (int node : order) {
            int parent = parents[node];
            if (jointNodes.contains(parent)) {
                if (!armatureIndex.containsKey(parent)) {
                    throw new IllegalArgumentException("joint " + name(nodes, node) + " comes before its parent");
                }
                builder.joint(name(nodes, node), name(nodes, parent), transform(nodes, node));
            } else {
                // The root: whatever sits above it (the armature object) is folded into its rest transform.
                Matrix4f above = new Matrix4f();
                for (int up = parent; up >= 0; up = parents[up]) {
                    above.mulLocal(matrix(nodes, up));
                }
                Matrix4f local = above.mul(matrix(nodes, node));
                builder.root(name(nodes, node), JointTransform.of(local.getTranslation(new Vector3f()),
                        local.getUnnormalizedRotation(new Quaternionf()), local.getScale(new Vector3f())));
            }
            armatureIndex.put(node, armatureIndex.size());
        }
        Armature armature = builder.build();
        if (reference != null) {
            List<String> problems = reference.shapeDifferences(armature);
            if (!problems.isEmpty()) {
                throw new IllegalArgumentException("skeleton does not match: " + String.join("; ", problems));
            }
        }

        int[] toArmature = new int[skinJoints.length];
        for (int i = 0; i < skinJoints.length; i++) {
            toArmature[i] = armatureIndex.get(skinJoints[i]);
        }
        if (skin.has("inverseBindMatrices")) {
            float[] matrices = floats(skin.get("inverseBindMatrices").getAsInt());
            for (int i = 0; i < skinJoints.length; i++) {
                Matrix4f fromFile = new Matrix4f().set(matrices, i * 16);
                if (!fromFile.equals(armature.inverseBind(toArmature[i]), MATRIX_TOLERANCE)) {
                    throw new IllegalArgumentException("inverse bind matrix of " + name(nodes, skinJoints[i])
                            + " does not match its rest transform");
                }
            }
        }

        List<SkinnedMesh> parts = new ArrayList<>();
        for (int node = 0; node < nodes.size(); node++) {
            JsonObject entry = nodes.get(node).getAsJsonObject();
            if (entry.has("mesh") && entry.has("skin") && entry.get("skin").getAsInt() == 0) {
                parts.add(mesh(entry, toArmature));
            }
        }
        return new SkinnedModel(armature, List.copyOf(parts));
    }

    private static void addParentFirst(JsonArray nodes, int node, Set<Integer> joints, List<Integer> order) {
        order.add(node);
        JsonObject entry = nodes.get(node).getAsJsonObject();
        if (entry.has("children")) {
            for (int child : ints(entry.getAsJsonArray("children"))) {
                if (joints.contains(child)) {
                    addParentFirst(nodes, child, joints, order);
                }
            }
        }
    }

    private SkinnedMesh mesh(JsonObject node, int[] toArmature) {
        String name = node.get("name").getAsString();
        boolean outer = node.has("extras") && node.getAsJsonObject("extras").has("outer")
                && node.getAsJsonObject("extras").get("outer").getAsBoolean();
        JsonObject mesh = array(root, "meshes").get(node.get("mesh").getAsInt()).getAsJsonObject();
        List<float[]> positions = new ArrayList<>(), normals = new ArrayList<>(), uvs = new ArrayList<>(),
                weights = new ArrayList<>();
        List<int[]> joints = new ArrayList<>(), indices = new ArrayList<>();
        int base = 0;
        for (JsonElement element : mesh.getAsJsonArray("primitives")) {
            JsonObject primitive = element.getAsJsonObject();
            if (primitive.has("mode") && primitive.get("mode").getAsInt() != TRIANGLES) {
                throw new IllegalArgumentException(name + ": only triangles are supported");
            }
            JsonObject attributes = primitive.getAsJsonObject("attributes");
            for (String needed : new String[] {"POSITION", "NORMAL", "TEXCOORD_0", "JOINTS_0", "WEIGHTS_0"}) {
                if (!attributes.has(needed)) {
                    throw new IllegalArgumentException(name + ": missing " + needed);
                }
            }
            float[] position = floats(attributes.get("POSITION").getAsInt());
            int count = position.length / 3;
            positions.add(position);
            normals.add(floats(attributes.get("NORMAL").getAsInt()));
            uvs.add(floats(attributes.get("TEXCOORD_0").getAsInt()));
            int[] joint = ints(attributes.get("JOINTS_0").getAsInt());
            float[] weight = floats(attributes.get("WEIGHTS_0").getAsInt());
            for (int i = 0; i < joint.length; i++) {
                if (joint[i] >= toArmature.length) {
                    throw new IllegalArgumentException(name + ": vertex joint " + joint[i] + " is not in the skin");
                }
                joint[i] = toArmature[joint[i]];
            }
            normalizeWeights(name, weight);
            joints.add(joint);
            weights.add(weight);
            int[] index = primitive.has("indices") ? ints(primitive.get("indices").getAsInt()) : sequence(count);
            for (int i = 0; i < index.length; i++) {
                index[i] += base;
            }
            indices.add(index);
            base += count;
        }
        return new SkinnedMesh(name, outer, join(positions), join(normals), join(uvs), joinInts(joints),
                join(weights), joinInts(indices));
    }

    private static void normalizeWeights(String name, float[] weights) {
        for (int v = 0; v < weights.length; v += SkinnedMesh.INFLUENCES) {
            float sum = 0;
            for (int i = 0; i < SkinnedMesh.INFLUENCES; i++) {
                sum += weights[v + i];
            }
            if (sum < 1e-6f) {
                throw new IllegalArgumentException(name + ": vertex " + v / SkinnedMesh.INFLUENCES + " follows no joint");
            }
            for (int i = 0; i < SkinnedMesh.INFLUENCES; i++) {
                weights[v + i] /= sum;
            }
        }
    }

    // Accessors.

    private ByteBuffer view(JsonObject accessor) {
        JsonObject view = array(root, "bufferViews").get(accessor.get("bufferView").getAsInt()).getAsJsonObject();
        if (view.has("byteStride")) {
            throw new IllegalArgumentException("interleaved buffers are not supported");
        }
        int offset = (view.has("byteOffset") ? view.get("byteOffset").getAsInt() : 0)
                + (accessor.has("byteOffset") ? accessor.get("byteOffset").getAsInt() : 0);
        ByteBuffer buffer = buffers.get(view.get("buffer").getAsInt()).duplicate().order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(offset);
        return buffer.slice().order(ByteOrder.LITTLE_ENDIAN);
    }

    private static int components(String type) {
        return switch (type) {
            case "SCALAR" -> 1;
            case "VEC2" -> 2;
            case "VEC3" -> 3;
            case "VEC4" -> 4;
            case "MAT4" -> 16;
            default -> throw new IllegalArgumentException("accessor type " + type + " is not supported");
        };
    }

    /** An accessor's values as floats, normalized integers scaled to 0..1. */
    float[] floats(int index) {
        JsonObject accessor = array(root, "accessors").get(index).getAsJsonObject();
        int count = accessor.get("count").getAsInt() * components(accessor.get("type").getAsString());
        int type = accessor.get("componentType").getAsInt();
        boolean normalized = accessor.has("normalized") && accessor.get("normalized").getAsBoolean();
        ByteBuffer buffer = view(accessor);
        float[] values = new float[count];
        for (int i = 0; i < count; i++) {
            values[i] = switch (type) {
                case 5126 -> buffer.getFloat();
                case 5121 -> normalized ? (buffer.get() & 0xFF) / 255f : buffer.get() & 0xFF;
                case 5123 -> normalized ? (buffer.getShort() & 0xFFFF) / 65535f : buffer.getShort() & 0xFFFF;
                default -> throw new IllegalArgumentException("component type " + type + " is not supported here");
            };
        }
        return values;
    }

    /** An accessor's values as unsigned integers. */
    int[] ints(int index) {
        JsonObject accessor = array(root, "accessors").get(index).getAsJsonObject();
        int count = accessor.get("count").getAsInt() * components(accessor.get("type").getAsString());
        int type = accessor.get("componentType").getAsInt();
        ByteBuffer buffer = view(accessor);
        int[] values = new int[count];
        for (int i = 0; i < count; i++) {
            values[i] = switch (type) {
                case 5121 -> buffer.get() & 0xFF;
                case 5123 -> buffer.getShort() & 0xFFFF;
                case 5125 -> buffer.getInt();
                default -> throw new IllegalArgumentException("component type " + type + " is not an integer type");
            };
        }
        return values;
    }

    // Nodes.

    private static int[] parentsOf(JsonArray nodes) {
        int[] parents = new int[nodes.size()];
        Arrays.fill(parents, -1);
        for (int node = 0; node < nodes.size(); node++) {
            JsonObject entry = nodes.get(node).getAsJsonObject();
            if (entry.has("children")) {
                for (int child : ints(entry.getAsJsonArray("children"))) {
                    parents[child] = node;
                }
            }
        }
        return parents;
    }

    private static String name(JsonArray nodes, int node) {
        JsonObject entry = nodes.get(node).getAsJsonObject();
        if (!entry.has("name")) {
            throw new IllegalArgumentException("node " + node + " has no name");
        }
        return entry.get("name").getAsString();
    }

    private static Matrix4f matrix(JsonArray nodes, int node) {
        JsonObject entry = nodes.get(node).getAsJsonObject();
        if (entry.has("matrix")) {
            float[] values = new float[16];
            JsonArray matrix = entry.getAsJsonArray("matrix");
            for (int i = 0; i < 16; i++) {
                values[i] = matrix.get(i).getAsFloat();
            }
            return new Matrix4f().set(values);
        }
        return transform(nodes, node).toMatrix(new Matrix4f());
    }

    private static JointTransform transform(JsonArray nodes, int node) {
        JsonObject entry = nodes.get(node).getAsJsonObject();
        if (entry.has("matrix")) {
            Matrix4f matrix = matrix(nodes, node);
            return JointTransform.of(matrix.getTranslation(new Vector3f()),
                    matrix.getUnnormalizedRotation(new Quaternionf()), matrix.getScale(new Vector3f()));
        }
        Vector3f translation = entry.has("translation") ? vector(entry.getAsJsonArray("translation")) : new Vector3f();
        Vector3f scale = entry.has("scale") ? vector(entry.getAsJsonArray("scale")) : new Vector3f(1, 1, 1);
        Quaternionf rotation = new Quaternionf();
        if (entry.has("rotation")) {
            JsonArray r = entry.getAsJsonArray("rotation");
            rotation.set(r.get(0).getAsFloat(), r.get(1).getAsFloat(), r.get(2).getAsFloat(), r.get(3).getAsFloat());
        }
        return JointTransform.of(translation, rotation, scale);
    }

    // Small helpers.

    private static JsonArray array(JsonObject object, String key) {
        return object.has(key) ? object.getAsJsonArray(key) : new JsonArray();
    }

    private static Vector3f vector(JsonArray values) {
        return new Vector3f(values.get(0).getAsFloat(), values.get(1).getAsFloat(), values.get(2).getAsFloat());
    }

    private static int[] ints(JsonArray values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = values.get(i).getAsInt();
        }
        return result;
    }

    private static int[] sequence(int count) {
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            result[i] = i;
        }
        return result;
    }

    private static float[] join(List<float[]> parts) {
        float[] result = new float[parts.stream().mapToInt(part -> part.length).sum()];
        int at = 0;
        for (float[] part : parts) {
            System.arraycopy(part, 0, result, at, part.length);
            at += part.length;
        }
        return result;
    }

    private static int[] joinInts(List<int[]> parts) {
        int[] result = new int[parts.stream().mapToInt(part -> part.length).sum()];
        int at = 0;
        for (int[] part : parts) {
            System.arraycopy(part, 0, result, at, part.length);
            at += part.length;
        }
        return result;
    }
}
