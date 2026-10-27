package com.murimblock.shenfa.client.mob;

import com.murimblock.shenfa.Armature;
import com.murimblock.shenfa.JointTransform;
import com.murimblock.shenfa.Pose;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Any model's own skeleton: one joint per model part, named as the model names it (the child's key in its parent,
 * or the field holding a top part, for models that keep no root), under a root {@link #ROOT} for the whole body.
 * Joints are in vanilla's own space (pixels, Y down, turns applied Z then Y then X): a joint's transform is its
 * part's place and turn, so clips keyed on parts read like vanilla's code. Turning or moving the root moves the
 * whole model about its feet (a flinch, a lunge).
 */
public final class PartRig implements MobRig {
    /** The whole body; it turns about the feet, {@link #FEET} pixels below the model's origin. */
    public static final String ROOT = "@model";
    private static final Vector3fc FEET = new Vector3f(0, 24, 0);
    private static final Field CHILDREN = children();

    private final Armature armature;
    /** The part of each joint (null for the root); and which joints hang straight from the root. */
    private final ModelPart[] parts;
    private final boolean[] top;

    private PartRig(Armature armature, ModelPart[] parts, boolean[] top) {
        this.armature = armature;
        this.parts = parts;
        this.top = top;
    }

    private static Field children() {
        try {
            Field field = ModelPart.class.getDeclaredField("children");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("ModelPart.children not found", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ModelPart> childrenOf(ModelPart part) {
        try {
            return (Map<String, ModelPart>) CHILDREN.get(part);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The rig of {@code model}'s parts: its root's tree, or the parts its fields hold. */
    public static PartRig of(EntityModel<?> model) {
        Map<String, ModelPart> tops = new LinkedHashMap<>();
        if (model instanceof HierarchicalModel<?> hierarchical) {
            tops.putAll(childrenOf(hierarchical.root()));
        } else {
            // Parts held in fields; a part that is another's child is reached through that one.
            Map<ModelPart, String> named = new IdentityHashMap<>();
            for (Class<?> type = model.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (field.getType() == ModelPart.class && !Modifier.isStatic(field.getModifiers())) {
                        try {
                            field.setAccessible(true);
                            ModelPart part = (ModelPart) field.get(model);
                            if (part != null) {
                                named.putIfAbsent(part, field.getName());
                            }
                        } catch (IllegalAccessException | RuntimeException e) {
                            // A field we may not read: its part stays out of the rig.
                        }
                    }
                }
            }
            Map<ModelPart, Boolean> isChild = new IdentityHashMap<>();
            named.keySet().forEach(part -> childrenOf(part).values().forEach(child -> isChild.put(child, true)));
            named.forEach((part, name) -> {
                if (!isChild.containsKey(part)) {
                    tops.put(name, part);
                }
            });
        }
        Armature.Builder builder = Armature.builder().root(ROOT, JointTransform.IDENTITY);
        List<ModelPart> parts = new ArrayList<>();
        List<Boolean> top = new ArrayList<>();
        parts.add(null);
        top.add(false);
        Map<String, Integer> used = new LinkedHashMap<>();
        tops.forEach((name, part) -> add(builder, ROOT, name, part, parts, top, true, used));
        boolean[] tops2 = new boolean[top.size()];
        for (int i = 0; i < tops2.length; i++) {
            tops2[i] = top.get(i);
        }
        return new PartRig(builder.build(), parts.toArray(new ModelPart[0]), tops2);
    }

    private static void add(Armature.Builder builder, String parent, String name, ModelPart part, List<ModelPart> parts,
                            List<Boolean> top, boolean isTop, Map<String, Integer> used) {
        // Two parts may share a name in different branches; the second gets a number.
        int seen = used.merge(name, 1, Integer::sum);
        String unique = seen == 1 ? name : name + "_" + seen;
        builder.joint(unique, parent, transform(part.getInitialPose()));
        parts.add(part);
        top.add(isTop);
        childrenOf(part).forEach((child, childPart) -> add(builder, unique, child, childPart, parts, top, false, used));
    }

    private static JointTransform transform(PartPose pose) {
        return JointTransform.of(new Vector3f(pose.x, pose.y, pose.z),
                new Quaternionf().rotationZYX(pose.zRot, pose.yRot, pose.xRot), new Vector3f(1, 1, 1));
    }

    @Override
    public Armature armature() {
        return armature;
    }

    @Override
    public boolean biped() {
        return false;
    }

    /** The joint names, for the dev command that lists a mob's parts. */
    public List<String> names() {
        return armature.joints().stream().map(joint -> joint.name()).toList();
    }

    @Override
    public Pose capture(EntityModel<?> model) {
        Pose pose = armature.bindPose();
        for (int i = 1; i < parts.length; i++) {
            ModelPart part = parts[i];
            pose.set(i, JointTransform.of(new Vector3f(part.x, part.y, part.z),
                    new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot),
                    new Vector3f(part.xScale, part.yScale, part.zScale)));
        }
        return pose;
    }

    @Override
    public void apply(EntityModel<?> model, Pose pose, Pose captured) {
        JointTransform root = pose.get(0);
        boolean moved = !same(root, JointTransform.IDENTITY);
        Vector3f euler = new Vector3f();
        for (int i = 1; i < parts.length; i++) {
            JointTransform transform = pose.get(i);
            if (moved && top[i]) {
                // The whole body turns about its feet and moves with the root.
                Vector3f at = new Vector3f(transform.translation()).sub(FEET);
                root.rotation().transform(at).add(FEET).add(root.translation());
                transform = JointTransform.of(at, new Quaternionf(root.rotation()).mul(transform.rotation()),
                        transform.scale());
            } else if (same(transform, captured.get(i))) {
                continue;
            }
            ModelPart part = parts[i];
            part.x = transform.translation().x();
            part.y = transform.translation().y();
            part.z = transform.translation().z();
            transform.rotation().getEulerAnglesZYX(euler);
            part.xRot = euler.x;
            part.yRot = euler.y;
            part.zRot = euler.z;
            part.xScale = transform.scale().x();
            part.yScale = transform.scale().y();
            part.zScale = transform.scale().z();
        }
    }

    private static boolean same(JointTransform a, JointTransform b) {
        return a.translation().distanceSquared(b.translation()) < 1e-8f && close(a.rotation(), b.rotation())
                && a.scale().distanceSquared(b.scale()) < 1e-8f;
    }

    private static boolean close(Quaternionfc a, Quaternionfc b) {
        float dot = a.x() * b.x() + a.y() * b.y() + a.z() * b.z() + a.w() * b.w();
        return Math.abs(Math.abs(dot) - 1) < 1e-7f;
    }
}
