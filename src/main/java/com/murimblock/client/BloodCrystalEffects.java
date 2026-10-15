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
import com.murimblock.martial.BloodCrystalEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

/**
 * Light and shards where a blood crystal breaks (Photon, client only). Every break lights its surroundings blood red
 * for a moment; only the great crystal's explosion throws particles: shards of crystal in its own reds, flying out,
 * falling and bouncing on the ground. The art otherwise shows no particles.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class BloodCrystalEffects {
    private static final ResourceLocation SHARD = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/particle/blood_shard.png");
    /** The crystal's reds, darkest to palest (its texture's shades). */
    private static final int[] REDS = {0xFF8E1424, 0xFFC0283B, 0xFFE8566A, 0xFFFFB3BD};
    static final int EXPLOSION_SHARDS = 160;
    private static final List<Burst> BURSTS = new ArrayList<>();
    private static int shardsThrown;
    private record Burst(ParticleEmitter emitter, long until) { }

    private BloodCrystalEffects() {
    }

    /** A crystal has broken (its state just changed): light, and for the great one its shards. */
    public static void broke(BloodCrystalEntity crystal) {
        // A crystal come away from a body at the form's end just breaks on the ground: no light.
        if (crystal.shed()) return;
        boolean great = crystal.state() == BloodCrystalEntity.EXPLODE;
        Vec3 at = crystal.centre();
        if (PhotonLights.isEnabled()) {
            PhotonLights.flash(new DynamicLight().point().at(at.x, at.y, at.z).color(.6F, .03F, .05F)
                    .intensity(great ? (crystal.sword() ? 2.4F : 3.2F) : .9F).range(great ? (crystal.sword() ? 5 : 7) : 2.2F).sourceRadius(great ? .8F : .2F), great ? 10 : 5);
        }
        if (great) explode(crystal, at);
    }

    private static void explode(BloodCrystalEntity crystal, Vec3 at) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var emitter = new ParticleEmitter();
        emitter.setName("murimblock:blood_crystal_explosion");
        var c = emitter.config;
        c.setDuration(60); c.setLooping(true); c.setMaxParticles(EXPLOSION_SHARDS + 8);
        c.setSimulationSpace(ParticleConfig.Space.World);
        c.setStartLifetime(NumberFunction.constant(40));
        c.setStartSpeed(NumberFunction.constant(0));
        c.setStartSize(new NumberFunction3(.2, .2, .2));
        c.setStartColor(NumberFunction.color(REDS[1]));
        c.emission.setEmissionRate(NumberFunction.constant(0));
        c.shape.setShape(new Dot());
        c.physics.setEnable(true);
        c.physics.setGravity(NumberFunction.constant(.6));
        c.physics.setHasCollision(true);
        c.physics.setFriction(NumberFunction.constant(.98));
        c.physics.setCollidedFriction(NumberFunction.constant(.6));
        c.physics.setBounceChance(NumberFunction.constant(.5));
        c.physics.setBounceRate(NumberFunction.constant(.35));
        c.lights.setEnable(true);
        c.lights.setSkyLight(NumberFunction.constant(12)); c.lights.setBlockLight(NumberFunction.constant(12));
        c.colorOverLifetime.setEnable(true);
        c.sizeOverLifetime.setEnable(true);
        c.renderer.getMaterials().clear();
        c.renderer.getMaterials().add(new MaterialSetting(new TextureMaterial(SHARD)).setCull(false).setDepthMask(false));
        emitter.emit((IEffectExecutor) () -> level);
        emitter.updatePos(new Vector3f((float) at.x, (float) at.y, (float) at.z));
        RandomSource random = RandomSource.create(crystal.getId() * 31L + crystal.tickCount);
        int count = crystal.sword() ? EXPLOSION_SHARDS / 2 : EXPLOSION_SHARDS;
        for (int i = 0; i < count; i++) {
            // Mostly outward and up, a ring of them flat along the ground.
            boolean ring = i % 4 == 0;
            double a = random.nextDouble() * Math.PI * 2, up = ring ? .05 + random.nextDouble() * .1 : -.2 + random.nextDouble() * 1.1;
            double speed = (ring ? .32 : .18) + random.nextDouble() * .28;
            var dir = new Vec3(Math.cos(a), up, Math.sin(a)).normalize().scale(speed);
            var shard = new TileParticle(emitter, emitter.config);
            shard.setSimPos(new Vector3f((float) at.x, (float) at.y, (float) at.z), true);
            shard.setInternalVelocity(new Vector3f((float) dir.x, (float) dir.y, (float) dir.z));
            float size = .12F + random.nextFloat() * .26F;
            shard.setSize(new Vector3f(size, size, size));
            shard.setRotation(new Vector3f(0, 0, random.nextFloat() * 6.283F));
            shard.setARGBColor(REDS[random.nextInt(REDS.length)]);
            shard.setLifetime(18 + random.nextInt(16));
            emitter.emitParticle(shard);
            shardsThrown++;
        }
        BURSTS.add(new Burst(emitter, level.getGameTime() + 50));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        BURSTS.removeIf(burst -> {
            if (level != null && level.getGameTime() < burst.until) return false;
            burst.emitter.remove(true);
            return true;
        });
    }

    /** Development tour evidence: shards thrown by explosions so far. */
    public static int shardsThrown() {
        return shardsThrown;
    }
}
