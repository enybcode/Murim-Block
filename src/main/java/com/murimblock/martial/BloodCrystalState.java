package com.murimblock.martial;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Unsaved combat state: every tracking client sees the same transformation and earned crystal count. */
public record BloodCrystalState(int gauge, boolean active, int crystals) {
    /** The blood gauge fills to 20 (the form, the great crystal); the form earns up to 10 dorsal crystals. */
    public static final int MAX_GAUGE = 20, MAX_CRYSTALS = 10;
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
    /** Draws `amount` points out of the gauge (Blood Crystal's charge); none in the form, where the gauge is spent. */
    public BloodCrystalState spend(int amount) {
        return active || amount <= 0 ? this : new BloodCrystalState(Math.max(0, gauge - amount), false, 0);
    }
    public BloodCrystalState consume() { return active ? new BloodCrystalState(0,true,0) : this; }
    public float damageFactor() { return active ? .7F : 1.0F; }
}
