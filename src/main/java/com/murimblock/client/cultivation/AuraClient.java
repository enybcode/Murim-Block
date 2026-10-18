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
        if (art.filter(MartialArts.AZURE_CLOUD_SWORD::equals).isPresent()) return new float[]{0.3F, 0.72F, 1.0F};
        if (art.filter(MartialArts.LONG_FIST::equals).isPresent()) return new float[]{1.0F, 0.65F, 0.18F};
        if (art.filter(MartialArts.SANGUINE_ART::equals).isPresent()) return new float[]{1.0F, 0.16F, 0.42F};
        if (art.filter(MartialArts.CELESTIAL_PEAK_SPEAR::equals).isPresent()) return new float[]{1.0F, 0.86F, 0.3F};
        if (art.filter(MartialArts.TRAVELLER_STAFF::equals).isPresent()) return new float[]{0.45F, 0.92F, 0.3F};
        if (art.filter(MartialArts.MOUNTAIN_SHATTERING_FIST::equals).isPresent()) return new float[]{1.0F, 0.45F, 0.14F};
        if (art.filter(MartialArts.SEVEN_STARS_SWORD::equals).isPresent()) return new float[]{0.32F, 0.4F, 1.0F};
        if (art.filter(MartialArts.THUNDER_SABER::equals).isPresent()) return new float[]{0.62F, 0.4F, 1.0F};
        if (art.filter(MartialArts.MAJIN_FIST::equals).isPresent()) return new float[]{0.7F, 0.1F, 0.12F};
        if (art.filter(MartialArts.GOLDEN_BELL::equals).isPresent()) return new float[]{1.0F, 0.78F, 0.22F};
        return new float[]{0.95F, 0.96F, 1.0F};
    }

    public static Optional<ResourceLocation> artOf(String id) {
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
            // One's own, seen through one's eyes: kept round and below, never across the view.
            boolean eyes = player == mc.player && mc.getCameraEntity() == player && mc.options.getCameraType().isFirstPerson();
            if (!aura.meditating()) continue;
            aura(level, random, player.position(), aura.fill(), colour(aura.auraArt()), player.tickCount, eyes, qiScale(aura.qi()));
            if (!eyes) artEffect(level, random, player.position(), aura.auraArt(), qiScale(aura.qi()), player.tickCount);
        }
        AABB near = mc.player.getBoundingBox().inflate(48);
        for (Entity e : level.getEntities((Entity) null, near, e -> e instanceof MeditatingBodyEntity || e instanceof HeartDemonEntity || e instanceof QiMoteEntity)) {
            if (e instanceof MeditatingBodyEntity body) aura(level, random, body.position(), 1.0F, colour(artOf(body.art())), body.tickCount, false, 1.0F);
            else if (e instanceof HeartDemonEntity demon) demonAura(level, random, demon);
            else if (random.nextInt(3) == 0) spark(level, e.getX() + (random.nextDouble() - 0.5) * 0.4, e.getY() + 0.25, e.getZ() + (random.nextDouble() - 0.5) * 0.4,
                    0, 0.02, 0, new float[]{0.75F, 0.92F, 1.0F}, true);
        }
    }

    /**
     * One tick of an aura round a seated body at {@code feet} (Enzo, 17/10/2026, after the manhwa auras he showed): a
     * volume of flames in the colour of the art mastered best, as big as the Qi held ({@code scale}). Seen through the
     * meditator's own eyes ({@code eyes}), only an ember now and then, further out, so none crosses the view.
     */
    static void aura(ClientLevel level, RandomSource random, Vec3 feet, float fill, float[] c, int age, boolean eyes, float scale) {
        if (!eyes) {
            volume(level, random, feet, fill, c, scale);
            return;
        }
        if (random.nextInt(3) != 0) return;
        double a = random.nextDouble() * Math.PI * 2, r = 1.2 + random.nextDouble() * 0.5;
        AuraParticle p = spark(level, feet.x + Math.cos(a) * r, feet.y + 0.1 + random.nextDouble() * 0.4, feet.z + Math.sin(a) * r,
                (random.nextDouble() - 0.5) * 0.03, 0.09 + random.nextDouble() * 0.08 * (0.5 + fill), (random.nextDouble() - 0.5) * 0.03, c, false);
        if (p != null) p.big(0.035F + random.nextFloat() * 0.035F, 24 + random.nextInt(18));
    }

    /**
     * How big an aura is for the Qi one holds (Enzo, 17/10/2026: "en fonction du nombre de Qi, plus ou moins gros, plus
     * ou moins grand, plus ou moins haut"): 0.6 at 100 Qi, 0.9 at 1 000, 1.2 at 10 000, 1.5 at 90 000; between 0.5 and 1.6.
     */
    public static float qiScale(float qi) {
        return (float) Math.max(0.5, Math.min(1.6, 0.6 + 0.3 * Math.log10(Math.max(1, qi) / 100.0)));
    }

    /**
     * The volume of flames (chosen by Enzo among four looks, 17/10/2026): many soft glowing flames all round the body,
     * rising and swaying, overlapping; thicker as the reserve fills, and as a whole wider, bigger and climbing higher
     * with the Qi one holds ({@code scale}).
     */
    static void volume(ClientLevel level, RandomSource random, Vec3 feet, float fill, float[] c, float scale) {
        int count = Math.round((4 + fill * 6) * scale * scale);
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = (0.15 + random.nextDouble() * 0.5) * scale;
            double x = feet.x + Math.cos(a) * r, z = feet.z + Math.sin(a) * r, y = feet.y + random.nextDouble() * 0.9 * scale;
            double rise = (0.05 + random.nextDouble() * 0.05 * (0.6 + fill)) * scale;
            AuraParticle p = spark(level, x, y, z, Math.cos(a) * 0.012 * scale + (random.nextDouble() - 0.5) * 0.02, rise,
                    Math.sin(a) * 0.012 * scale + (random.nextDouble() - 0.5) * 0.02, c, false);
            if (p != null) p.big((0.16F + random.nextFloat() * (0.12F + 0.12F * fill)) * scale, Math.round((16 + random.nextInt(12) + 8 * fill) * (0.7F + 0.3F * scale)));
        }
    }

    /**
     * Each art's little touch in its aura (Enzo, 17/10/2026), the art one has mastered best: clouds for the Azure Cloud,
     * a ring blown along the ground for the Long Fist, golden streaks shooting up for the spear, green leaves of light
     * swirling for the staff, stones lifting for the Mountain-Shattering Fist, seven stars turning over the head for the
     * Seven Stars, sparks of lightning for the Thunder Saber, black smoke for the Majin Fist, a golden bell-ring of light
     * for the Golden Bell, drops of blood for the Sanguine Art.
     */
    static void artEffect(ClientLevel level, RandomSource random, Vec3 feet, Optional<ResourceLocation> art, float scale, int age) {
        if (art.isEmpty()) return;
        ResourceLocation id = art.get();
        float[] c = colour(art);
        double r = 0.7 * scale;
        if (id.equals(MartialArts.AZURE_CLOUD_SWORD)) {
            if (age % 5 == 0) {
                double a = random.nextDouble() * Math.PI * 2;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD, feet.x + Math.cos(a) * r, feet.y + 0.3 + random.nextDouble() * scale, feet.z + Math.sin(a) * r,
                        -Math.sin(a) * 0.02, 0.01, Math.cos(a) * 0.02);
            }
        } else if (id.equals(MartialArts.LONG_FIST)) {
            if (age % 30 == 0) ring(level, feet.add(0, 0.08, 0), 0.5 * scale, 24, c, 0.12 * scale);
        } else if (id.equals(MartialArts.CELESTIAL_PEAK_SPEAR)) {
            if (age % 6 == 0) {
                AuraParticle p = spark(level, feet.x + (random.nextDouble() - 0.5) * r, feet.y + 0.4, feet.z + (random.nextDouble() - 0.5) * r, 0, 0.45 * scale, 0, c, false);
                if (p != null) p.big(0.05F, 10);
            }
        } else if (id.equals(MartialArts.TRAVELLER_STAFF)) {
            double a = age * 0.15 + random.nextDouble() * 0.3;
            AuraParticle p = spark(level, feet.x + Math.cos(a) * r * 1.2, feet.y + 0.3 + (age % 40) / 40.0 * 1.2 * scale, feet.z + Math.sin(a) * r * 1.2,
                    -Math.sin(a) * 0.04, 0.01, Math.cos(a) * 0.04, new float[]{0.45F, 0.95F, 0.4F}, false);
            if (p != null) p.big(0.07F, 18);
        } else if (id.equals(MartialArts.MOUNTAIN_SHATTERING_FIST)) {
            if (age % 4 == 0) {
                double a = random.nextDouble() * Math.PI * 2;
                level.addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
                                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
                        feet.x + Math.cos(a) * r, feet.y + 0.1, feet.z + Math.sin(a) * r, 0, 0.35 + random.nextDouble() * 0.2, 0);
            }
        } else if (id.equals(MartialArts.SEVEN_STARS_SWORD)) {
            // The Dipper's seven stars, turning slowly over the head.
            double[][] dipper = {{-0.9, 0.1}, {-0.55, 0.15}, {-0.25, 0.05}, {0.05, -0.05}, {0.15, -0.35}, {0.55, -0.35}, {0.6, -0.05}};
            double turn = age * 0.03, h = feet.y + 1.4 + 0.6 * scale;
            for (double[] st : dipper) {
                double x = st[0] * Math.cos(turn) - st[1] * Math.sin(turn), z = st[0] * Math.sin(turn) + st[1] * Math.cos(turn);
                AuraParticle p = spark(level, feet.x + x * scale, h, feet.z + z * scale, 0, 0, 0, new float[]{0.9F, 0.94F, 1.0F}, false);
                if (p != null) p.big(0.06F + (random.nextInt(8) == 0 ? 0.04F : 0), 2);
            }
        } else if (id.equals(MartialArts.THUNDER_SABER)) {
            if (random.nextInt(3) == 0) level.addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    feet.x + (random.nextDouble() - 0.5) * r * 2, feet.y + random.nextDouble() * 1.6 * scale, feet.z + (random.nextDouble() - 0.5) * r * 2,
                    (random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.3);
        } else if (id.equals(MartialArts.MAJIN_FIST)) {
            if (age % 2 == 0) level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    feet.x + (random.nextDouble() - 0.5) * r, feet.y + 0.2 + random.nextDouble() * scale, feet.z + (random.nextDouble() - 0.5) * r, 0, 0.04, 0);
        } else if (id.equals(MartialArts.GOLDEN_BELL)) {
            if (age % 20 == 0) {
                ring(level, feet.add(0, 0.9 * scale, 0), 0.55 * scale, 28, c, 0.0);
                ring(level, feet.add(0, 0.2, 0), 0.75 * scale, 32, c, 0.0);
            }
        } else if (id.equals(MartialArts.SANGUINE_ART)) {
            if (age % 3 == 0) level.addParticle(com.murimblock.bloodsect.BloodSect.BLOOD_DROPLET.get(),
                    feet.x + (random.nextDouble() - 0.5) * r * 1.6, feet.y + 0.6 + random.nextDouble() * 1.2 * scale, feet.z + (random.nextDouble() - 0.5) * r * 1.6, 0, -0.02, 0);
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
