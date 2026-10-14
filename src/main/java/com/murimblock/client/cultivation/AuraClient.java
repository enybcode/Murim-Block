package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.AuraState;
import com.murimblock.cultivation.CultivationRegistries;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.cultivation.inner.HeartDemonEntity;
import com.murimblock.cultivation.inner.MeditatingBodyEntity;
import com.murimblock.cultivation.inner.QiMoteEntity;
import com.murimblock.martial.MartialArts;
import com.murimblock.network.CultivationPayloads;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.joml.Vector3f;

/**
 * What every client draws of the Qi (docs/cultivation/qi-cultivation.md), from what the server shares:
 * <ul>
 *   <li>the aura of a meditating player (or of a body left in meditation): sparks rising round them, more as the
 *   reserve fills, flames and a ring on the ground when it is full; its colour is the main style's (white without
 *   one, pale azure for the Azure Cloud Sword, amber for the Long Fist);</li>
 *   <li>the dark aura of a Heart Demon, the glow of the motes of Qi;</li>
 *   <li>the breakthroughs: a spiral drawn into the body and a ring (small), a pillar of light and a ring blown out
 *   (great), a burst of dark red and blood (a deviation), the body dissolving into the inner world and back.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class AuraClient {
    static SpriteSet auraSprites, moteSprites;

    private AuraClient() {
    }

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(CultivationRegistries.AURA.get(), sprites -> {
            auraSprites = sprites;
            return new AuraParticle.Provider(sprites);
        });
        event.registerSpriteSet(CultivationRegistries.QI_MOTE.get(), sprites -> {
            moteSprites = sprites;
            return new AuraParticle.Provider(sprites);
        });
    }

    /** The aura's colour for a main style (r, g, b in 0..1). */
    public static float[] colour(Optional<ResourceLocation> art) {
        if (art.filter(MartialArts.AZURE_CLOUD_SWORD::equals).isPresent()) return new float[]{0.62F, 0.86F, 1.0F};
        if (art.filter(MartialArts.LONG_FIST::equals).isPresent()) return new float[]{1.0F, 0.72F, 0.3F};
        return new float[]{0.95F, 0.96F, 1.0F};
    }

    static Optional<ResourceLocation> artOf(String id) {
        return id == null || id.isEmpty() ? Optional.empty() : Optional.ofNullable(ResourceLocation.tryParse(id));
    }

    /** A spark of the aura, tinted. */
    static AuraParticle spark(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, float[] c, boolean mote) {
        SpriteSet sprites = mote ? moteSprites : auraSprites;
        if (sprites == null) return null;
        AuraParticle p = new AuraParticle(level, x, y, z, vx, vy, vz, sprites);
        p.setColor(c[0], c[1], c[2]);
        Minecraft.getInstance().particleEngine.add(p);
        return p;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused() || mc.player == null) return;
        RandomSource random = level.random;
        for (AbstractClientPlayer player : level.players()) {
            if (player.distanceToSqr(mc.player) > 64 * 64 || player.isInvisible()) continue;
            AuraState aura = MeditationService.aura(player);
            if (aura.meditating()) aura(level, random, player.position(), aura.fill(), colour(aura.mainArt()), player.tickCount);
        }
        AABB near = mc.player.getBoundingBox().inflate(48);
        for (Entity e : level.getEntities((Entity) null, near, e -> e instanceof MeditatingBodyEntity || e instanceof HeartDemonEntity || e instanceof QiMoteEntity)) {
            if (e instanceof MeditatingBodyEntity body) aura(level, random, body.position(), 1.0F, colour(artOf(body.art())), body.tickCount);
            else if (e instanceof HeartDemonEntity demon) demonAura(level, random, demon);
            else if (random.nextInt(3) == 0) spark(level, e.getX() + (random.nextDouble() - 0.5) * 0.4, e.getY() + 0.25, e.getZ() + (random.nextDouble() - 0.5) * 0.4,
                    0, 0.02, 0, new float[]{0.75F, 0.92F, 1.0F}, true);
        }
    }

    /**
     * One tick of an aura round a seated body at `feet`: sparks rising round it (more with a fuller reserve), drawn in
     * and swirling; full, flames climbing higher and a ring of light on the ground every half second.
     */
    static void aura(ClientLevel level, RandomSource random, Vec3 feet, float fill, float[] c, int age) {
        int count = 1 + Math.round(fill * 4);
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = 0.45 + random.nextDouble() * 0.35;
            double x = feet.x + Math.cos(a) * r, z = feet.z + Math.sin(a) * r, y = feet.y + random.nextDouble() * 1.1;
            // Turning round the body as it rises.
            double swirl = 0.025;
            spark(level, x, y, z, -Math.sin(a) * swirl - Math.cos(a) * 0.006, 0.025 + random.nextDouble() * 0.04, Math.cos(a) * swirl - Math.sin(a) * 0.006, c, false);
        }
        if (fill >= 0.98F) {
            for (int i = 0; i < 2; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = 0.3 + random.nextDouble() * 0.3;
                AuraParticle p = spark(level, feet.x + Math.cos(a) * r, feet.y + 0.2 + random.nextDouble() * 0.6, feet.z + Math.sin(a) * r,
                        0, 0.08 + random.nextDouble() * 0.06, 0, c, false);
                if (p != null) p.big(0.12F + random.nextFloat() * 0.08F, 22);
            }
            if (age % 10 == 0) ring(level, feet.add(0, 0.05, 0), 1.1, 20, c, 0.0);
        }
    }

    static void demonAura(ClientLevel level, RandomSource random, HeartDemonEntity demon) {
        float[] dark = {0.32F, 0.06F, 0.42F};
        for (int i = 0; i < 2; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            AuraParticle p = spark(level, demon.getX() + Math.cos(a) * 0.45, demon.getY() + random.nextDouble() * 1.9, demon.getZ() + Math.sin(a) * 0.45,
                    0, 0.03 + random.nextDouble() * 0.03, 0, dark, false);
            if (p != null) p.big(0.14F, 26);
        }
    }

    /** A ring of sparks on the ground, still (`speed` 0) or blown outwards. */
    static void ring(ClientLevel level, Vec3 at, double radius, int count, float[] c, double speed) {
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2 / count;
            spark(level, at.x + Math.cos(a) * radius, at.y, at.z + Math.sin(a) * radius, Math.cos(a) * speed, 0.005, Math.sin(a) * speed, c, false);
        }
    }

    // ---------------------------------------------------------------- breakthroughs

    public static void breakthrough(int entityId, int kind) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Entity body = level.getEntity(entityId);
        if (body == null) return;
        RandomSource random = level.random;
        Vec3 feet = body.position();
        float[] c = body instanceof AbstractClientPlayer p ? colour(MeditationService.aura(p).mainArt())
                : body instanceof MeditatingBodyEntity b ? colour(artOf(b.art())) : colour(Optional.empty());
        boolean self = body == mc.player;
        switch (kind) {
            case CultivationPayloads.Breakthrough.MINOR -> {
                // A spiral of motes drawn in from all round, then a ring blown out on the ground.
                for (int i = 0; i < 48; i++) {
                    double t = i / 48.0, a = t * Math.PI * 6, r = 1.8 - t * 1.4, y = feet.y + 0.2 + t * 1.6;
                    spark(level, feet.x + Math.cos(a) * r, y, feet.z + Math.sin(a) * r, -Math.cos(a) * 0.05, 0.01, -Math.sin(a) * 0.05, c, true);
                }
                ring(level, feet.add(0, 0.1, 0), 0.6, 32, c, 0.18);
            }
            case CultivationPayloads.Breakthrough.MAJOR -> {
                // A pillar of light climbing thirty blocks, a great ring, motes rushing in from afar.
                float[] gold = {1.0F, 0.92F, 0.6F};
                for (double y = 0; y < 30; y += 0.35) {
                    for (int k = 0; k < 2; k++) {
                        double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * 0.6;
                        AuraParticle p = spark(level, feet.x + Math.cos(a) * r, feet.y + y, feet.z + Math.sin(a) * r, 0, 0.15, 0, k == 0 ? c : gold, false);
                        if (p != null) p.big(0.2F + random.nextFloat() * 0.15F, 40 + random.nextInt(20));
                    }
                }
                ring(level, feet.add(0, 0.15, 0), 1.0, 64, c, 0.45);
                ring(level, feet.add(0, 0.4, 0), 0.8, 48, gold, 0.3);
                for (int i = 0; i < 80; i++) {
                    double a = random.nextDouble() * Math.PI * 2, r = 5 + random.nextDouble() * 3, y = feet.y + random.nextDouble() * 3;
                    double x = feet.x + Math.cos(a) * r, z = feet.z + Math.sin(a) * r;
                    spark(level, x, y, z, (feet.x - x) * 0.09, (feet.y + 1 - y) * 0.09, (feet.z - z) * 0.09, c, true);
                }
                if (self) ScreenEffects.flash(0xFFFFF4D8, 28);
                ScreenEffects.shake(14, self ? 1.2F : 0.5F);
            }
            case CultivationPayloads.Breakthrough.DEVIATION -> {
                float[] red = {0.75F, 0.08F, 0.08F};
                for (int i = 0; i < 40; i++) {
                    double a = random.nextDouble() * Math.PI * 2, s = 0.05 + random.nextDouble() * 0.12;
                    spark(level, feet.x, feet.y + 0.8, feet.z, Math.cos(a) * s, random.nextDouble() * 0.08, Math.sin(a) * s, red, false);
                }
                DustParticleOptions blood = new DustParticleOptions(new Vector3f(0.45F, 0.02F, 0.02F), 1.2F);
                Vec3 front = Vec3.directionFromRotation(0, body.getYRot()).scale(0.4);
                for (int i = 0; i < 14; i++) {
                    level.addParticle(blood, feet.x + front.x + (random.nextDouble() - 0.5) * 0.2, feet.y + 0.9, feet.z + front.z + (random.nextDouble() - 0.5) * 0.2,
                            front.x * 0.3, -0.05, front.z * 0.3);
                }
                if (self) ScreenEffects.flash(0x99600000, 30);
            }
            case CultivationPayloads.Breakthrough.ENTER, CultivationPayloads.Breakthrough.RETURN -> {
                for (int i = 0; i < 40; i++) {
                    double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * 0.5;
                    spark(level, feet.x + Math.cos(a) * r, feet.y + random.nextDouble() * 1.6, feet.z + Math.sin(a) * r, 0, 0.06 + random.nextDouble() * 0.06, 0, c, true);
                }
                if (self) ScreenEffects.flash(0xFFFFFFFF, kind == CultivationPayloads.Breakthrough.ENTER ? 30 : 24);
            }
            default -> { }
        }
    }
}
