package com.murimblock.combat;

/**
 * Where a fighter is in their M1 chain, with no game types so it can be unit tested. A click within the style's
 * chain window after a move ends goes on to the next move (after the last, the first again); later, or after a blow
 * taken, the chain starts over.
 */
public final class ChainTracker {
    /** Index of the last move thrown, or -1 when the chain starts over. */
    private int last = -1;
    /** Ticks since that move ended. */
    private int idle;

    /** The chain index the next strike should use. */
    public int next(int window, int size) {
        return last >= 0 && idle <= window && size > 0 ? (last + 1) % size : 0;
    }

    public void thrown(int index) {
        last = index;
        idle = 0;
    }

    /** Once a tick: idle time only counts while no move plays. */
    public void tick(boolean striking) {
        if (!striking && last >= 0) {
            idle++;
        }
    }

    public void reset() {
        last = -1;
        idle = 0;
    }
}
