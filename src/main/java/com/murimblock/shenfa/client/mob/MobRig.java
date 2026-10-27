package com.murimblock.shenfa.client.mob;

import com.murimblock.shenfa.Armature;
import com.murimblock.shenfa.Pose;
import net.minecraft.client.model.EntityModel;

/**
 * How Shenfa reads and drives a mob's vanilla model: vanilla poses the model each frame (its walk, its look, its
 * bow), the rig reads that pose as a skeleton's, Shenfa lays its layers on it, and the rig writes the result back
 * into the model's parts before they are drawn (with everything drawn from them: armor, held items, glowing eyes).
 */
public interface MobRig {
    Armature armature();

    /** Whether the rig is the biped's: the mob plays the player's clips (Blender's), not clips keyed on its parts. */
    boolean biped();

    /** The model's pose this frame, as the armature's joints. */
    Pose capture(EntityModel<?> model);

    /** Writes {@code pose} into the model's parts where it differs from {@code captured}. */
    void apply(EntityModel<?> model, Pose pose, Pose captured);
}
