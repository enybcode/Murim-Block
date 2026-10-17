package com.murimblock.client;

import com.lowdragmc.photon.client.light.LightSink;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.network.SevenStarsPayloads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static com.murimblock.client.SevenStarsFx.BEAM;
import static com.murimblock.client.SevenStarsFx.BURST_LIGHT;
import static com.murimblock.client.SevenStarsFx.DOT;
import static com.murimblock.client.SevenStarsFx.RING;
import static com.murimblock.client.SevenStarsFx.SEAL;
import static com.murimblock.client.SevenStarsFx.SIGIL;
import static com.murimblock.client.SevenStarsFx.STAR;
import static com.murimblock.client.SevenStarsFx.STAR_LIGHT;
import static com.murimblock.client.SevenStarsFx.SWORD;

/**
 * The Seven Stars Sword's techniques on the client, after Shooting Star (SevenStarsFx draws the stars, the bursts and the
 * streak): the same thin, clean shapes, the same light (Enzo, 17/10/2026: "en gardant le niveau graphique du passif"),
 * every one lit by Photon ({@link #lights}).
 * <ul>
 *   <li><b>Dipper Steps</b>: at each bound a star's sigil lights on the ground under the player, joined to the last by a
 *   line of light along the ground; their figure stays a moment where they bounded from; a cross of light at each thrust;
 *   the three sigils fade together;</li>
 *   <li><b>Orbiting Stars</b>: seven stars round the player at the chest, turning fast, each with a short tail; one that
 *   stops a blow flares where it met it and darts at the attacker; those left at the end fly off as small comets;</li>
 *   <li><b>Constellation Link</b>: lines of light from the raised blade to each marked enemy, then from one to the next,
 *   brightening until they all burst;</li>
 *   <li><b>Falling Star Rain</b>: the Big Dipper's seal, turning, on the ground where it falls; each star comes down from
 *   the sky with a long tapering tail, a flash and a ring where it lands;</li>
 *   <li><b>Polaris Riposte</b>: the pole star over the player, bigger than the others, its long rays;</li>
 *   <li><b>Seven Stars Sword Array</b>: the great seal under the target; seven swords of light rise from it in the Dipper,
 *   turn to the target, strike one by one, then plunge in together, a great flash;</li>
 *   <li><b>Extinguished Stars</b>: a star lit over the body at each thrust, then the seven fading out together.</li>
 * </ul>
 */
final class SevenStarsTechniqueFx {
    private SevenStarsTechniqueFx() {
    }

    private record Steps(int playerId, int targetId, List<Vec3> spots, int gap, float start) { }
    private record Orbit(int playerId, float start, float until) { }
    private record Flare(Vec3 at, int entityId, float start, float life, float size, boolean cross) { }
    private record Bolt(Vec3 from, int toEntity, Vec3 to, float start, float life, boolean comet) { }
    private record Link(int playerId, List<Integer> ids, float start, float at) { }
    private record Circle(Vec3 centre, float radius, float start, float length) { }
    private record Fall(Vec3 from, Vec3 to, float start, float fall) { }
    private record Array(int targetId, List<Vec3> swords, int rise, int first, int gap, int fin, float start) { }
    private record Polaris(int playerId, float start) { }
    private record Fade(int entityId, Vec3 top, float start) { }

    private static final List<Steps> STEPS = new ArrayList<>();
    /** Each player's orbiting stars: how many are left. */
    private static final Map<Integer, Integer> ORBIT_COUNT = new HashMap<>();
    private static final Map<Integer, Orbit> ORBITS = new HashMap<>();
    private static final List<Flare> FLARES = new ArrayList<>();
    private static final List<Bolt> BOLTS = new ArrayList<>();
    private static final List<Link> LINKS = new ArrayList<>();
    private static final List<Circle> CIRCLES = new ArrayList<>();
    private static final List<Fall> FALLS = new ArrayList<>();
    private static final List<Array> ARRAYS = new ArrayList<>();
    private static final List<Polaris> POLES = new ArrayList<>();
    private static final List<Fade> FADES = new ArrayList<>();

    /** The orbit: its radius round the chest, its height, its turn (radians a tick). */
    private static final float ORBIT_RADIUS = 1.1F, ORBIT_HEIGHT = 1.15F, ORBIT_SPIN = 0.16F;
    /** A falling star's tail (blocks), how long a rain's landing shows, the array's swords' length and seal. */
    private static final float TAIL = 5, SWORD_LENGTH = 2.4F, SWORD_WIDTH = 0.85F, ARRAY_SEAL = 4.6F;
    private static final int LAND_TICKS = 14, ARRAY_AFTER = 18, FADE_TICKS = 14;

