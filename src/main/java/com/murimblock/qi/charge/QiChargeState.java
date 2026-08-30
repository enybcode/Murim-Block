package com.murimblock.qi.charge;

public final class QiChargeState {
    private boolean charging;

    public QiChargeState() {
        this(false);
    }

    public QiChargeState(boolean charging) {
        this.charging = charging;
    }

    public boolean isCharging() {
        return charging;
    }

    public void start() {
        charging = true;
    }

    public void stop() {
        charging = false;
    }
}
