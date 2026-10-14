package com.murimblock.client;

import com.lowdragmc.photon.client.fx.IEffectExecutor;
import com.lowdragmc.photon.client.gameobject.emitter.data.MaterialSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.material.TextureMaterial;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction3;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Dot;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleConfig;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleEmitter;
import com.lowdragmc.photon.client.gameobject.particle.TileParticle;
import com.lowdragmc.photon.client.light.DynamicLight;
import com.lowdragmc.photon.client.light.PhotonLights;
import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.martial.SanguineArt;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/** Genuine Photon particles, driven by the same Epic Fight clock on the owner and every observing client. */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SanguineEffects {
    private static final ResourceLocation SPARK = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/particle/sanguine_spark.png");
    private static final Map<UUID, Effect> ACTIVE = new HashMap<>();
    private static ClientLevel world;
    private static int emittedPoints;
    private record Motion(int kind, float time) { }
    private static final class Effect {
        final ParticleEmitter emitter;
        final int kind;
        float last;
        boolean flashed;
        Effect(ClientLevel level, int kind, float time) {
            this.kind = kind; last = time;
            emitter = new ParticleEmitter();
            emitter.setName("murimblock:sanguine_" + kind);
            var c = emitter.config;
            c.setDuration(80); c.setLooping(true); c.setMaxParticles(384);
            c.setSimulationSpace(ParticleConfig.Space.World);
            c.setStartLifetime(NumberFunction.constant(6));
            c.setStartSpeed(NumberFunction.constant(0));
            c.setStartSize(new NumberFunction3(0.13, 0.13, 0.13));
            c.setStartColor(NumberFunction.color(0xDF981019));
            c.emission.setEmissionRate(NumberFunction.constant(0));
            c.shape.setShape(new Dot());
            c.lights.setEnable(true);
            c.lights.setSkyLight(NumberFunction.constant(9)); c.lights.setBlockLight(NumberFunction.constant(9));
            c.colorOverLifetime.setEnable(true);
            c.sizeOverLifetime.setEnable(true);
            c.renderer.getMaterials().clear();
            c.renderer.getMaterials().add(new MaterialSetting(new TextureMaterial(SPARK)).setCull(false).setDepthMask(false));
            emitter.emit((IEffectExecutor) () -> level);
        }
        void stop() { emitter.remove(true); }
        void dot(Vec3 position) {
            if (emitter.getParticleAmount() + emitter.waitToAdded.size() >= 384) return;
            TileParticle p = new TileParticle(emitter, emitter.config);
            p.setSimPos(new Vector3f((float)position.x, (float)position.y, (float)position.z), true);
            emitter.emitParticle(p); emittedPoints++;
        }
    }
    private SanguineEffects() { }

    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        EpicFightEventHooks.Animation.INIT_ANIMATOR.registerEvent(e -> {
            if (e.getEntityPatch() instanceof PlayerPatch<?>) e.getAnimator().addLivingAnimation(MurimAnimations.Motions.SANGUINE_GUARD, MurimAnimations.SANGUINE_GUARD);
        }, "murimblock:sanguine_guard", 0);
        EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_COMPOSITE.registerEvent(e -> {
            var patch = e.getPlayerPatch();
            if ((e.getMotion() == LivingMotions.BLOCK || patch.getHoldingSkill() instanceof GuardSkill)
                    && SanguineArt.guardsBareHanded(patch.getOriginal())) e.setMotion(MurimAnimations.Motions.SANGUINE_GUARD);
        }, "murimblock:sanguine_guard", -10);
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level != world) { ACTIVE.values().forEach(Effect::stop); ACTIVE.clear(); world = mc.level; }
        if (world == null || mc.isPaused() || mc.player == null) return;
        Set<UUID> seen = new HashSet<>();
        for (Player player : world.players()) {
            if (!player.isAlive() || player.distanceToSqr(mc.player) > 48 * 48) continue;
            var patch = EpicFightBridge.patch(player);
            Motion motion = motion(patch);
            if (motion == null) continue;
            int kind = motion.kind;
            float time = motion.time;
            Effect effect = ACTIVE.get(player.getUUID());
            if (effect != null && (effect.kind != kind || time + 0.001F < effect.last)) { effect.stop(); ACTIVE.remove(player.getUUID()); effect = null; }
            if (effect == null) {
                if (ACTIVE.size() >= 16) continue;
                effect = new Effect(world, kind, time); ACTIVE.put(player.getUUID(), effect);
            }
            seen.add(player.getUUID());
            if (time > effect.last || time == 0) draw(effect, player, time);
            effect.last = time;
        }
        for (var entity : world.entitiesForRendering()) {
            if (!(entity instanceof com.murimblock.martial.BloodCrystalEntity crystal) || crystal.state()!=com.murimblock.martial.BloodCrystalEntity.FLYING
                    || crystal.distanceToSqr(mc.player)>48*48) continue;
            Effect effect=ACTIVE.get(crystal.getUUID());
            if(effect==null) {if(ACTIVE.size()>=16)continue;effect=new Effect(world,7,0);ACTIVE.put(crystal.getUUID(),effect);}
            seen.add(crystal.getUUID());
            Vec3 at=crystal.position(), velocity=crystal.getDeltaMovement();
            effect.emitter.updatePos(new Vector3f((float)at.x,(float)at.y,(float)at.z));
            for(int i=0;i<12;i++)effect.dot(at.subtract(velocity.scale(i/12.0)));
        }
        ACTIVE.entrySet().removeIf(entry -> { if (seen.contains(entry.getKey())) return false; entry.getValue().stop(); return true; });
    }

    private static Motion motion(PlayerPatch<?> patch) {
        if (patch == null) return null;
        var animations = java.util.List.of(MurimAnimations.SANGUINE_CRYSTAL_CHARGE, MurimAnimations.SANGUINE_CRYSTAL_THROW,
                MurimAnimations.SCARLET_TEAR, MurimAnimations.SANGUINE_CRYSTAL_FORM, MurimAnimations.SANGUINE_CRYSTAL_BARRAGE,
                MurimAnimations.SANGUINE_STRAIGHT, MurimAnimations.SANGUINE_CROSS, MurimAnimations.SANGUINE_RISE);
        int[] kinds = {5,1,2,3,6,4,4,4};
        // Non-stiff M1 uses the composite layer: query each animation's own clock.
        for (int i=0;i<animations.size();i++) {
            var animation=animations.get(i);var clock=patch.getAnimator().getPlayerFor(animation);
            if (clock!=null&&!clock.isEmpty()&&clock.getRealAnimation()==animation) return new Motion(kinds[i],clock.getElapsedTime());
        }
        return null;
    }

    private static Vec3 point(Player p, double side, double height, double ahead) {
        Vec3 forward = Vec3.directionFromRotation(0, p.getYRot());
        return p.position().add(-forward.z * side + forward.x * ahead, height, forward.x * side + forward.z * ahead);
    }

    private static void draw(Effect e, Player p, float t) {
        e.emitter.updatePos(new Vector3f((float)p.getX(), (float)p.getY(), (float)p.getZ()));
        if (e.kind == 5 || e.kind == 1 && t < .40F) {
            Vec3 hand=SanguineArt.handPosition(p,1);
            double radius=e.kind==5?.10+.07*Math.sin(t*8):.17;
            for(int i=0;i<14;i++) {
                double a=i*Math.PI/7+t*7;
                e.dot(hand.add(Math.cos(a)*radius,Math.sin(a)*radius,Math.sin(a*.5)*radius));
            }
        } else if (e.kind == 2 && t >= .30F && t <= .72F) {
            double progress = Math.min(1, (t - .30) / .28), radius = .5 + progress * 2.4;
            for (int i = 0; i < 48; i++) {
                double a = -1.35 + i / 47.0 * 2.7;
                e.dot(point(p, Math.sin(a) * radius, 1.0 + .2 * Math.cos(a * 2), Math.cos(a) * radius));
            }
        } else if (e.kind == 3 && t >= .20F && t <= .80F) {
            double rise=(t-.20)/.60;
            for(int i=0;i<32;i++) {
                double a=i*Math.PI/16+t*5;
                e.dot(point(p,Math.cos(a)*.45,rise*1.8,Math.sin(a)*.45));
            }
        } else if (e.kind == 6 && t >= .18F && t <= 1.25F) {
            for(int i=0;i<12;i++) {
                double a=i*Math.PI/6+t*8;
                e.dot(point(p,Math.cos(a)*.6,1.4+Math.sin(a)*.2,-.7));
            }
        } else if (e.kind == 4 && t >= .10F && t <= .34F) {
            for (int hand = -1; hand <= 1; hand += 2) for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6 + t * 16;
                e.dot(point(p, hand * .4 + Math.cos(a) * .14, 1.12 + Math.sin(a) * .14, .6 + Math.sin(t * 8) * .4));
            }
        }
        float peak = e.kind == 1 ? .30F : e.kind == 2 ? .43F : e.kind == 3 ? .55F : e.kind == 6 ? .30F : .18F;
        if (!e.flashed && e.last < peak && t >= peak) {
            e.flashed = true;
            if (PhotonLights.isEnabled()) {
                Vec3 at = point(p, 0, 1, e.kind == 3 ? 2.0 : 1.0);
                PhotonLights.flash(new DynamicLight().point().at(at.x, at.y, at.z).color(.55F, .018F, .025F)
                        .intensity(e.kind == 3 ? 2.2F : 1.0F).range(e.kind == 3 ? 3 : 1.8F).sourceRadius(.2F), 4);
            }
        }
    }

    /** Development tour evidence: actual Photon particles queued, never a proxy damage calculation. */
    public static int emittedPoints() { return emittedPoints; }
    public static int activeEffects() { return ACTIVE.size(); }
}
