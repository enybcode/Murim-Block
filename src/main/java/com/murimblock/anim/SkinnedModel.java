package com.murimblock.anim;

import java.util.List;

/** A skeleton and the parts of the body skinned to it, as loaded from a glTF file. */
public record SkinnedModel(Armature armature, List<SkinnedMesh> parts) {
    /** The part with this name, or null. */
    public SkinnedMesh part(String name) {
        for (SkinnedMesh part : parts) {
            if (part.name().equals(name)) {
                return part;
            }
        }
        return null;
    }
}
