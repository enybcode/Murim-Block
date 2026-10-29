package com.murimblock.shenfa.client;

import com.murimblock.Murimblock;
import com.murimblock.shenfa.Biped;
import com.murimblock.shenfa.ClipEvent;
import com.murimblock.shenfa.Joint;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Runs on the client the events a player's clips passed ({@link ClipEvent}): sounds, particles at a joint,
 * footsteps, the camera shaking for the player who struck. Server events wait for the server to play clips.
 */
public final class ClipEvents {
    /** Settings already reported as wrong, so a looping clip does not fill the log. */
    private static final Set<String> REPORTED = new HashSet<>();
    /** For checks: hears every event run, with its player. */
    private static Consumer<ClipEvent> probe;

    private ClipEvents() {
    }

    public static void probe(Consumer<ClipEvent> listener) {
        probe = listener;
    }

    public static void run(LivingEntity player, List<ClipEvent> events) {
        for (ClipEvent event : events) {
            if (!event.side().client()) {
                continue;
            }
            if (probe != null) {
                probe.accept(event);
            }
            switch (event.type()) {
                case "sound" -> sound(player, event);
                case "particle" -> particle(player, event);
                case "footstep" -> Footsteps.step(player, player.position(), event.number("volume", 1));
                case "fracture" -> {
                    // The ground breaks this far ahead of the body; every client draws it the same.
                    Vec3 at = player.position().add(Vec3.directionFromRotation(0, player.yBodyRot)
                            .scale(event.number("ahead", 0)));
                    GroundFractures.start(player.level(), at, event.number("radius", 3), player.getId() * 31
                            + player.tickCount);
                }
                case "shake" -> {
                    if (player == Minecraft.getInstance().player) {
                        CameraShake.start(event.number("strength", 0.5f), (int) event.number("ticks", 5));
                    }
                }
                default -> {
                    // A server callback: nothing to do on the client.
                }
            }
        }
    }

    private static void sound(LivingEntity player, ClipEvent event) {
        ResourceLocation id = ResourceLocation.tryParse(event.string("sound", ""));
        if (id == null) {
            report("sound " + event.string("sound", ""));
            return;
        }
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(id)
                .orElseGet(() -> SoundEvent.createVariableRangeEvent(id));
        Vec3 at = player.position();
        player.level().playLocalSound(at.x, at.y, at.z, sound, player.getSoundSource(), event.number("volume", 1),
                event.number("pitch", 1), false);
    }

    private static void particle(LivingEntity player, ClipEvent event) {
        ResourceLocation id = ResourceLocation.tryParse(event.string("particle", ""));
        ParticleType<?> type = id == null ? null : BuiltInRegistries.PARTICLE_TYPE.get(id);
        if (!(type instanceof SimpleParticleType simple)) {
            report("particle " + event.string("particle", "") + " (only plain particles)");
            return;
        }
        // A mob's parts are not followed here: its effects rise from the middle of its body.
        Vec3 at = player instanceof AbstractClientPlayer body ? joint(body, event.string("joint", Biped.ROOT))
                : player.position().add(0, player.getBbHeight() * 0.6, 0);
        int count = (int) event.number("count", 1);
        float spread = event.number("spread", 0.1f), speed = event.number("speed", 0);
        var random = player.getRandom();
        for (int i = 0; i < count; i++) {
            player.level().addParticle(simple, at.x + random.nextGaussian() * spread, at.y + random.nextGaussian() * spread,
                    at.z + random.nextGaussian() * spread, random.nextGaussian() * speed, random.nextGaussian() * speed,
                    random.nextGaussian() * speed);
        }
    }

    /** Where a joint of the player's clip pose is in the world (the pose of the last tick, before the feet are placed). */
    static Vec3 joint(AbstractClientPlayer player, String name) {
        Joint joint = Biped.ARMATURE.find(name);
        Vector3f at = PlayerAnimators.pose(player, 1).toModelMatrices(Biped.ARMATURE)[joint.index()]
                .getTranslation(new Vector3f())
                .mul(SkeletalPlayerRenderer.PLAYER_SCALE / 16 * player.getScale())
                .rotateY((float) Math.toRadians(-player.yBodyRot));
        return player.position().add(at.x, at.y, at.z);
    }

    private static void report(String what) {
        if (REPORTED.add(what)) {
            Murimblock.LOGGER.warn("Clip event: unknown {}", what);
        }
    }
}
