package com.murimblock.martial;

import com.murimblock.Murimblock;
import com.murimblock.mob.MurimEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Scarlet Bloom (猩红绽放), the Sanguine Art's finish (docs/combat/styles/sanguine-art.md), after the reference film Enzo
 * gave on 15/10/2026: B on a knocked-down player, for an executor whose main art is the Sanguine Art and who has unlocked
 * every one of its techniques. Seen as a moving film by its two players (client/FinishCinematic), and by everyone near:
 * <ol>
 *   <li>0.0 s: the executor drives his hand into the ground;</li>
 *   <li>0.7 s: blood crystals erupt in a ring around the downed body, one great spike under it, stones are torn from
 *   the ground, and the body is thrown 9 blocks up, arched back and flailing (its own animation, scarlet_bloom_flung),
 *   then hangs spread in the sky and curls up as the cage closes;</li>
 *   <li>2.0 s: while it hangs in the sky, ten crystals leave the executor's back, take their places around it and weave
 *   a cage of crystal beams between them;</li>
 *   <li>3.5 s: the cage closes on the body, slowly, turning (seen in black and white by the two players, Enzo's
 *   choice);</li>
 *   <li>5.0 s: the executor closes his fist and pulls it down: the execution (KnockdownService.finish). A great bloom of
 *   crystal opens, the cage bursts and the body bursts with it, in slow motion: its head, torso, arms and legs (in its
 *   own skin) drift apart among gouts of blood and the cage's big shards; 5.6 s: time runs again, everything is flung
 *   away and falls;</li>
 *   <li>6.1 s: the crater: great crystals rise where the body lay; the pieces of the body land in pools of blood and
 *   stay there until the end; shards rain down.</li>
 * </ol>
 * Everything shown is its own models (ScarletPieceEntity: crystals from Blockbench, stones of the ground's own block),
 * Enzo wanting no particles: nothing is built in the world or saved, and all of it is gone 10 s after the start (or at
 * once if the finish is broken off, the body then put back where it lay).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class ScarletBloom {
    /** The film's beats, in ticks from the start. */
    public static final int LAUNCH_TICK = 14, PEAK_TICK = 40, CAGE_TICK = 40, CLOSE_TICK = 70, BURST_TICK = 100, RESUME_TICK = 112,
            LAND_TICK = 122, RAIN_END_TICK = 150, CLEAR_TICK = 230;
    /** The execution: the body bursts with the cage. */
    public static final int HIT_TICK = BURST_TICK;
    /** The rain of blood after the burst: how long it falls (ticks), how many drops a tick, over how wide (blocks). */
    public static final int BLOOD_RAIN_TICKS = 80, BLOOD_DROPS = 5;
    static final double BLOOD_RAIN_RADIUS = 5.5;
    /** How far the cage closes on the body (a share of its size), and how fast it turns meanwhile (radians a tick). */
    static final double CLOSE_SHARE = 0.55, CLOSE_TURN = 0.025;
    /** How high the body is thrown (blocks). */
    public static final double RISE = 9.0;

    private static final Map<UUID, Show> SHOWS = new HashMap<>();

    private ScarletBloom() { }

    // ---------------------------------------------------------------- who may

    /** Whether this player has unlocked every technique of the Sanguine Art. */
    public static boolean masters(Player player) {
        MartialArts.Art art = MartialArts.art(MartialArts.SANGUINE_ART);
        if (art == null) return false;
        MartialData data = MartialService.data(player);
        for (MartialArts.Technique t : art.techniques()) if (!data.hasTechnique(t.id())) return false;
        return true;
    }

    /** Whether this executor plays Scarlet Bloom: the Sanguine Art their main art, every technique of it unlocked. */
    public static boolean finishes(ServerPlayer executor) {
        return SanguineArt.stanceActive(executor) && masters(executor);
    }

    // ---------------------------------------------------------------- the show

    /** Begins the show (KnockdownService.execute). */
    public static void start(ServerPlayer executor, ServerPlayer victim) {
        abort(executor.getUUID(), executor.server);
        SHOWS.put(executor.getUUID(), new Show(executor, victim));
    }

    /** Breaks off the show: everything taken away at once, the body put back on the ground where it lay. */
    public static void abort(UUID executor, MinecraftServer server) {
        Show show = SHOWS.remove(executor);
        if (show == null) return;
        if (show.tick < HIT_TICK && server.getPlayerList().getPlayer(show.victim) instanceof ServerPlayer victim && victim.isAlive()) {
            victim.teleportTo(show.level, show.origin.x, show.origin.y, show.origin.z, victim.getYRot(), victim.getXRot());
            victim.setInvisible(false);
            victim.setNoGravity(false);
        }
        show.clear();
    }

    /**
     * Whether this player is the body being swept into the sky (from the eruption to the burst): it is then shown in its
     * own animation, not lying knocked down (KnockdownService keeps its knocked-down pose away meanwhile).
     */
    public static boolean sweeps(Player victim) {
        for (Show show : SHOWS.values()) if (show.victim.equals(victim.getUUID()) && show.tick >= LAUNCH_TICK && show.tick <= BURST_TICK) return true;
        return false;
    }

    /** Whether a show is running for this executor (tests). */
    public static boolean showing(Player executor) {
        return SHOWS.containsKey(executor.getUUID());
    }

    /** Where the body is carried at this tick of a show begun with it lying at {@code origin}, thrown away from {@code ahead}. */
    public static Vec3 bodyAt(Vec3 origin, Vec3 ahead, int t) {
        Vec3 drift = ahead.scale(0.6);
        if (t < LAUNCH_TICK) return origin;
        if (t < PEAK_TICK) {
            float k = (t - LAUNCH_TICK) / (float) (PEAK_TICK - LAUNCH_TICK), ease = 1 - (1 - k) * (1 - k) * (1 - k);
            return origin.add(drift.scale(ease)).add(0, RISE * ease, 0);
        }
        if (t < RESUME_TICK) return origin.add(drift).add(0, RISE + 0.25 * Mth.sin((Math.min(t, BURST_TICK) - PEAK_TICK) * 0.2F), 0);
        if (t < HIT_TICK - 1) {
            float k = (t - RESUME_TICK) / (float) (HIT_TICK - 1 - RESUME_TICK);
            return origin.add(drift.scale(1 - k)).add(0, RISE * (1 - k * k), 0);
        }
        return origin;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Show show : List.copyOf(SHOWS.values())) {
            if (!show.step(event.getServer())) {
                show.clear();
                SHOWS.remove(show.executor);
            }
        }
    }

    /** One finish being shown. */
    private static final class Show {
        final UUID executor, victim;
        final ServerLevel level;
        /** Where the body lay, and the way from the executor to it (flat). */
        final Vec3 origin, ahead;
        final RandomSource random;
        int tick;
        final List<ScarletPieceEntity> spikes = new ArrayList<>(), shards = new ArrayList<>(), beams = new ArrayList<>(), crater = new ArrayList<>();
        final List<Flying> flying = new ArrayList<>();
        final List<ScarletPieceEntity> pools = new ArrayList<>();
        ScarletPieceEntity bloom;
        /** Which way the body faced when it burst. */
        float bodyYaw;
        Vec3[] cage = new Vec3[0], shardFrom = new Vec3[0];

        Show(ServerPlayer executor, ServerPlayer victim) {
            this.executor = executor.getUUID();
            this.victim = victim.getUUID();
            this.level = executor.serverLevel();
            this.origin = victim.position();
            Vec3 flat = new Vec3(origin.x - executor.getX(), 0, origin.z - executor.getZ());
            this.ahead = flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
            this.random = level.getRandom();
        }

        Vec3 body(int t) {
            return bodyAt(origin, ahead, t);
        }

        /** One tick; false once over. */
        boolean step(MinecraftServer server) {
            ServerPlayer exec = server.getPlayerList().getPlayer(executor);
            ServerPlayer body = server.getPlayerList().getPlayer(victim);
            int t = tick++;
            if (t > CLEAR_TICK) return false;
            // The body, carried through the sky until the blow (a server without flight must not kick it meanwhile).
            if (t <= HIT_TICK && body != null && body.isAlive() && body.level() == level) {
                Vec3 at = body(t);
                float yaw = t >= PEAK_TICK && t < CLOSE_TICK ? body.getYRot() + 9.0F : t >= CLOSE_TICK && t < RESUME_TICK ? body.getYRot() + 1.5F : body.getYRot();
                // Looking nowhere but where the animation turns the head: neither at the camera nor up and down.
                body.teleportTo(level, at.x, at.y, at.z, yaw, 0.0F);
                body.setYHeadRot(yaw);
                body.setYBodyRot(yaw);
                body.setForcedPose(t >= LAUNCH_TICK && t <= BURST_TICK ? null : body.getForcedPose());
                body.fallDistance = 0;
                body.setDeltaMovement(Vec3.ZERO);
                if (body.connection != null) body.connection.aboveGroundTickCount = 0;
            }
            if (t == 0) sound(origin, SoundEvents.WARDEN_HEARTBEAT, 1.4F, 0.8F);
            if (t == 11 && exec != null) {
                // The hand into the ground: a few stones jump at it.
                Vec3 hand = exec.position().add(ahead.scale(0.7));
                for (int i = 0; i < 4; i++) throwStone(hand, ground(hand), 0.2F + random.nextFloat() * 0.15F, 0.25, 0.1, 18);
                sound(hand, SoundEvents.STONE_BREAK, 1.2F, 0.6F);
            }
            if (t == LAUNCH_TICK) {
                erupt();
                // Thrown up, arched back and flailing, then spread, then curled (its own animation).
                if (body != null && body.isAlive()) {
                    // Weightless while swept (the clients then show it in its own animation, not lying knocked down).
                    body.setNoGravity(true);
                    var patch = com.murimblock.integration.epicfight.EpicFightBridge.patch(body);
                    if (patch != null) patch.playAnimationSynchronized(com.murimblock.integration.epicfight.MurimAnimations.SCARLET_BLOOM_FLUNG, 0.0F);
                }
            }
            // The body bursts at the next tick: from now on only its pieces are seen.
            if (t == BURST_TICK - 1 && body != null && body.isAlive()) bodyYaw = body.getYRot();
            if (t == BURST_TICK && body != null) {
                // Its pieces are all that is left: the body itself unseen, kept up there (its end not seen on the ground).
                body.setInvisible(true);
                body.setNoGravity(true);
            }
            if (t >= BURST_TICK && t < BURST_TICK + BLOOD_RAIN_TICKS) bloodRain(t);
            if (t == LAUNCH_TICK + 26) for (ScarletPieceEntity s : spikes) s.grow(0.01F, 6);
            if (t == LAUNCH_TICK + 33) { spikes.forEach(ScarletPieceEntity::discard); spikes.clear(); }
            if (t == CAGE_TICK && exec != null) cageShards(exec);
            if (t > CAGE_TICK && t <= CAGE_TICK + 8) flyShards(t - CAGE_TICK);
            if (t >= CAGE_TICK + 8 && t < CAGE_TICK + 8 + EDGES.length) weave(t - CAGE_TICK - 8);
            if (t >= CLOSE_TICK && t < BURST_TICK) close(t);
            if (t == CLOSE_TICK || t == CLOSE_TICK + 12 || t == CLOSE_TICK + 24) sound(body(t), SoundEvents.WARDEN_HEARTBEAT, 2.0F, 0.6F);
            if (t == BURST_TICK) burst();
            if (t > BURST_TICK && t < RESUME_TICK) spin();
            if (t == RESUME_TICK) resume();
            if (t == RESUME_TICK + 4 && bloom != null) bloom.grow(0.01F, 5);
            if (t == RESUME_TICK + 9 && bloom != null) { bloom.discard(); bloom = null; }
            if (t == LAND_TICK) land();
            if (t > LAND_TICK && t <= RAIN_END_TICK && t % 2 == 0) rain();
            moveFlying(t);
            return true;
        }

        /** The ring of crystals bursts from the ground, the great spike under the body, the stones fly. */
        void erupt() {
            spikes.add(spike(origin.add(0, -0.6, 0), new Vec3(0, 1, 0), 1.2F, 5.0F, 3));
            for (int i = 0; i < 14; i++) {
                double a = Math.PI * 2 * i / 14 + random.nextDouble() * 0.3;
                Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
                Vec3 base = origin.add(out.scale(1.6 + random.nextDouble() * 0.9)).add(0, -0.4, 0);
                Vec3 dir = out.scale(Math.tan(Math.toRadians(22 + random.nextInt(14)))).add(0, 1, 0).normalize();
                spikes.add(spike(base, dir, 0.55F + random.nextFloat() * 0.4F, 2.4F + random.nextFloat() * 1.8F, 2 + random.nextInt(3)));
            }
            BlockState earth = ground(origin);
            for (int i = 0; i < 24; i++) throwStone(origin.add(0, 0.2, 0), earth, 0.35F + random.nextFloat() * 0.6F, 0.15 + random.nextDouble() * 0.25,
                    0.45 + random.nextDouble() * 0.45, LAUNCH_TICK + 50);
            sound(origin, SoundEvents.GENERIC_EXPLODE.value(), 1.3F, 0.7F);
            sound(origin, SoundEvents.AMETHYST_BLOCK_BREAK, 1.6F, 0.5F);
        }

        /** Ten crystals leave the executor's back for their places around the body. */
        void cageShards(ServerPlayer exec) {
            Vec3 centre = body(BURST_TICK - 1).add(0, 0.9, 0);
            cage = new Vec3[10];
            shardFrom = new Vec3[10];
            Vec3 back = exec.position().add(0, 1.4, 0).subtract(ahead.scale(0.35));
            for (int i = 0; i < 10; i++) {
                int ring = i / 5, j = i % 5;
                double a = Math.PI * 2 * j / 5 + (ring == 1 ? Math.PI / 5 : 0);
                cage[i] = centre.add(Math.cos(a) * 3.0, ring == 0 ? -1.4 : 1.4, Math.sin(a) * 3.0);
                double b = Math.PI * 2 * i / 10;
                shardFrom[i] = back.add(Math.cos(b) * 0.4, 0.3 * Math.sin(b * 2), Math.sin(b) * 0.4);
                ScarletPieceEntity shard = piece(shardFrom[i], ScarletPieceEntity.SHARD, 0.45F, 1.3F, along(centre.subtract(cage[i]).normalize()));
                shards.add(shard);
            }
            sound(back, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2F, 1.4F);
        }

        void flyShards(int k) {
            float e = 1 - (1 - k / 8.0F) * (1 - k / 8.0F);
            for (int i = 0; i < shards.size(); i++) {
                Vec3 at = shardFrom[i].lerp(cage[i], e).add(0, Math.sin(e * Math.PI) * 2.0, 0);
                shards.get(i).moveTo(at.x, at.y, at.z);
            }
        }

        /** The cage's edges: the lower ring, the upper ring, and the zigzag between them. */
        static final int[][] EDGES = {
                {0, 1}, {5, 6}, {0, 5}, {1, 5}, {1, 2}, {6, 7}, {1, 6}, {2, 6}, {2, 3}, {7, 8},
                {2, 7}, {3, 7}, {3, 4}, {8, 9}, {3, 8}, {4, 8}, {4, 0}, {9, 5}, {4, 9}, {0, 9}};

        /** One more beam of the cage, drawn out from one crystal to the next. */
        void weave(int i) {
            Vec3 a = cage[EDGES[i][0]], b = cage[EDGES[i][1]];
            ScarletPieceEntity beam = piece(a, ScarletPieceEntity.ROD, 0.16F, 0.01F, along(b.subtract(a).normalize()));
            beam.grow((float) a.distanceTo(b), 3);
            beams.add(beam);
            level.playSound(null, BlockPos.containing(a), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.4F, 1.2F + 0.05F * i);
        }

        /**
         * The cage closes on the body, slowly, turning: its crystals drawn in, its beams after them (one step a tick; the
         * two players see it in black and white).
         */
        void close(int t) {
            if (cage.length == 0) return;
            Vec3 centre = body(BURST_TICK - 1).add(0, 0.9, 0);
            float k = (t - CLOSE_TICK) / (float) (BURST_TICK - CLOSE_TICK);
            double in = CLOSE_SHARE * k * k * (3 - 2 * k), turn = (t - CLOSE_TICK) * CLOSE_TURN;
            double c = Math.cos(turn), sn = Math.sin(turn);
            Vec3[] now = new Vec3[cage.length];
            for (int i = 0; i < cage.length; i++) {
                Vec3 r = cage[i].subtract(centre);
                now[i] = centre.add(new Vec3(r.x * c - r.z * sn, r.y, r.x * sn + r.z * c).scale(1 - in));
                if (i < shards.size()) {
                    shards.get(i).moveTo(now[i].x, now[i].y, now[i].z);
                    shards.get(i).turn(along(centre.subtract(now[i]).normalize()));
                }
            }
            for (int i = 0; i < beams.size(); i++) {
                Vec3 a = now[EDGES[i][0]], b = now[EDGES[i][1]];
                ScarletPieceEntity beam = beams.get(i);
                beam.moveTo(a.x, a.y, a.z);
                beam.turn(along(b.subtract(a).normalize()));
                beam.grow((float) a.distanceTo(b), 1);
            }
            if (t == CLOSE_TICK) sound(centre, SoundEvents.BEACON_POWER_SELECT, 1.6F, 0.5F);
        }

        /**
         * The fist closes: a great bloom of crystal opens round the body and the cage bursts, in slow motion: its
         * crystals and more great shards drift out, turning, until time runs again ({@link #resume}).
         */
        void burst() {
            Vec3 c = body(BURST_TICK).add(0, 0.9, 0);
            beams.forEach(ScarletPieceEntity::discard);
            beams.clear();
            for (ScarletPieceEntity shard : shards) {
                Vec3 out = shard.position().subtract(c).normalize();
                flying.add(new Flying(shard, out.scale(0.1), RESUME_TICK + 30, true));
            }
            shards.clear();
            for (int i = 0; i < 16; i++) {
                Vec3 out = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.6 + 0.2, random.nextGaussian()).normalize();
                ScarletPieceEntity s = piece(c.add(out.scale(0.6)), ScarletPieceEntity.SHARD, 0.4F + random.nextFloat() * 0.35F,
                        1.1F + random.nextFloat() * 0.9F, along(out));
                flying.add(new Flying(s, out.scale(0.06 + random.nextDouble() * 0.07), RESUME_TICK + 30, true));
            }
            burstBody(body(BURST_TICK));
            bloom = piece(c, ScarletPieceEntity.BLOOM, 1.0F, 0.01F, new Quaternionf());
            bloom.grow(3.0F, RESUME_TICK - BURST_TICK);
            sound(c, SoundEvents.GLASS_BREAK, 2.0F, 0.4F);
            sound(c, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 0.5F);
            sound(c, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.5F);
        }

        /**
         * The body bursts: its head, torso, arms and legs, each where it was, drift apart (slowly, until time runs
         * again), turning, among gouts of blood. They fall, land, and stay in pools of blood.
         */
        void burstBody(Vec3 feet) {
            double yaw = Math.toRadians(-bodyYaw);
            Vec3 side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
            Vec3 centre = feet.add(0, 1.0, 0);
            // part, its place (sideways, up), size (for the blood round it)
            double[][] parts = {{ScarletPieceEntity.HEAD, 0, 1.75}, {ScarletPieceEntity.TORSO, 0, 1.1}, {ScarletPieceEntity.RIGHT_ARM, -0.37, 1.15},
                    {ScarletPieceEntity.LEFT_ARM, 0.37, 1.15}, {ScarletPieceEntity.RIGHT_LEG, -0.12, 0.4}, {ScarletPieceEntity.LEFT_LEG, 0.12, 0.4}};
            for (double[] p : parts) {
                Vec3 at = feet.add(side.scale(p[1])).add(0, p[2], 0);
                Vec3 out = at.subtract(centre).add(random.nextGaussian() * 0.3, 0.25, random.nextGaussian() * 0.3).normalize();
                ScarletPieceEntity limb = piece(at, ScarletPieceEntity.LIMB, 1.0F, 1.0F,
                        new Quaternionf().rotationY((float) Math.toRadians(-bodyYaw)));
                limb.limb(victim, (byte) p[0]);
                Flying f = new Flying(limb, out.scale(0.1 + random.nextDouble() * 0.04), CLEAR_TICK, true);
                f.sticks = true;
                f.resume = 1.6;
                flying.add(f);
            }
            for (int i = 0; i < 18; i++) {
                Vec3 out = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.5 + 0.3, random.nextGaussian()).normalize();
                ScarletPieceEntity gout = piece(centre.add(out.scale(0.3)), ScarletPieceEntity.BLOOD, 1.0F, 0.25F + random.nextFloat() * 0.35F,
                        new Quaternionf().rotationXYZ(random.nextFloat() * 6.28F, random.nextFloat() * 6.28F, random.nextFloat() * 6.28F));
                Flying f = new Flying(gout, out.scale(0.05 + random.nextDouble() * 0.06), CLEAR_TICK, true);
                f.sticks = true;
                f.resume = 2.8;
                flying.add(f);
            }
            sound(centre, SoundEvents.SLIME_SQUISH, 1.6F, 0.5F);
            sound(centre, SoundEvents.PLAYER_HURT, 1.2F, 0.6F);
        }

        /**
         * A rain of blood over the crater for four seconds from the burst: drops (the blood's own model, small, stretched
         * by their fall) falling from above the body all around; one in four leaves a small pool where it lands.
         */
        void bloodRain(int t) {
            Vec3 sky = body(BURST_TICK);
            for (int i = 0; i < BLOOD_DROPS; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = Math.sqrt(random.nextDouble()) * BLOOD_RAIN_RADIUS;
                Vec3 at = new Vec3(origin.x + Math.cos(a) * r, sky.y + 1.5 + random.nextDouble() * 3, origin.z + Math.sin(a) * r);
                ScarletPieceEntity drop = piece(at, ScarletPieceEntity.BLOOD, 1.0F, 0.12F + random.nextFloat() * 0.12F, new Quaternionf());
                Flying f = new Flying(drop, new Vec3(0, -0.35 - random.nextDouble() * 0.25, 0), t + 60, false);
                f.sticks = true;
                f.rain = random.nextInt(4) != 0;
                flying.add(f);
            }
            if (t % 10 == 0) sound(origin, SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA, 1.2F, 0.6F);
        }

        /** In the slow motion, the drifting crystals turn a little. */
        void spin() {
            for (Flying f : flying) if (f.slow) {
                byte k = f.piece.kind();
                if (k == ScarletPieceEntity.SHARD) f.piece.turn(f.piece.turn().rotateAxis(0.06F, 0.3F, 1.0F, 0.2F));
                if (k == ScarletPieceEntity.LIMB || k == ScarletPieceEntity.BLOOD) f.piece.turn(f.piece.turn().rotateAxis(0.04F, 1.0F, 0.2F, 0.5F));
            }
        }

        /** Time runs again: everything drifting is flung away, and the body falls. */
        void resume() {
            for (Flying f : flying) if (f.slow) { f.slow = false; f.velocity = f.velocity.scale(f.resume).add(0, 0.15, 0); }
            sound(body(RESUME_TICK), SoundEvents.LIGHTNING_BOLT_IMPACT, 1.4F, 0.6F);
        }

        /** The body lands, executed, in a crater of crystals. */
        void land() {
            for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * i / 12 + 0.2;
                Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
                // None on the executor's side: he stands at the crater's edge, and the camera comes round him.
                if (out.dot(ahead) < -0.35) continue;
                Vec3 dir = out.scale(Math.tan(Math.toRadians(40))).add(0, 1, 0).normalize();
                crater.add(spike(origin.add(out.scale(1.3 + (i % 2) * 0.6)).add(0, -0.4, 0), dir, 0.45F + random.nextFloat() * 0.35F, 1.6F + random.nextFloat() * 1.6F, 2));
            }
            BlockState earth = ground(origin);
            for (int i = 0; i < 10; i++) throwStone(origin.add(0, 0.2, 0), earth, 0.25F + random.nextFloat() * 0.3F, 0.2 + random.nextDouble() * 0.2, 0.3 + random.nextDouble() * 0.3, LAND_TICK + 30);
            sound(origin, SoundEvents.ANVIL_LAND, 1.2F, 0.5F);
            sound(origin, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.6F);
        }

        /** A rain of small crystal shards over the crater. */
        void rain() {
            for (int i = 0; i < 2; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * 4.0;
                Vec3 at = origin.add(Math.cos(a) * r, 7 + random.nextDouble() * 2, Math.sin(a) * r);
                ScarletPieceEntity s = piece(at, ScarletPieceEntity.SHARD, 0.15F, 0.3F + random.nextFloat() * 0.25F, along(new Vec3(0, -1, 0)));
                flying.add(new Flying(s, new Vec3(0, -0.3, 0), tick + 40));
            }
        }

        void throwStone(Vec3 from, BlockState state, float size, double out, double up, int until) {
            double a = random.nextDouble() * Math.PI * 2;
            Vec3 vel = new Vec3(Math.cos(a) * out, up, Math.sin(a) * out);
            ScarletPieceEntity stone = piece(from.add(Math.cos(a) * 0.6, 0, Math.sin(a) * 0.6), ScarletPieceEntity.STONE, size, size,
                    new Quaternionf().rotationXYZ(random.nextFloat() * 6.28F, random.nextFloat() * 6.28F, random.nextFloat() * 6.28F));
            stone.block(state);
            flying.add(new Flying(stone, vel, until));
        }

        void moveFlying(int t) {
            for (Flying f : List.copyOf(flying)) {
                if (f.landed) continue;
                if (!f.slow) f.velocity = f.velocity.add(0, -0.06, 0).scale(0.98);
                Vec3 at = f.piece.position().add(f.velocity);
                if (f.sticks && f.velocity.y < 0) {
                    // A piece of the body or a gout of blood: on the ground it stays, in a pool of blood.
                    BlockPos column = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(at));
                    double ground = column.getY() + 0.02;
                    double rest = f.piece.kind() == ScarletPieceEntity.LIMB ? 0.18 : 0.08;
                    if (at.y <= ground + rest && f.rain) {
                        // A drop of the rain that leaves no pool: gone where it lands.
                        f.piece.discard();
                        flying.remove(f);
                        continue;
                    }
                    if (at.y <= ground + rest) {
                        f.landed = true;
                        f.piece.moveTo(at.x, ground + rest, at.z);
                        ScarletPieceEntity pool = piece(new Vec3(at.x, ground, at.z), ScarletPieceEntity.PUDDLE, 1.0F, 0.05F,
                                new Quaternionf().rotationY(random.nextFloat() * 6.28F));
                        pool.grow(f.piece.kind() == ScarletPieceEntity.LIMB ? 1.4F : f.piece.length(0) < 0.25F ? 0.35F + random.nextFloat() * 0.3F
                                : 0.6F + random.nextFloat() * 0.5F, 10);
                        pools.add(pool);
                        if (random.nextInt(3) == 0) sound(at, SoundEvents.SLIME_SQUISH_SMALL, 0.8F, 0.6F);
                        continue;
                    }
                }
                if (t > f.until || at.y < origin.y - 1.0) {
                    f.piece.discard();
                    flying.remove(f);
                } else f.piece.moveTo(at.x, at.y, at.z);
            }
        }

        // ---------------------------------------------------------------- the pieces

        /** A crystal spike from its base along a direction, grown to its length over a few ticks. */
        ScarletPieceEntity spike(Vec3 base, Vec3 dir, float width, float length, int grow) {
            ScarletPieceEntity s = piece(base, ScarletPieceEntity.SPIKE, width, 0.01F, along(dir));
            s.grow(length, grow);
            return s;
        }

        ScarletPieceEntity piece(Vec3 at, byte kind, float width, float length, Quaternionf turn) {
            ScarletPieceEntity p = new ScarletPieceEntity(MurimEntities.SCARLET_PIECE.get(), level);
            p.shape(kind, width, length, turn);
            p.moveTo(at.x, at.y, at.z);
            level.addFreshEntity(p);
            return p;
        }

        /** The turn bringing +Y onto this direction. */
        static Quaternionf along(Vec3 dir) {
            return new Quaternionf().rotationTo(new Vector3f(0, 1, 0), new Vector3f((float) dir.x, (float) dir.y, (float) dir.z));
        }

        void clear() {
            for (List<ScarletPieceEntity> list : List.of(spikes, shards, beams, crater)) { list.forEach(ScarletPieceEntity::discard); list.clear(); }
            flying.forEach(f -> f.piece.discard());
            flying.clear();
            pools.forEach(ScarletPieceEntity::discard);
            pools.clear();
            if (bloom != null) { bloom.discard(); bloom = null; }
        }

        BlockState ground(Vec3 at) {
            BlockState s = level.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
            return s.isAir() || !s.getFluidState().isEmpty() ? Blocks.DIRT.defaultBlockState() : s;
        }

        void sound(Vec3 at, SoundEvent sound, float volume, float pitch) {
            level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
        }
    }

    /** A piece thrown through the air. */
    private static final class Flying {
        final ScarletPieceEntity piece;
        Vec3 velocity;
        final int until;
        /** Drifting in the slow motion: no weight yet. */
        boolean slow;
        /** How much faster it goes when time runs again. */
        double resume = 6.0;
        /** A piece of the body or of blood: it lands and stays (until the end), and has landed. */
        boolean sticks, landed;
        /** A drop of the rain that leaves no pool. */
        boolean rain;

        Flying(ScarletPieceEntity piece, Vec3 velocity, int until) {
            this(piece, velocity, until, false);
        }

        Flying(ScarletPieceEntity piece, Vec3 velocity, int until, boolean slow) {
            this.piece = piece;
            this.velocity = velocity;
            this.until = until;
            this.slow = slow;
        }
    }
}
