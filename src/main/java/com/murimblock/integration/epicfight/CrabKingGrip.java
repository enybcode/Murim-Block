package com.murimblock.integration.epicfight;

/**
 * Written by scripts/animation/crab-king-moves.cjs (do not edit): where the Crab King's great pincer holds its catch,
 * the point halfway between the middles of its two fingers, at his size, in blocks from his feet along his right,
 * his front and up (the root's own forward step left out: the game moves his body for it).
 */
public final class CrabKingGrip {
    /** One point every {@link #STEP} seconds of crab_king_grab_hold, from 0 to its release. */
    public static final float STEP = 0.05F;
    public static final float[][] HOLD = {
            {0.846F, 2.395F, 1.837F},
            {0.829F, 2.413F, 2.078F},
            {0.810F, 2.407F, 2.322F},
            {0.787F, 2.377F, 2.566F},
            {0.761F, 2.323F, 2.808F},
            {0.733F, 2.245F, 3.044F},
            {0.702F, 2.144F, 3.274F},
            {0.669F, 2.021F, 3.493F},
            {0.633F, 1.876F, 3.700F},
            {0.596F, 1.712F, 3.892F},
            {0.557F, 1.530F, 4.068F},
            {0.573F, 1.498F, 4.095F},
            {0.590F, 1.465F, 4.119F},
            {0.608F, 1.430F, 4.141F},
            {0.627F, 1.392F, 4.161F},
            {0.648F, 1.353F, 4.178F},
            {0.670F, 1.312F, 4.192F},
            {0.499F, 1.507F, 4.002F},
            {0.496F, 1.519F, 3.992F},
            {0.493F, 1.530F, 3.982F},
            {0.519F, 1.494F, 4.024F},
            {0.566F, 1.422F, 4.096F},
            {0.614F, 1.343F, 4.162F},
            {0.575F, 1.402F, 4.113F},
            {0.536F, 1.456F, 4.059F},
            {0.499F, 1.507F, 4.002F},
            {0.496F, 1.519F, 3.992F},
            {0.493F, 1.530F, 3.982F},
            {0.519F, 1.494F, 4.024F},
            {0.566F, 1.422F, 4.096F},
            {0.614F, 1.343F, 4.162F},
            {0.555F, 1.429F, 4.087F},
            {0.499F, 1.507F, 4.002F},
            {0.498F, 1.514F, 3.996F},
            {0.496F, 1.521F, 3.990F},
            {0.494F, 1.528F, 3.984F},
            {0.492F, 1.534F, 3.978F},
            {0.594F, 0.903F, 4.347F},
            {0.486F, 0.235F, 4.534F},
            {0.215F, -0.359F, 4.549F},
            {-0.152F, -0.811F, 4.432F},
            {-0.519F, -0.037F, 4.213F},
            {-0.631F, -0.042F, 4.132F},
            {-0.736F, -0.038F, 4.042F},
            {-0.832F, -0.025F, 3.945F},
            {-0.918F, -0.005F, 3.842F},
            {-0.084F, 0.102F, 4.408F},
            {0.451F, 1.244F, 4.251F},
            {-0.247F, 2.339F, 3.169F},
            {-1.445F, 1.930F, 1.689F}
    };
    /** Where the jaws shut in crab_king_grab, at its catch. */
    public static final float[] CATCH = {0.846F, 2.395F, 1.837F};

    private CrabKingGrip() { }

    /** The grip at t seconds into the hold, between the two nearest points. */
    public static float[] at(float t) {
        float x = Math.max(0, t / STEP);
        int i = Math.min(HOLD.length - 1, (int) x), j = Math.min(HOLD.length - 1, i + 1);
        float k = Math.min(1, x - i);
        return new float[] {HOLD[i][0] + (HOLD[j][0] - HOLD[i][0]) * k, HOLD[i][1] + (HOLD[j][1] - HOLD[i][1]) * k, HOLD[i][2] + (HOLD[j][2] - HOLD[i][2]) * k};
    }
}
