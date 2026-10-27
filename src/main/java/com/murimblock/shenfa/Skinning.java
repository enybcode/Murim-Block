package com.murimblock.shenfa;

import org.joml.Matrix4f;

/** Linear blend skinning: moves a mesh's rest vertices to where a pose puts them. */
public final class Skinning {
    private Skinning() {
    }

    /**
     * Writes each vertex's skinned position and normal (model space) into {@code positions} and {@code normals},
     * which must be as long as the mesh's. {@code skin} comes from {@link Pose#toSkinMatrices}.
     */
    public static void skin(SkinnedMesh mesh, Matrix4f[] skin, float[] positions, float[] normals) {
        float[] restPositions = mesh.positions();
        float[] restNormals = mesh.normals();
        int[] joints = mesh.joints();
        float[] weights = mesh.weights();
        for (int v = 0; v < mesh.vertexCount(); v++) {
            float px = restPositions[v * 3], py = restPositions[v * 3 + 1], pz = restPositions[v * 3 + 2];
            float nx = restNormals[v * 3], ny = restNormals[v * 3 + 1], nz = restNormals[v * 3 + 2];
            float x = 0, y = 0, z = 0, a = 0, b = 0, c = 0;
            for (int i = 0; i < SkinnedMesh.INFLUENCES; i++) {
                float w = weights[v * SkinnedMesh.INFLUENCES + i];
                if (w == 0) {
                    continue;
                }
                Matrix4f m = skin[joints[v * SkinnedMesh.INFLUENCES + i]];
                x += w * (m.m00() * px + m.m10() * py + m.m20() * pz + m.m30());
                y += w * (m.m01() * px + m.m11() * py + m.m21() * pz + m.m31());
                z += w * (m.m02() * px + m.m12() * py + m.m22() * pz + m.m32());
                a += w * (m.m00() * nx + m.m10() * ny + m.m20() * nz);
                b += w * (m.m01() * nx + m.m11() * ny + m.m21() * nz);
                c += w * (m.m02() * nx + m.m12() * ny + m.m22() * nz);
            }
            float length = (float) Math.sqrt(a * a + b * b + c * c);
            if (length > 1e-6f) {
                a /= length;
                b /= length;
                c /= length;
            }
            positions[v * 3] = x;
            positions[v * 3 + 1] = y;
            positions[v * 3 + 2] = z;
            normals[v * 3] = a;
            normals[v * 3 + 1] = b;
            normals[v * 3 + 2] = c;
        }
    }
}
