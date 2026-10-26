package com.murimblock.anim;

/**
 * One joint of an {@link Armature}: its place in the armature's list, its parent's index (-1 for the root) and
 * its rest transform relative to the parent.
 */
public record Joint(String name, int index, int parent, JointTransform bind) {
    public boolean isRoot() {
        return parent < 0;
    }
}