    static boolean idle() {
        return STEPS.isEmpty() && ORBITS.isEmpty() && FLARES.isEmpty() && BOLTS.isEmpty() && LINKS.isEmpty() && CIRCLES.isEmpty()
                && FALLS.isEmpty() && ARRAYS.isEmpty() && POLES.isEmpty() && FADES.isEmpty();
    }

    static void clear() {
        STEPS.clear(); ORBIT_COUNT.clear(); ORBITS.clear(); FLARES.clear(); BOLTS.clear(); LINKS.clear(); CIRCLES.clear();
        FALLS.clear(); ARRAYS.clear(); POLES.clear(); FADES.clear();
    }

    private static Vec3 v(Vector3f f) {
        return new Vec3(f.x(), f.y(), f.z());
    }

    // ---------------------------------------------------------------- from the server

    static void receive(SevenStarsPayloads.Fx fx) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        float now = level.getGameTime();
        List<Vec3> points = fx.points().stream().map(SevenStarsTechniqueFx::v).toList();
        RandomSource random = level.random;
        switch (fx.kind()) {
            case SevenStarsPayloads.Fx.STEPS -> {
                STEPS.add(new Steps(fx.playerId(), fx.target(), points, fx.value(), now));
                // The figure stays a moment where it left each place, the first being where it stood.
                Entity p = level.getEntity(fx.playerId());
                if (p != null) SevenStarsFx.GHOSTS.add(new SevenStarsFx.Ghost(fx.playerId(), p.position(), now, 8));
                for (int k = 0; k + 1 < points.size(); k++)
                    SevenStarsFx.GHOSTS.add(new SevenStarsFx.Ghost(fx.playerId(), points.get(k), now + (k + 1) * fx.value(), 8));
                for (int k = 0; k < points.size(); k++)
                    FLARES.add(new Flare(null, fx.target(), now + k * fx.value() + 2, 6, 1.4F, true));
            }
            case SevenStarsPayloads.Fx.ORBIT -> {
                int ticks = fx.ids().isEmpty() ? 120 : fx.ids().get(0);
                ORBITS.put(fx.playerId(), new Orbit(fx.playerId(), now, now + ticks));
                ORBIT_COUNT.put(fx.playerId(), fx.value());
            }
            case SevenStarsPayloads.Fx.ORBIT_BLOCK -> {
                ORBIT_COUNT.put(fx.playerId(), fx.value());
                Vec3 from = points.isEmpty() ? null : points.get(0);
                Entity p = level.getEntity(fx.playerId());
                if (p != null && from != null) {
                    Vec3 chest = p.position().add(0, ORBIT_HEIGHT, 0);
                    Vec3 meet = chest.add(from.add(0, 1, 0).subtract(chest).multiply(1, 0.4, 1).normalize().scale(ORBIT_RADIUS));
                    FLARES.add(new Flare(meet, -1, now, 7, 1.3F, false));
                    if (fx.target() >= 0) BOLTS.add(new Bolt(meet, fx.target(), null, now, 6, false));
                    for (int i = 0; i < 12; i++)
                        SevenStarsFx.spark(level, meet.x, meet.y, meet.z, (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.3) * 0.15, (random.nextDouble() - 0.5) * 0.2, 0.07F, 10 + random.nextInt(8));
                }
                if (fx.target() >= 0) FLARES.add(new Flare(null, fx.target(), now + 3, 6, 1.4F, true));
                if (fx.value() <= 0) ORBITS.remove(fx.playerId());
            }
            case SevenStarsPayloads.Fx.ORBIT_RELEASE -> {
                Entity p = level.getEntity(fx.playerId());
                Orbit o = ORBITS.remove(fx.playerId());
                int left = ORBIT_COUNT.getOrDefault(fx.playerId(), 0);
                ORBIT_COUNT.remove(fx.playerId());
                if (p != null) for (int i = 0; i < fx.ids().size(); i++) {
                    Vec3 from = orbitPlace(p, o == null ? now : o.start, i, Math.max(left, fx.ids().size()), now);
                    BOLTS.add(new Bolt(from, fx.ids().get(i), null, now + i, 9, true));
                    FLARES.add(new Flare(null, fx.ids().get(i), now + i + 9, 5, 0.9F, true));
                }
            }
            case SevenStarsPayloads.Fx.LINK -> LINKS.add(new Link(fx.playerId(), List.copyOf(fx.ids()), now, now + fx.value()));
            case SevenStarsPayloads.Fx.RAIN_CIRCLE -> {
                if (!points.isEmpty()) CIRCLES.add(new Circle(points.get(0), fx.value() / 10.0F, now, fx.ids().isEmpty() ? 60 : fx.ids().get(0)));
            }
            case SevenStarsPayloads.Fx.RAIN_STAR -> {
                if (points.size() >= 2) FALLS.add(new Fall(points.get(0), points.get(1), now, Math.max(1, fx.value())));
            }
            case SevenStarsPayloads.Fx.ARRAY -> {
                List<Integer> t = fx.ids();
                if (t.size() >= 4) ARRAYS.add(new Array(fx.target(), points, t.get(0), t.get(1), t.get(2), t.get(3), now));
            }
            case SevenStarsPayloads.Fx.POLARIS -> POLES.add(new Polaris(fx.playerId(), now));
            case SevenStarsPayloads.Fx.FINISH_STAR -> SevenStarsFx.show(fx.target(), fx.value());
            case SevenStarsPayloads.Fx.FINISH_OUT -> {
                Entity e = level.getEntity(fx.target());
                SevenStarsFx.show(fx.target(), 0);
                if (e != null) {
                    Vec3 top = e.position().add(0, Math.max(0.6, e.getBbHeight()) + 0.7, 0);
                    FADES.add(new Fade(fx.target(), top, now));
                    for (int i = 0; i < 30; i++)
                        SevenStarsFx.spark(level, top.x + (random.nextDouble() - 0.5) * 1.6, top.y + (random.nextDouble() - 0.5) * 0.4, top.z + (random.nextDouble() - 0.5) * 1.6,
                                0, -0.02 - random.nextDouble() * 0.02, 0, 0.06F, 30 + random.nextInt(20));
                }
            }
            default -> { }
        }
    }

    /** Every client tick: what is over is forgotten; the dust of the landings, the sigils and the swords. */
    static void tick(ClientLevel level, long now) {
        RandomSource random = level.random;
        STEPS.removeIf(s -> now - s.start > s.spots.size() * s.gap + 24);
        ORBITS.values().removeIf(o -> now > o.until + 2);
        FLARES.removeIf(f -> now - f.start > f.life);
        BOLTS.removeIf(b -> now - b.start > b.life);
        LINKS.removeIf(l -> now > l.at + 6);
        CIRCLES.removeIf(c -> now - c.start > c.length + 10);
        FADES.removeIf(f -> now - f.start > FADE_TICKS);
        POLES.removeIf(p -> now - p.start > 24);
        for (var it = FALLS.iterator(); it.hasNext(); ) {
            Fall f = it.next();
            float t = now - f.start;
            if (Math.abs(t - f.fall) < 0.5F) {
                for (int i = 0; i < 18; i++) {
                    double a = random.nextDouble() * Math.PI * 2, sp = 0.08 + random.nextDouble() * 0.18;
                    SevenStarsFx.spark(level, f.to.x, f.to.y + 0.2, f.to.z, Math.cos(a) * sp, 0.05 + random.nextDouble() * 0.15, Math.sin(a) * sp, 0.08F, 14 + random.nextInt(10));
                }
            }
            if (t > f.fall + LAND_TICKS) it.remove();
        }
        for (var it = ARRAYS.iterator(); it.hasNext(); ) {
            Array a = it.next();
            float t = now - a.start;
            Entity target = level.getEntity(a.targetId);
            if (Math.abs(t - a.fin) < 0.5F && target != null) {
                Vec3 c = target.getBoundingBox().getCenter();
                for (int i = 0; i < 60; i++) {
                    double ang = random.nextDouble() * Math.PI * 2, up = random.nextDouble() * 0.35, sp = 0.15 + random.nextDouble() * 0.35;
                    SevenStarsFx.spark(level, c.x, c.y, c.z, Math.cos(ang) * sp, up, Math.sin(ang) * sp, 0.1F, 20 + random.nextInt(16));
                }
            }
            if (t > a.fin + ARRAY_AFTER) it.remove();
        }
        // A little star dust from each lit sigil of Dipper Steps.
        for (Steps s : STEPS) for (int k = 0; k < s.spots.size(); k++) {
            float t = now - s.start - k * s.gap;
            if (t >= 0 && t < 4) {
                Vec3 p = s.spots.get(k);
                SevenStarsFx.spark(level, p.x + (random.nextDouble() - 0.5), p.y + 0.1, p.z + (random.nextDouble() - 0.5), 0, 0.04, 0, 0.06F, 14);
            }
        }
    }

    // ---------------------------------------------------------------- places

    /** Where orbiting star `i` of `n` is now round a player. */
    private static Vec3 orbitPlace(Entity p, float since, int i, int n, float now) {
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        double a = (now - since) * ORBIT_SPIN + i * Math.PI * 2 / Math.max(1, n);
        Vec3 c = p.getPosition(partial).add(0, ORBIT_HEIGHT + 0.1 * Math.sin(now * 0.2 + i), 0);
        return c.add(Math.cos(a) * ORBIT_RADIUS, 0, Math.sin(a) * ORBIT_RADIUS);
    }

    private static Vec3 heart(Entity e) {
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        return e.getPosition(partial).add(0, e.getBbHeight() * 0.55, 0);
    }

    private static Vec3 crown(Entity e) {
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        return e.getPosition(partial).add(0, e.getBbHeight() + 0.55, 0);
    }

    /** Where an array's sword `i` stands now (the point toward the target), and where its point is. */
    private static Vec3[] swordAt(Array a, int i, float t, Vec3 target) {
        Vec3 home = a.swords.get(i);
        float rise = Mth.clamp((t - i) / a.rise, 0, 1);
        rise = 1 - (1 - rise) * (1 - rise);
        Vec3 base = home.add(0, -1.6 * (1 - rise), 0);
        Vec3 dir = target.subtract(base).normalize();
        float strike = a.first + i * a.gap;
        // Its own blow: lunges at the target and back; the last, all together, in to the hilt.
        double lunge = 0;
        if (t >= strike - 1 && t < strike + 4) lunge = Math.sin(Mth.clamp((t - strike + 1) / 5, 0, 1) * Math.PI) * 0.9;
        if (t >= a.fin - 3) lunge = Math.min(target.distanceTo(base) - 0.4, (t - a.fin + 3) / 3.0 * target.distanceTo(base));
        Vec3 grip = base.add(dir.scale(lunge - SWORD_LENGTH * 0.5));
        // Before it has turned to the target, it stands point up.
        float turn = Mth.clamp((t - i - a.rise * 0.6F) / 5, 0, 1);
        Vec3 axis = new Vec3(0, 1, 0).lerp(dir, turn).normalize();
        return new Vec3[] {grip, grip.add(axis.scale(SWORD_LENGTH))};
    }

    // ---------------------------------------------------------------- drawing

    static void draw(Map<ResourceLocation, List<Consumer<VertexConsumer>>> draws, Matrix4f m, float now, Camera camera, Vec3 eye, ClientLevel level) {
        Minecraft mc = Minecraft.getInstance();
        // Dipper Steps: the sigils lit one by one, joined along the ground, fading together.
        for (Steps s : STEPS) {
            float end = s.spots.size() * s.gap;
            float fade = 1 - Mth.clamp((now - s.start - end - 6) / 18, 0, 1);
            for (int k = 0; k < s.spots.size(); k++) {
                float t = now - s.start - k * s.gap;
                if (t < 0) continue;
                float lit = Mth.clamp(t / 3, 0, 1) * fade;
                Vec3 p = s.spots.get(k).add(0, 0.04, 0).subtract(eye);
                float spin = now * 0.05F + k;
                draws.get(SIGIL).add(out -> flatTurned(out, m, p, 0.8F + 0.2F * (1 - Mth.clamp(t / 4, 0, 1)), spin, lit));
                if (k > 0) {
                    Vec3 q = s.spots.get(k - 1).add(0, 0.05, 0).subtract(eye);
                    float grow = Mth.clamp(t / 2, 0, 1);
                    Vec3 to = q.lerp(p, grow);
                    draws.get(BEAM).add(out -> flatBeam(out, m, q, to, 0.12F, 0.8F * fade));
                }
            }
        }
        // Orbiting Stars: seven stars round the chest, each with a short tail.
        for (Orbit o : ORBITS.values()) {
            Entity p = level.getEntity(o.playerId);
            // Seen in first person too: the stars circle round the camera, at the chest.
            if (p == null) continue;
            int n = ORBIT_COUNT.getOrDefault(o.playerId, 0);
            float appear = Mth.clamp((now - o.start) / 6, 0, 1), end = Mth.clamp((o.until - now) / 10, 0, 1);
            for (int i = 0; i < 7; i++) {
                boolean lit = i < n;
                Vec3 at = orbitPlace(p, o.start, i, 7, now).subtract(eye);
                if (!lit) continue;
                float b = appear * Math.max(0.4F, end);
                float spin = now * 0.2F + i;
                draws.get(STAR).add(out -> SevenStarsFx.sprite(out, m, at, camera, 0.5F, b, spin));
                for (int k = 1; k <= 3; k++) {
                    Vec3 tail = orbitPlace(p, o.start, i, 7, now - k * 0.9F).subtract(eye);
                    float tb = b * (0.5F - k * 0.13F), ts = 0.22F - k * 0.04F;
                    draws.get(DOT).add(out -> SevenStarsFx.sprite(out, m, tail, camera, ts, tb, 0));
                }
            }
            Vec3 ring = p.getPosition(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)).add(0, ORBIT_HEIGHT, 0).subtract(eye);
            draws.get(RING).add(out -> SevenStarsFx.flat(out, m, ring, ORBIT_RADIUS * 1.18F, 0.18F * appear));
        }
        // Flares: a star's flash (a cross for a cut) at a place or on a body.
        for (Flare f : FLARES) {
            float t = now - f.start;
            if (t < 0) continue;
            Vec3 at = f.at;
            if (at == null) {
                Entity e = level.getEntity(f.entityId);
                if (e == null) continue;
                at = heart(e);
            }
            float k = t / f.life;
            Vec3 c = at.subtract(eye);
            draws.get(STAR).add(out -> SevenStarsFx.sprite(out, m, c, camera, f.size * (1 + 0.4F * k), 1 - k, f.cross ? (float) Math.PI / 4 : k));
        }
        // Bolts: a star darting from a place to a body (a stopped blow struck back, a released star).
        for (Bolt b : BOLTS) {
            float t = now - b.start;
            if (t < 0) continue;
            Vec3 to = b.to;
            if (to == null) {
                Entity e = level.getEntity(b.toEntity);
                if (e == null) continue;
                to = b.comet ? crown(e) : heart(e);
            }
            float k = Mth.clamp(t / b.life, 0, 1), ease = 1 - (1 - k) * (1 - k);
            Vec3 head = b.from.lerp(to, ease), tail = b.from.lerp(to, Math.max(0, ease - 0.35F));
            Vec3 h = head.subtract(eye), tl = tail.subtract(eye);
            float fade = 1 - k * 0.5F;
            draws.get(BEAM).add(out -> SevenStarsFx.beam(out, m, tl, h, eye, 0.1F, 0.9F * fade));
            draws.get(BEAM).add(out -> SevenStarsFx.beam(out, m, tl, h, eye, 0.45F, 0.3F * fade));
            draws.get(STAR).add(out -> SevenStarsFx.sprite(out, m, h, camera, 0.55F, fade, now * 0.3F));
        }
        // Constellation Link: from the raised blade to each marked enemy, then from one to the next.
        for (Link l : LINKS) {
            Entity p = level.getEntity(l.playerId);
            if (p == null) continue;
            float t = now - l.start, span = l.at - l.start;
            float build = Mth.clamp(t / (span * 0.5F), 0, 1), glow = 0.6F + 0.4F * Mth.clamp(t / span, 0, 1);
            float fade = now > l.at ? 1 - Mth.clamp((now - l.at) / 6, 0, 1) : 1;
            Vec3 blade = p.getPosition(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)).add(0, p.getBbHeight() + 1.1, 0);
            Vec3 bladeEye = blade.subtract(eye);
            draws.get(STAR).add(out -> SevenStarsFx.sprite(out, m, bladeEye, camera, 0.9F, fade, now * 0.2F));
            Vec3 prev = null;
            for (int i = 0; i < l.ids.size(); i++) {
                Entity e = level.getEntity(l.ids.get(i));
                if (e == null) continue;
                Vec3 c = crown(e);
                float k = Mth.clamp(build * l.ids.size() - i, 0, 1);
                Vec3 from = blade.subtract(eye), to = blade.lerp(c, k).subtract(eye);
                float b = glow * fade;
                draws.get(BEAM).add(out -> SevenStarsFx.beam(out, m, from, to, eye, 0.07F, b));
                draws.get(BEAM).add(out -> SevenStarsFx.beam(out, m, from, to, eye, 0.4F, 0.25F * b));
                if (prev != null) {
                    Vec3 a2 = prev.subtract(eye), b2 = prev.lerp(c, k).subtract(eye);
                    draws.get(BEAM).add(out -> SevenStarsFx.beam(out, m, a2, b2, eye, 0.05F, 0.8F * b));
                }
                prev = c;
            }
        }
        // Falling Star Rain: the seal; the stars coming down; their landings.
        for (Circle c : CIRCLES) {
            float t = now - c.start;
            float b = Mth.clamp(t / 6, 0, 1) * (1 - Mth.clamp((t - c.length) / 10, 0, 1));
            Vec3 at = c.centre.add(0, 0.05, 0).subtract(eye);
            float spin = t * 0.03F;
            draws.get(SEAL).add(out -> flatTurned(out, m, at, c.radius * 1.1F, spin, b * 0.9F));
        }
        for (Fall f : FALLS) {
            float t = now - f.start;
            Vec3 dir = f.to.subtract(f.from).normalize();
            if (t <= f.fall) {
                float k = t / f.fall;
                Vec3 head = f.from.lerp(f.to, k * k);
                Vec3 tail = head.subtract(dir.scale(TAIL * (0.4F + 0.6F * k)));
                Vec3 h = head.subtract(eye), tl = tail.subtract(eye);
                draws.get(BEAM).add(out -> SevenStarsFx.beam(out, m, tl, h, eye, 0.12F, 1));
                draws.get(BEAM).add(out -> SevenStarsFx.beam(out, m, tl, h, eye, 0.6F, 0.35F));
                draws.get(STAR).add(out -> SevenStarsFx.sprite(out, m, h, camera, 0.95F, 1, now * 0.25F));
                draws.get(DOT).add(out -> SevenStarsFx.sprite(out, m, h, camera, 1.3F, 0.5F, 0));
            } else {
                float k = (t - f.fall) / LAND_TICKS;
                Vec3 ground = f.to.add(0, 0.07, 0).subtract(eye), flash = f.to.add(0, 0.5, 0).subtract(eye);
                draws.get(RING).add(out -> SevenStarsFx.flat(out, m, ground, 0.3F + 1.4F * (1 - (1 - k) * (1 - k)), 1 - k));
                if (k < 0.5F) draws.get(DOT).add(out -> SevenStarsFx.sprite(out, m, flash, camera, 1.8F * (1 - 2 * k), 1 - 2 * k, 0));
                draws.get(SIGIL).add(out -> flatTurned(out, m, ground, 0.6F, 0, 0.7F * (1 - k)));
            }
        }
        // Polaris Riposte: the pole star over the player, its long rays.
        for (Polaris pol : POLES) {
            Entity p = level.getEntity(pol.playerId);
            if (p == null) continue;
            float t = now - pol.start, k = t / 24;
            Vec3 at = crown(p).add(0, 0.6, 0).subtract(eye);
            float b = 1 - k * k;
            draws.get(STAR).add(out -> SevenStarsFx.sprite(out, m, at, camera, 1.6F + 0.4F * k, b, 0));
            draws.get(STAR).add(out -> SevenStarsFx.sprite(out, m, at, camera, 3.2F * (1 - k * 0.5F), 0.45F * b, (float) Math.PI / 4));
            draws.get(DOT).add(out -> SevenStarsFx.sprite(out, m, at, camera, 2.0F, 0.4F * b, 0));
        }
        // Seven Stars Sword Array.
        for (Array a : ARRAYS) {
            Entity target = level.getEntity(a.targetId);
            if (target == null) continue;
            float t = now - a.start;
            Vec3 heart = heart(target);
            float end = 1 - Mth.clamp((t - a.fin) / ARRAY_AFTER, 0, 1);
            float sealB = Mth.clamp(t / 8, 0, 1) * end;
            Vec3 ground = target.position().add(0, 0.05, 0).subtract(eye);
            float spin = -t * 0.02F;
            draws.get(SEAL).add(out -> flatTurned(out, m, ground, ARRAY_SEAL, spin, sealB));
            draws.get(RING).add(out -> SevenStarsFx.flat(out, m, ground, ARRAY_SEAL * 1.08F, 0.4F * sealB));
            for (int i = 0; i < a.swords.size(); i++) {
                if (t < i) continue;
                Vec3[] s = swordAt(a, i, t, heart);
                float b = Mth.clamp((t - i) / 4, 0, 1) * (t > a.fin ? end : 1);
                Vec3 grip = s[0].subtract(eye), tip = s[1].subtract(eye);
                draws.get(SWORD).add(out -> blade(out, m, grip, tip, SWORD_WIDTH, b));
                draws.get(DOT).add(out -> SevenStarsFx.sprite(out, m, grip.lerp(tip, 0.55), camera, 1.1F, 0.25F * b, 0));
                float strike = a.first + i * a.gap;
                if (t >= strike && t < strike + 5) {
                    float k = (t - strike) / 5;
                    Vec3 h = heart.subtract(eye);
                    draws.get(STAR).add(out -> SevenStarsFx.sprite(out, m, h, camera, 1.2F + 0.6F * k, 1 - k, (float) Math.PI / 4));
                }
            }
            if (t >= a.fin && t < a.fin + 10) {
                float k = (t - a.fin) / 10;
                Vec3 h = heart.subtract(eye);
                draws.get(DOT).add(out -> SevenStarsFx.sprite(out, m, h, camera, 4.5F * (1 - k * 0.5F), 1 - k, 0));
                draws.get(STAR).add(out -> SevenStarsFx.sprite(out, m, h, camera, 3.0F + 2 * k, 1 - k, k));
                draws.get(RING).add(out -> SevenStarsFx.flat(out, m, ground, 0.5F + 5.5F * (1 - (1 - k) * (1 - k)), 1 - k));
            }
        }
        // Extinguished Stars: the seven fading out together over the body.
        for (Fade f : FADES) {
            float t = now - f.start, k = t / FADE_TICKS;
            for (int i = 0; i < 7; i++) {
                Vec3 at = SevenStarsFx.dipperPlace(f.top.add(0, 0.25, 0), i, camera).subtract(eye);
                draws.get(STAR).add(out -> SevenStarsFx.sprite(out, m, at, camera, 0.48F * (1 - 0.6F * k), 1 - k, k));
            }
        }
    }

    // ---------------------------------------------------------------- light

    static void lights(LightSink sink, ClientLevel level, float now, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        for (Steps s : STEPS) for (int k = 0; k < s.spots.size(); k++) {
            float t = now - s.start - k * s.gap;
            float end = s.spots.size() * s.gap;
            float fade = 1 - Mth.clamp((now - s.start - end - 6) / 18, 0, 1);
            if (t >= 0) SevenStarsFx.light(sink, s.spots.get(k).add(0, 0.4, 0), STAR_LIGHT, 0.9F * fade * Mth.clamp(t / 3, 0, 1) + (t < 3 ? 1.2F : 0), 4, 0);
        }
        for (Orbit o : ORBITS.values()) {
            Entity p = level.getEntity(o.playerId);
            if (p == null) continue;
            int n = ORBIT_COUNT.getOrDefault(o.playerId, 0);
            for (int i = 0; i < n; i++) SevenStarsFx.light(sink, orbitPlace(p, o.start, i, 7, now), STAR_LIGHT, 0.55F, 3.5F, 0);
        }
        for (Flare f : FLARES) {
            float t = now - f.start;
            if (t < 0) continue;
            Vec3 at = f.at;
            if (at == null) { Entity e = level.getEntity(f.entityId); if (e == null) continue; at = heart(e); }
            SevenStarsFx.light(sink, at, BURST_LIGHT, 2.2F * (1 - t / f.life), 4.5F, 0);
        }
        for (Bolt b : BOLTS) {
            float t = now - b.start;
            if (t < 0) continue;
            Vec3 to = b.to;
            if (to == null) { Entity e = level.getEntity(b.toEntity); if (e == null) continue; to = heart(e); }
            float k = Mth.clamp(t / b.life, 0, 1);
            SevenStarsFx.light(sink, b.from.lerp(to, 1 - (1 - k) * (1 - k)), STAR_LIGHT, 1.2F, 4, 0);
        }
        for (Link l : LINKS) {
            Entity p = level.getEntity(l.playerId);
            if (p == null) continue;
            Vec3 blade = p.position().add(0, p.getBbHeight() + 1.1, 0);
            float glow = Mth.clamp((now - l.start) / (l.at - l.start), 0, 1);
            SevenStarsFx.light(sink, blade, STAR_LIGHT, 1.0F + glow, 5, 0.2F);
            for (int id : l.ids) {
                Entity e = level.getEntity(id);
                if (e != null) SevenStarsFx.light(sink, blade.lerp(crown(e), 0.5), STAR_LIGHT, 0.6F + 0.8F * glow, 4, 0.15F);
            }
        }
        for (Circle c : CIRCLES) {
            float t = now - c.start, b = Mth.clamp(t / 6, 0, 1) * (1 - Mth.clamp((t - c.length) / 10, 0, 1));
            SevenStarsFx.light(sink, c.centre.add(0, 0.6, 0), STAR_LIGHT, 1.2F * b, c.radius + 3, 0.2F * b);
        }
        for (Fall f : FALLS) {
            float t = now - f.start;
            if (t <= f.fall) { float k = t / f.fall; SevenStarsFx.light(sink, f.from.lerp(f.to, k * k), STAR_LIGHT, 2.4F, 7, 0.4F); }
            else { float k = (t - f.fall) / LAND_TICKS; SevenStarsFx.light(sink, f.to.add(0, 0.6, 0), BURST_LIGHT, 3.2F * (1 - k) * (1 - k), 7, 0.3F * (1 - k)); }
        }
        for (Polaris pol : POLES) {
            Entity p = level.getEntity(pol.playerId);
            if (p == null) continue;
            float k = (now - pol.start) / 24;
            SevenStarsFx.light(sink, crown(p).add(0, 0.6, 0), BURST_LIGHT, 2.6F * (1 - k * k), 7, 0.4F * (1 - k));
        }
        for (Array a : ARRAYS) {
            Entity target = level.getEntity(a.targetId);
            if (target == null) continue;
            float t = now - a.start;
            Vec3 heart = heart(target);
            float end = 1 - Mth.clamp((t - a.fin) / ARRAY_AFTER, 0, 1);
            SevenStarsFx.light(sink, target.position().add(0, 0.5, 0), STAR_LIGHT, 1.4F * Mth.clamp(t / 8, 0, 1) * end, ARRAY_SEAL + 3, 0.3F * end);
            for (int i = 0; i < a.swords.size(); i++) {
                if (t < i) continue;
                Vec3[] s = swordAt(a, i, t, heart);
                SevenStarsFx.light(sink, s[0].lerp(s[1], 0.5), STAR_LIGHT, 0.8F * end, 3.5F, 0);
            }
            if (t >= a.fin && t < a.fin + 14) {
                float k = (t - a.fin) / 14;
                SevenStarsFx.light(sink, heart, BURST_LIGHT, 6.0F * (1 - k) * (1 - k), 14, 0.8F * (1 - k));
            }
        }
        for (Fade f : FADES) SevenStarsFx.light(sink, f.top, STAR_LIGHT, 1.2F * (1 - (now - f.start) / FADE_TICKS), 5, 0);
    }

    // ---------------------------------------------------------------- shapes

    /** A flat square on the ground turned `spin` radians, `radius` out from its middle. */
    private static void flatTurned(VertexConsumer out, Matrix4f m, Vec3 at, float radius, float spin, float b) {
        int lum = (int) (255 * Mth.clamp(b, 0, 1));
        float c = Mth.cos(spin) * radius, s = Mth.sin(spin) * radius;
        SevenStarsFx.vertex(out, m, at.x - c + s, at.y, at.z - s - c, 0, 0, lum);
        SevenStarsFx.vertex(out, m, at.x - c - s, at.y, at.z - s + c, 0, 1, lum);
        SevenStarsFx.vertex(out, m, at.x + c - s, at.y, at.z + s + c, 1, 1, lum);
        SevenStarsFx.vertex(out, m, at.x + c + s, at.y, at.z + s - c, 1, 0, lum);
    }

    /** A line of light lying on the ground from a to b. */
    private static void flatBeam(VertexConsumer out, Matrix4f m, Vec3 a, Vec3 b, float width, float bright) {
        Vec3 along = b.subtract(a);
        Vec3 side = new Vec3(-along.z, 0, along.x).normalize().scale(width / 2);
        if (!Double.isFinite(side.x) || along.lengthSqr() < 1e-6) return;
        int lum = (int) (255 * Mth.clamp(bright, 0, 1));
        SevenStarsFx.vertex(out, m, a.x + side.x, a.y, a.z + side.z, 0, 0, lum);
        SevenStarsFx.vertex(out, m, a.x - side.x, a.y, a.z - side.z, 0, 1, lum);
        SevenStarsFx.vertex(out, m, b.x - side.x, b.y, b.z - side.z, 1, 1, lum);
        SevenStarsFx.vertex(out, m, b.x + side.x, b.y, b.z + side.z, 1, 0, lum);
    }

    /** A sword of light from its grip to its point: two crossed planes along the blade (its texture, point up). */
    private static void blade(VertexConsumer out, Matrix4f m, Vec3 grip, Vec3 tip, float width, float bright) {
        Vec3 axis = tip.subtract(grip).normalize();
        Vec3 ref = Math.abs(axis.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 s1 = axis.cross(ref).normalize().scale(width / 2), s2 = axis.cross(s1).normalize().scale(width / 2);
        int lum = (int) (255 * Mth.clamp(bright, 0, 1));
        for (Vec3 s : new Vec3[] {s1, s2}) {
            SevenStarsFx.vertex(out, m, tip.x - s.x, tip.y - s.y, tip.z - s.z, 0, 0, lum);
            SevenStarsFx.vertex(out, m, grip.x - s.x, grip.y - s.y, grip.z - s.z, 0, 1, lum);
            SevenStarsFx.vertex(out, m, grip.x + s.x, grip.y + s.y, grip.z + s.z, 1, 1, lum);
            SevenStarsFx.vertex(out, m, tip.x + s.x, tip.y + s.y, tip.z + s.z, 1, 0, lum);
        }
    }
}
