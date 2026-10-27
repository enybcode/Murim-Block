package com.murimblock.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Reads a clip set: the animations of a glTF file (as Blender exports an armature's actions) and the sidecar JSON
 * that gives each clip its settings:
 *
 * <pre>
 * {"clips": {"wave": {"loop": true, "transition": 0.2, "mask": ["Shoulder_R"]}}}
 * </pre>
 *
 * {@code mask} lists joints taken with their children (all joints when it is left out). A clip missing from the
 * sidecar gets the defaults (no loop, {@link Animation#DEFAULT_TRANSITION}, all joints); a name or key the sidecar
 * does not know is refused, to catch typos.
 */
public final class ClipLoader {
    private static final Set<String> KEYS = Set.of("loop", "transition", "mask");

    private ClipLoader() {
    }

    /**
     * @param set     the clip set's name, the start of each animation's id
     * @param json    the .gltf file's text
     * @param buffer  reads a buffer file named in the .gltf
     * @param sidecar the sidecar JSON's text, or null when there is none
     * @return the animations by clip name, in the file's order
     */
    public static Map<String, Animation> load(String set, String json, Function<String, byte[]> buffer,
            Armature armature, String sidecar) {
        Map<String, Clip> clips = clips(new GltfLoader(json, buffer), armature);
        JsonObject settings = sidecar == null ? new JsonObject() : JsonParser.parseString(sidecar).getAsJsonObject();
        JsonObject entries = settings.has("clips") ? settings.getAsJsonObject("clips") : new JsonObject();
        for (String name : entries.keySet()) {
            if (!clips.containsKey(name)) {
                throw new IllegalArgumentException("sidecar names clip " + name + ", which is not in the file");
            }
        }
        Map<String, Animation> animations = new LinkedHashMap<>();
        clips.forEach((name, clip) -> {
            JsonObject entry = entries.has(name) ? entries.getAsJsonObject(name) : new JsonObject();
            for (String key : entry.keySet()) {
                if (!KEYS.contains(key)) {
                    throw new IllegalArgumentException("clip " + name + ": unknown setting " + key);
                }
            }
            boolean loop = entry.has("loop") && entry.get("loop").getAsBoolean();
            float transition = entry.has("transition") ? entry.get("transition").getAsFloat()
                    : Animation.DEFAULT_TRANSITION;
            JointMask mask = JointMask.all(armature);
            if (entry.has("mask")) {
                JsonArray joints = entry.getAsJsonArray("mask");
                String[] names = new String[joints.size()];
                for (int i = 0; i < names.length; i++) {
                    names[i] = joints.get(i).getAsString();
                    if (armature.find(names[i]) == null) {
                        throw new IllegalArgumentException("clip " + name + ": mask names unknown joint " + names[i]);
                    }
                }
                mask = JointMask.withChildren(armature, names);
            }
            animations.put(name, new Animation(set + "/" + name, clip, loop, transition, mask));
        });
        return animations;
    }

    /** The file's animations as clips on {@code armature}: channels are matched to joints by node name. */
    static Map<String, Clip> clips(GltfLoader file, Armature armature) {
        JsonObject root = file.json();
        JsonArray nodes = GltfLoader.array(root, "nodes");
        int[] parents = GltfLoader.parentsOf(nodes);
        Map<Integer, Integer> jointOfNode = new HashMap<>();
        int rootNode = -1;
        for (int node = 0; node < nodes.size(); node++) {
            JsonObject entry = nodes.get(node).getAsJsonObject();
            Joint joint = entry.has("name") ? armature.find(entry.get("name").getAsString()) : null;
            if (joint != null) {
                jointOfNode.put(node, joint.index());
                if (joint.parent() < 0) {
                    rootNode = node;
                }
            }
        }
        // As for the model, whatever sits above the root (the armature object) is folded into the root's values.
        Matrix4f above = new Matrix4f();
        if (rootNode >= 0) {
            for (int up = parents[rootNode]; up >= 0; up = parents[up]) {
                above.mulLocal(GltfLoader.matrix(nodes, up));
            }
        }

        Map<String, Clip> clips = new LinkedHashMap<>();
        for (JsonElement element : GltfLoader.array(root, "animations")) {
            JsonObject animation = element.getAsJsonObject();
            String name = animation.has("name") ? animation.get("name").getAsString() : "clip" + clips.size();
            JsonArray samplers = animation.getAsJsonArray("samplers");
            List<Clip.Channel> channels = new ArrayList<>();
            for (JsonElement channelElement : animation.getAsJsonArray("channels")) {
                JsonObject channel = channelElement.getAsJsonObject();
                JsonObject target = channel.getAsJsonObject("target");
                if (!target.has("node")) {
                    continue;
                }
                Integer joint = jointOfNode.get(target.get("node").getAsInt());
                String pathName = target.get("path").getAsString();
                if (joint == null || pathName.equals("weights")) {
                    // Nodes outside the skeleton (meshes, the armature object) and morph weights are not ours.
                    continue;
                }
                Clip.Path path = Clip.Path.valueOf(pathName.toUpperCase());
                JsonObject sampler = samplers.get(channel.get("sampler").getAsInt()).getAsJsonObject();
                Clip.Interpolation interpolation = sampler.has("interpolation")
                        ? Clip.Interpolation.valueOf(sampler.get("interpolation").getAsString()) : Clip.Interpolation.LINEAR;
                float[] values = file.floats(sampler.get("output").getAsInt());
                if (joint == armature.joint(0).index() && !above.equals(new Matrix4f(), 1e-6f)) {
                    fold(above, path, interpolation, values);
                }
                channels.add(new Clip.Channel(joint, path, interpolation, file.floats(sampler.get("input").getAsInt()),
                        values));
            }
            if (clips.put(name, new Clip(name, channels)) != null) {
                throw new IllegalArgumentException("two clips named " + name);
            }
        }
        return clips;
    }

    /** Puts the root's keyframes in the space above the armature object (values only; tangents are left alone). */
    private static void fold(Matrix4f above, Clip.Path path, Clip.Interpolation interpolation, float[] values) {
        int n = path.size;
        int stride = interpolation == Clip.Interpolation.CUBICSPLINE ? 3 * n : n;
        int first = interpolation == Clip.Interpolation.CUBICSPLINE ? n : 0;
        Quaternionf turn = above.getUnnormalizedRotation(new Quaternionf());
        for (int at = first; at < values.length; at += stride) {
            switch (path) {
                case TRANSLATION -> {
                    Vector3f point = above.transformPosition(new Vector3f(values[at], values[at + 1], values[at + 2]));
                    values[at] = point.x;
                    values[at + 1] = point.y;
                    values[at + 2] = point.z;
                }
                case ROTATION -> {
                    Quaternionf q = new Quaternionf(turn).mul(new Quaternionf(values[at], values[at + 1],
                            values[at + 2], values[at + 3]));
                    values[at] = q.x;
                    values[at + 1] = q.y;
                    values[at + 2] = q.z;
                    values[at + 3] = q.w;
                }
                case SCALE -> {
                    Vector3f scale = above.getScale(new Vector3f());
                    values[at] *= scale.x;
                    values[at + 1] *= scale.y;
                    values[at + 2] *= scale.z;
                }
            }
        }
    }
}
