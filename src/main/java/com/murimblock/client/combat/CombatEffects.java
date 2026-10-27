package com.murimblock.client.combat;

import com.murimblock.combat.CombatPayloads;
import com.murimblock.combat.CombatState;
import com.murimblock.combat.Moveset;
import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.AttackData;
import com.murimblock.shenfa.client.CameraShake;
import com.murimblock.shenfa.client.PlayerAnimators;
import com.murimblock.shenfa.client.SkeletalClips;
import com.murimblock.shenfa.client.mob.MobRig;
import com.murimblock.shenfa.client.mob.MobShenfa;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * What a blow feels like, beyond Epic Fight (which has no hit stop and no camera shake in its core): both bodies
 * freeze a few ticks, sparks fly where it landed (blunt or cut, a guard's, a parry's flash), its sound, the camera
 * shakes for the one who takes it (less for the one who gives it), and the target reels with a clip of its own
 * skeleton (the player's reactions on the biped, a flinch of the whole body for other mobs).
 */
public final class CombatEffects {
    private CombatEffects() {
    }

    /** For checks: hears every blow the server reports. */
    public static java.util.function.Consumer<CombatPayloads.Hit> probe;

    public static void hit(CombatPayloads.Hit hit, CombatState local) {
        if (probe != null) {
            probe.accept(hit);
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        Entity attacker = level.getEntity(hit.attacker()), target = level.getEntity(hit.target());
        boolean meAttacking = attacker == mc.player, meHit = target == mc.player;
        if (hit.hitStop() > 0) {
            freeze(attacker, hit.hitStop());
            freeze(target, hit.hitStop());
            if (meAttacking || meHit) {
                local.hitStop = Math.max(local.hitStop, hit.hitStop());
            }
        }
        sparks(level, hit);
        sound(level, hit);
        float impact = hit.impact();
        switch (hit.result()) {
            case HIT -> {
                if (meHit) {
                    CameraShake.start(0.35f + impact * 0.3f, 6 + Math.round(impact * 2));
                    if (impact >= 3) {
                        CombatHud.flash(3);
                    }
                } else if (meAttacking) {
                    CameraShake.start(0.08f + impact * 0.08f, 4);
                    if (target instanceof LivingEntity body) {
                        CombatClient.struck(body);
                    }
                }
                AttackData.Stun stun = AttackData.Stun.values()[hit.stun()];
                if (stun != AttackData.Stun.NONE && target instanceof LivingEntity body) {
                    react(body, switch (stun) {
                        case SHORT -> "hit_short";
                        case LONG -> "hit_long";
                        default -> "knockdown";
                    });
                }
            }
            case GUARD -> {
                if (meHit) {
                    CameraShake.start(0.2f + impact * 0.1f, 4);
                }
                if (target instanceof LivingEntity body) {
                    react(body, "guard_hit");
                }
            }
            case PARRY -> {
                if (meHit || meAttacking) {
                    CameraShake.start(0.35f, 5);
                }
                if (attacker instanceof LivingEntity body) {
                    react(body, "parried");
                }
            }
            case GUARD_BREAK -> {
                if (meHit) {
                    CameraShake.start(0.9f, 10);
                    CombatHud.flash(4);
                }
                if (target instanceof LivingEntity body) {
                    react(body, "guard_break");
                }
            }
            case DODGED -> {
            }
        }
    }

    private static void freeze(Entity entity, int ticks) {
        if (entity instanceof AbstractClientPlayer player) {
            PlayerAnimators.freeze(player, ticks);
        } else if (entity instanceof LivingEntity living) {
            MobShenfa.freeze(living, ticks);
        }
    }

    /**
     * Plays a reaction on {@code body}'s own skeleton: the biped's clip of that name ({@code combat/<name>}) for
     * players and humanoid mobs, the whole body's flinch ({@code common/<flinch|stagger|knockdown>}) for the others.
     */
    static void react(LivingEntity body, String name) {
        if (body instanceof AbstractClientPlayer player) {
            Animation clip = SkeletalClips.get(Moveset.clip("combat/" + name));
            if (clip != null) {
                PlayerAnimators.play(player, Animator.Priority.HIGHEST, clip, 1, false);
            }
            return;
        }
        MobRig rig = MobShenfa.rig(body);
        if (rig == null) {
            return;
        }
        ResourceLocation clip = rig.biped() ? Moveset.clip("combat/" + name) : Moveset.clip("common/" + switch (name) {
            case "hit_long", "parried", "guard_break" -> "stagger";
            case "knockdown" -> "knockdown";
            default -> "flinch";
        });
        MobShenfa.play(body, Animator.Priority.HIGHEST, clip, 1, false);
    }

    private static void sparks(ClientLevel level, CombatPayloads.Hit hit) {
        Vec3 at = hit.at();
        RandomSource random = level.random;
        switch (hit.result()) {
            case HIT -> {
                if (hit.cut()) {
                    level.addParticle(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 0, 0, 0);
                }
                burst(level, ParticleTypes.CRIT, at, 6 + Math.round(hit.impact() * 4), 0.5, random);
                if (hit.impact() >= 2) {
                    burst(level, ParticleTypes.POOF, at, 4, 0.15, random);
                }
            }
            case GUARD -> burst(level, ParticleTypes.ELECTRIC_SPARK, at, 8, 0.4, random);
            case PARRY -> {
                level.addParticle(ParticleTypes.FLASH, at.x, at.y, at.z, 0, 0, 0);
                burst(level, ParticleTypes.ELECTRIC_SPARK, at, 16, 0.7, random);
            }
            case GUARD_BREAK -> {
                burst(level, ParticleTypes.ELECTRIC_SPARK, at, 20, 0.9, random);
                burst(level, ParticleTypes.CRIT, at, 12, 0.6, random);
            }
            case DODGED -> burst(level, ParticleTypes.CLOUD, at, 3, 0.05, random);
        }
    }

    private static void burst(ClientLevel level, ParticleOptions particle, Vec3 at, int count, double speed,
                              RandomSource random) {
        for (int i = 0; i < count; i++) {
            level.addParticle(particle, at.x, at.y, at.z, random.nextGaussian() * speed, random.nextGaussian() * speed,
                    random.nextGaussian() * speed);
        }
    }

    private static void sound(ClientLevel level, CombatPayloads.Hit hit) {
        Vec3 at = hit.at();
        switch (hit.result()) {
            case HIT -> {
                SoundEvent sound = hit.cut() ? SoundEvents.PLAYER_ATTACK_SWEEP : hit.impact() >= 1.5f
                        ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK;
                play(level, at, sound, 0.9f, 0.9f + level.random.nextFloat() * 0.2f);
                if (hit.impact() >= 2.5f) {
                    play(level, at, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.8f);
                }
            }
            case GUARD -> play(level, at, SoundEvents.SHIELD_BLOCK, 0.8f, 1.1f);
            case PARRY -> {
                play(level, at, SoundEvents.ANVIL_PLACE, 0.35f, 1.9f);
                play(level, at, SoundEvents.SHIELD_BLOCK, 0.8f, 1.4f);
            }
            case GUARD_BREAK -> play(level, at, SoundEvents.SHIELD_BREAK, 1, 0.8f);
            case DODGED -> play(level, at, SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.7f, 1.2f);
        }
    }

    private static void play(ClientLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch, false);
    }
}
