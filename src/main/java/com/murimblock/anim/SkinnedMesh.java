package com.murimblock.anim;

/**
 * One part of a skinned model (a head, an arm, a sleeve...): triangles whose vertices follow up to four joints each.
 * Positions and normals are in model space at rest (pixels), UVs in 0..1 over the skin. Joint indices are the
 * armature's.
 */
public record SkinnedMesh(String name, boolean outer, float[] positions, float[] normals, float[] uvs, int[] joints,
                          float[] weights, int[] indices) {
    public static final int INFLUENCES = 4;

    public int vertexCount() {
        return positions.length / 3;
    }

    public int triangleCount() {
        return indices.length / 3;
    }
}
