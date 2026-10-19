package com.murimblock.martial;

import com.murimblock.mob.MurimEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * The Raven Messenger Art's doors in the world (RavenGateEntity), opened by the server where something goes from one
 * world to the other, with their sound and a burst of feathers: a pit under what falls in, a door standing where one
 * steps through, a mouth in the air where what was swallowed falls back out.
 */
public final class RavenFx {
    private RavenFx() {
    }

    /** A door of `radius` opened at `at`, of a kind, facing `yaw`, open `life` ticks. */
    public static RavenGateEntity gate(ServerLevel level, Vec3 at, byte kind, float radius, float yaw, int life) {
        RavenGateEntity gate = MurimEntities.RAVEN_GATE.get().create(level);
        if (gate == null) return null;
        gate.shape(kind, radius, yaw, life);
        gate.setPos(at.x, at.y, at.z);
        level.addFreshEntity(gate);
        return gate;
    }

    /** A pit opening under a body's feet (it falls in). */
    public static RavenGateEntity pit(ServerLevel level, Vec3 feet, float radius, int life) {
        level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 0.5F);
        feathers(level, feet.add(0, 0.3, 0), 10, radius);
        return gate(level, feet.add(0, 0.04, 0), RavenGateEntity.PIT, radius, 0, life);
    }

    /** A door standing at `feet`, facing `yaw` (one steps through it). */
    public static RavenGateEntity door(ServerLevel level, Vec3 feet, float yaw, int life) {
        level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 0.7F);
        feathers(level, feet.add(0, 1.1, 0), 8, 0.8);
        return gate(level, feet, RavenGateEntity.DOOR, 1.1F, yaw, life);
    }

    /** A mouth in the air at `at`, facing down (what was swallowed falls out of it). */
    public static RavenGateEntity sky(ServerLevel level, Vec3 at, float radius, int life) {
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.4F);
        feathers(level, at, 12, radius);
        return gate(level, at, RavenGateEntity.SKY, radius, 0, life);
    }

    /** A burst of black feathers round `at`. */
    public static void feathers(ServerLevel level, Vec3 at, int count, double spread) {
        level.sendParticles(RavenParticles.FEATHER.get(), at.x, at.y, at.z, count, spread * 0.5, 0.4, spread * 0.5, 0.04);
    }
}
