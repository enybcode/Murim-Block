package com.murimblock.combat.preview;

/** Converts accepted server ages to local simulation ticks, never wall-clock time. */
public final class PreviewPlayback {
    private PreviewState snapshot = PreviewState.initial();
    private long localAnchor;
    private double ageAnchor;

    public void observe(PreviewState incoming, long localTick) {
        if (incoming.equals(snapshot) || incoming.generation() < snapshot.generation()
                || (incoming.generation() == snapshot.generation()
                && (incoming.sampledAt() < snapshot.sampledAt() || (!snapshot.playing() && snapshot.generation() != 0)))) {
            return;
        }
        double previousAge = age(localTick, 0);
        boolean newAction = incoming.generation() != snapshot.generation();
        snapshot = incoming;
        ageAnchor = newAction ? incoming.sampledAge() : Math.max(previousAge, incoming.sampledAge());
        localAnchor = localTick;
    }

    public float age(long localTick, float partialTick) {
        if (!Float.isFinite(partialTick)) throw new IllegalArgumentException("Non-finite partial tick");
        return (float) Math.min(snapshot.durationTicks(), ageAnchor + Math.max(0, localTick - localAnchor)
                + Math.clamp(partialTick, 0, 1));
    }

    public boolean isActive(long localTick, float partialTick) {
        return snapshot.playing() && age(localTick, partialTick) < snapshot.durationTicks();
    }

    public long generation() {
        return snapshot.generation();
    }
}
