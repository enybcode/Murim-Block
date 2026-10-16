package com.murimblock.bloodsect.ninja;

/** Per-hand animation clock. Frame ranges match the baked OBJ poses. */
public final class BladeAnimation {
    private Object heldItem;
    private int equippedAt;

    public void update(Object item, int tick) {
        if (item != this.heldItem) {
            this.heldItem = item;
            this.equippedAt = tick;
        }
    }

    public int frame(float age, float swing, boolean swingingHand) {
        if (this.heldItem == null) return 24;
        if (swingingHand && swing > 0 && swing < 1) {
            return 40 + Math.min(23, (int)(swing * 24));
        }
        float elapsed = Math.max(0, age - this.equippedAt);
        if (elapsed < 12) return Math.min(23, (int)(elapsed * 2));
        return 24 + (int)(((elapsed - 12) % 32) / 2);
    }
}
