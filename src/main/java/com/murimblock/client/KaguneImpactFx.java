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
import com.murimblock.martial.KaguneState;
import com.murimblock.network.KaguneImpactPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Vector3f;

/**
 * A kagune's blow landing, on this client (Enzo, 18/10/2026: "no particles but at the impact; use all we have to make it
 * stylish"). Nothing shows while the kagune strikes; where it lands:
 * <ul>
 *   <li>a flash of light (Photon), deep red for the tentacles, red and violet for the centipede's bite;</li>
 *   <li>a spray of blood thrown out from the wound away from the striker, falling and splashing on the ground;</li>
 *   <li>the view shaken, hard for the one struck, lighter for the striker;</li>
 *   <li>the hit-stop: the striker's kagune held still a moment on the blow (KaguneStrikes reads it).</li>
 * </ul>
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class KaguneImpactFx {
    private static final ResourceLocation DROP = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/particle/blood_droplet.png");
    private static final int[] BLOOD = {0xFF6E0812, 0xFF9C1020, 0xFFC81E30, 0xFFE8505E};
    /** How long the kagune is held on the blow (ticks), and the view shaken (ticks). */
    public static final float HIT_STOP_TICKS = 2.5F;
    private static final int SHAKE_TICKS = 7;
    private record Burst(ParticleEmitter emitter, long until) { }
    private static final List<Burst> BURSTS = new ArrayList<>();
    /** When each striker's last blow landed (client game time), for the hit-stop. */
    private static final Map<Integer, Long> LANDED = new HashMap<>();
    private static long shakeSince = Long.MIN_VALUE;
    private static float shakeStrength;

    private KaguneImpactFx() { }

    /** Game time (client) at which this striker's last blow landed, or none. */
    static Long landed(int striker) {
        return LANDED.get(striker);
    }

    public static void impact(KaguneImpactPayload p) {
        Minecraft mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) return;
        long now = level.getGameTime();
        LANDED.put(p.attacker(), now);
        boolean bite = p.strike() == KaguneState.STRIKE_BITE || p.strike() == KaguneState.STRIKE_BURROW || p.strike() == KaguneState.STRIKE_CONSTRICT;
        Vec3 at = new Vec3(p.x(), p.y(), p.z());
        if (PhotonLights.isEnabled()) {
            PhotonLights.flash(new DynamicLight().point().at(p.x(), p.y(), p.z()).color(0.85F, 0.05F, 0.08F)
                    .intensity(bite ? 2.6F : 1.8F).range(bite ? 6 : 4.5F).sourceRadius(0.3F), bite ? 9 : 6);
            if (bite) PhotonLights.flash(new DynamicLight().point().at(p.x(), p.y(), p.z()).color(0.55F, 0.12F, 0.9F)
                    .intensity(1.4F).range(3).sourceRadius(0.2F), 5);
        }
        // Thrown out of the wound, away from the striker.
        var striker = level.getEntity(p.attacker());
        Vec3 away = striker == null ? new Vec3(0, 0, 1) : at.subtract(striker.position()).multiply(1, 0, 1);
        away = away.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : away.normalize();
        spray(level, at, away, bite ? 46 : 26, now ^ p.victim());
        if (mc.player != null && (mc.player.getId() == p.victim() || mc.player.getId() == p.attacker())) {
            shakeSince = now;
            shakeStrength = mc.player.getId() == p.victim() ? (bite ? 3.2F : 2.2F) : (bite ? 1.4F : 0.9F);
        }
    }

    private static void spray(net.minecraft.client.multiplayer.ClientLevel level, Vec3 at, Vec3 away, int count, long seed) {
        var emitter = new ParticleEmitter();
        emitter.setName("murimblock:kagune_impact");
        var c = emitter.config;
        c.setDuration(40); c.setLooping(true); c.setMaxParticles(count + 4);
        c.setSimulationSpace(ParticleConfig.Space.World);
        c.setStartLifetime(NumberFunction.constant(24));
        c.setStartSpeed(NumberFunction.constant(0));
        c.setStartSize(new NumberFunction3(.12, .12, .12));
        c.setStartColor(NumberFunction.color(BLOOD[1]));
        c.emission.setEmissionRate(NumberFunction.constant(0));
        c.shape.setShape(new Dot());
        c.physics.setEnable(true);
        c.physics.setGravity(NumberFunction.constant(.9));
        c.physics.setHasCollision(true);
        c.physics.setFriction(NumberFunction.constant(.96));
        c.physics.setCollidedFriction(NumberFunction.constant(.2));
        c.physics.setBounceChance(NumberFunction.constant(.15));
        c.physics.setBounceRate(NumberFunction.constant(.2));
        c.lights.setEnable(true);
        c.lights.setSkyLight(NumberFunction.constant(11)); c.lights.setBlockLight(NumberFunction.constant(11));
        c.renderer.getMaterials().clear();
        c.renderer.getMaterials().add(new MaterialSetting(new TextureMaterial(DROP)).setCull(false).setDepthMask(false));
        emitter.emit((IEffectExecutor) () -> level);
        emitter.updatePos(new Vector3f((float) at.x, (float) at.y, (float) at.z));
        RandomSource random = RandomSource.create(seed);
        for (int i = 0; i < count; i++) {
            // A cone away from the striker, up a little; some slow drops that just fall.
            boolean slow = i % 5 == 0;
            Vec3 dir = away.add((random.nextDouble() - 0.5) * 1.3, 0.15 + random.nextDouble() * 0.7, (random.nextDouble() - 0.5) * 1.3).normalize()
                    .scale((slow ? 0.05 : 0.16) + random.nextDouble() * (slow ? 0.06 : 0.24));
            var drop = new TileParticle(emitter, emitter.config);
            drop.setSimPos(new Vector3f((float) at.x, (float) at.y, (float) at.z), true);
            drop.setInternalVelocity(new Vector3f((float) dir.x, (float) dir.y, (float) dir.z));
            float size = 0.04F + random.nextFloat() * (slow ? 0.1F : 0.06F);
            drop.setSize(new Vector3f(size, size, size));
            drop.setARGBColor(BLOOD[random.nextInt(BLOOD.length)]);
            drop.setLifetime(14 + random.nextInt(14));
            emitter.emitParticle(drop);
        }
        BURSTS.add(new Burst(emitter, level.getGameTime() + 40));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        BURSTS.removeIf(burst -> {
            if (level != null && level.getGameTime() < burst.until) return false;
            burst.emitter.remove(true);
            return true;
        });
        if (level == null) LANDED.clear();
        else LANDED.values().removeIf(t -> level.getGameTime() - t > 40);
    }

    /** The view shaken after a blow, dying away. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float t = mc.level.getGameTime() - shakeSince + (float) event.getPartialTick();
        if (t < 0 || t > SHAKE_TICKS) return;
        float amount = shakeStrength * (1 - t / SHAKE_TICKS);
        double n = System.nanoTime() / 1.6e7;
        event.setRoll(event.getRoll() + amount * Mth.sin((float) n));
        event.setPitch(event.getPitch() + amount * 0.8F * Mth.cos((float) (n * 1.9)));
        event.setYaw(event.getYaw() + amount * 0.5F * Mth.sin((float) (n * 2.7)));
    }
}
