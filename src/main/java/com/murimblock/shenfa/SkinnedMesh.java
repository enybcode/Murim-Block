package com.murimblock.shenfa;

/**
 * One part of a skinned model (a head, an arm, a sleeve, a piece of armor...): triangles whose vertices follow up to
 * four joints each. Positions and normals are in model space at rest (pixels), UVs in 0..1 over the part's texture.
 * Joint indices are the armature's. {@code layer} says what the part is drawn with: {@link #SKIN},
 * {@link #SKIN_OUTER} (hat, jacket, sleeves, pants), {@link #ARMOR_OUTER} or {@link #ARMOR_INNER}.
 */
public record SkinnedMesh(String name, String layer, float[] positions, float[] normals, float[] uvs, int[] joints,
                          float[] weights, int[] indices) {
    public static final int INFLUENCES = 4;
    public static final String SKIN = "skin";
    public static final String SKIN_OUTER = "skin_outer";
    public static final String ARMOR_OUTER = "armor_outer";
    public static final String ARMOR_INNER = "armor_inner";

    public int vertexCount() {
        return positions.length / 3;
    }

    public int triangleCount() {
        return indices.length / 3;
    }

    public boolean outer() {
        return layer.equals(SKIN_OUTER);
    }

    public boolean armor() {
        return layer.equals(ARMOR_OUTER) || layer.equals(ARMOR_INNER);
    }
}
