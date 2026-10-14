package com.murimblock.martial;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Unsaved combat state: every tracking client sees the same transformation and earned crystal count. */
public record BloodCrystalState(int gauge, boolean active, int crystals) {
    public static final int MAX_GAUGE = 10, MAX_CRYSTALS = 10;
    public static final BloodCrystalState EMPTY = new BloodCrystalState(0, false, 0);
    public static final StreamCodec<ByteBuf, BloodCrystalState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BloodCrystalState::gauge,
            ByteBufCodecs.BOOL, BloodCrystalState::active,
            ByteBufCodecs.VAR_INT, BloodCrystalState::crystals, BloodCrystalState::new);
    public BloodCrystalState {
        gauge = active ? 0 : Math.clamp(gauge, 0, MAX_GAUGE);
        crystals = active ? Math.clamp(crystals, 0, MAX_CRYSTALS) : 0;
    }
    public BloodCrystalState gain(int amount) {
        if (amount <= 0) return this;
        return active ? new BloodCrystalState(0, true, Math.min(MAX_CRYSTALS, crystals + Math.min(amount,MAX_CRYSTALS)))
                : new BloodCrystalState(Math.min(MAX_GAUGE, gauge + Math.min(amount,MAX_GAUGE)), false, 0);
    }
    public BloodCrystalState activate() {
        return !active && gauge == MAX_GAUGE ? new BloodCrystalState(0, true, 0) : this;
    }
    public BloodCrystalState consume() { return active ? new BloodCrystalState(0,true,0) : this; }
    public float damageFactor() { return active ? .85F : 1.0F; }
}
