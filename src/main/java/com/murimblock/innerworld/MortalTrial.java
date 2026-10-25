package com.murimblock.innerworld;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.Cultivation;
import com.murimblock.cultivation.CultivationPayloads;
import com.murimblock.cultivation.CultivationRealms;
import com.murimblock.cultivation.CultivationRules;
import com.murimblock.cultivation.Meditation;
import com.murimblock.registry.MurimEntities;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The trial out of the mortal realm (Enzo, 25 Oct 2026): from Body Tempering stage 9 with a full gauge and the
 * meridians the realm asks for, BEGIN TRIAL starts a 3 s dive into the dantian (moving or being struck cancels it),
 * then sends the spirit to a graveyard island at night (the body stays seated), where it gets up and the title says
 * SURVIVE DEATH.
 * One wave of 15 dead climbs out of the graves, a few at a time; the last is the Grave Warden, out of the
 * mausoleum. Putting them all down breaks through to Third-Rate Warrior. Being struck down, falling off, leaving, logging out or a blow on
 * the body loses the trial and scatters 30 % of the gathered stage Qi. Runs live in memory only.
 */
public final class MortalTrial {
    /** The dead to put down (Enzo, 25 Oct): one wave of 15, the last of them the Grave Warden. */
    public static final int TOTAL = 15;
    /** At most this many climb out at once, so a mortal can face them. */
    public static final int MAX_ALIVE = 4;
    /** Climbing out together when the wave starts. */
    public static final int FIRST_RISE = 2;
    /** Ticks between two of the dead climbing out (while fewer than {@link #MAX_ALIVE} stand). */
    private static final int RISE_EVERY = 60;
    public static final double LOSS = 0.30;
    /** The dive into the dantian before the spirit leaves, in ticks. */
    public static final int DIVE_TICKS = 60;
    /** Calm before the wave: the arrival, getting up and the title come first. */
    private static final int FIRST_CALM = 140;
    private static final int RETARGET = 20;

    /** One player's trial in progress. */
    private static final class Run {
        int calm = FIRST_CALM;
        int risen;
        int killed;
        int cooldown;
        final List<UUID> alive = new ArrayList<>();
        Status sent;
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();
    /** Players diving into their dantian, with the ticks left. */
    private static final Map<UUID, Integer> DIVES = new HashMap<>();

    /** Server to the player and those around: the dive starts or is cancelled; to the player: they arrived. */
    public record Dive(int entityId, int phase) implements CustomPacketPayload {
        public static final int START = 0;
        public static final int CANCEL = 1;
        public static final int ARRIVE = 2;
        public static final Type<Dive> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "trial_dive"));
        public static final StreamCodec<ByteBuf, Dive> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Dive::entityId, ByteBufCodecs.VAR_INT, Dive::phase, Dive::new);

        @Override
        public Type<Dive> type() {
            return TYPE;
        }
    }

    /** Server to the player: the wave counter (wave 0-based, -1 before the first; calm in seconds; inactive ends it). */
    public record Status(boolean active, int wave, int waves, int left, int total, int calm) implements CustomPacketPayload {
        public static final Type<Status> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "trial_status"));
        public static final StreamCodec<ByteBuf, Status> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Status::active, ByteBufCodecs.VAR_INT, s -> s.wave() + 1, ByteBufCodecs.VAR_INT, Status::waves,
                ByteBufCodecs.VAR_INT, Status::left, ByteBufCodecs.VAR_INT, Status::total, ByteBufCodecs.VAR_INT, Status::calm,
                (active, wave, waves, left, total, calm) -> new Status(active, wave - 1, waves, left, total, calm));
        public static final Status NONE = new Status(false, -1, 1, 0, 0, 0);

        @Override
        public Type<Status> type() {
            return TYPE;
        }
    }

    /** Client to server: BEGIN TRIAL. */
    public record Begin() implements CustomPacketPayload {
        public static final Type<Begin> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "trial_begin"));
        public static final StreamCodec<ByteBuf, Begin> STREAM_CODEC = StreamCodec.unit(new Begin());

        @Override
        public Type<Begin> type() {
            return TYPE;
        }
    }

    private MortalTrial() {
    }

    /** Mortal, last stage, full gauge, enough meridians open. */
    public static boolean ready(int realm, int stage, double stageQi, int meridians) {
        return realm == 0 && stage >= CultivationRules.STAGES && CultivationRules.gaugeFull(realm, stage, stageQi)
                && meridians >= CultivationRules.meridiansForTrial(realm);
    }

    public static boolean diving(ServerPlayer player) {
        return DIVES.containsKey(player.getUUID());
    }

    public static boolean active(ServerPlayer player) {
        return RUNS.containsKey(player.getUUID());
    }

    public static void begin(ServerPlayer player) {
        Cultivation c = Meditation.of(player);
        ServerLevel trial = player.server.getLevel(InnerWorld.TRIAL_LEVEL);
        if (active(player) || DIVES.containsKey(player.getUUID()) || InnerWorld.state(player).inside()
                || InnerWorld.isInnerWorld(player.level())) {
            return;
        }
        if (!ready(c.realm(), c.stage(), c.stageQi(), c.meridians())) {
            InnerWorld.system(player, "message.murimblock.trial.not_ready", ChatFormatting.GOLD);
            return;
        }
        if (!c.meditating()) {
            InnerWorld.system(player, "message.murimblock.trial.meditate", ChatFormatting.GOLD);
            return;
        }
        if (trial == null) {
            InnerWorld.system(player, "message.murimblock.inner_world.unavailable", ChatFormatting.RED);
            return;
        }
        DIVES.put(player.getUUID(), DIVE_TICKS);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new Dive(player.getId(), Dive.START));
    }

    /** Every tick, anywhere: counts the dive down; standing up or being struck (which ends meditation) cancels it. */
    static void tickDive(ServerPlayer player) {
        Integer left = DIVES.get(player.getUUID());
        if (left == null) {
            return;
        }
        if (!Meditation.of(player).meditating()) {
            DIVES.remove(player.getUUID());
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new Dive(player.getId(), Dive.CANCEL));
        } else if (left > 1) {
            DIVES.put(player.getUUID(), left - 1);
        } else {
            DIVES.remove(player.getUUID());
            enter(player);
        }
    }

    private static void enter(ServerPlayer player) {
        ServerLevel trial = player.server.getLevel(InnerWorld.TRIAL_LEVEL);
        if (trial == null) {
            return;
        }
        BlockPos center = InnerIslands.get(player.server).prepareTrial(player.getUUID(), trial);
        trial.getEntitiesOfClass(TrialZombie.class, new AABB(center).inflate(InnerIsland.CLEAR_RADIUS))
                .forEach(Entity::discard);
        InnerWorld.sendSpirit(player, trial, center);
        // The spirit arrives whole: full health and a full stomach, whatever the body went through.
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5);
        player.clearFire();
        RUNS.put(player.getUUID(), new Run());
        PacketDistributor.sendToPlayer(player, new Dive(player.getId(), Dive.ARRIVE));
        player.displayClientMessage(Component.translatable("message.murimblock.trial.begin").withStyle(ChatFormatting.DARK_AQUA), false);
        status(player, RUNS.get(player.getUUID()));
    }

    /** Every tick while the player is in the trial dimension. */
    static void tick(ServerPlayer player) {
        Run run = RUNS.get(player.getUUID());
        if (run == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        int before = run.alive.size();
        run.alive.removeIf(id -> !(level.getEntity(id) instanceof TrialZombie z) || !z.isAlive());
        run.killed += before - run.alive.size();
        if (run.killed >= TOTAL) {
            win(player);
            return;
        }
        if (run.calm > 0) {
            if (--run.calm == 0) {
                level.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.5f, 0.5f);
                player.displayClientMessage(Component.translatable("message.murimblock.trial.wave", TOTAL)
                        .withStyle(ChatFormatting.RED), true);
                for (int i = 0; i < FIRST_RISE; i++) {
                    rise(player, level, run);
                }
                run.cooldown = RISE_EVERY;
            }
        } else {
            if (run.cooldown > 0) {
                run.cooldown--;
            }
            if (run.risen < TOTAL - 1 && run.cooldown == 0 && run.alive.size() < MAX_ALIVE) {
                rise(player, level, run);
                run.cooldown = RISE_EVERY;
            } else if (run.risen == TOTAL - 1 && run.alive.isEmpty()) {
                // The last of them: the Grave Warden leaves the mausoleum once all the others are down.
                BlockPos center = InnerIslands.get(player.server).existingCenter(player.getUUID());
                BlockPos w = TrialIsland.WARDEN;
                spawn(player, level, run, center.getX() + w.getX() + 0.5, w.getY(), center.getZ() + w.getZ() + 0.5, true);
                level.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.5f, 0.4f);
                player.displayClientMessage(Component.translatable("message.murimblock.trial.warden").withStyle(ChatFormatting.DARK_RED), true);
            }
            if (player.tickCount % RETARGET == 0) {
                for (UUID id : run.alive) {
                    if (level.getEntity(id) instanceof TrialZombie z && z.getTarget() != player) {
                        z.setTarget(player);
                    }
                }
            }
        }
        status(player, run);
    }

    /** One of the dead climbs out of a grave, one away from the player when possible. */
    private static void rise(ServerPlayer player, ServerLevel level, Run run) {
        BlockPos center = InnerIslands.get(player.server).existingCenter(player.getUUID());
        List<BlockPos> graves = new ArrayList<>(TrialIsland.GRAVES);
        Collections.shuffle(graves, new Random(player.getRandom().nextLong()));
        graves.sort((a, b) -> Boolean.compare(near(player, center, a), near(player, center, b)));
        BlockPos g = graves.get(0);
        spawn(player, level, run, center.getX() + g.getX() + 0.5, g.getY() + 1, center.getZ() + g.getZ() + 0.5, false);
    }

    private static boolean near(ServerPlayer player, BlockPos center, BlockPos grave) {
        return player.distanceToSqr(center.getX() + grave.getX(), grave.getY(), center.getZ() + grave.getZ()) < 64;
    }

    private static void spawn(ServerPlayer player, ServerLevel level, Run run, double x, double y, double z, boolean warden) {
        TrialZombie zombie = MurimEntities.TRIAL_ZOMBIE.get().create(level);
        if (zombie == null) {
            return;
        }
        zombie.moveTo(x, y, z, player.getRandom().nextFloat() * 360, 0);
        if (warden) {
            zombie.makeWarden();
        }
        zombie.setTarget(player);
        level.addFreshEntity(zombie);
        run.alive.add(zombie.getUUID());
        run.risen++;
        // Earth thrown up as it climbs out.
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.defaultBlockState()),
                x, y + 0.2, z, warden ? 50 : 25, 0.35, 0.25, 0.35, 0.15);
        level.playSound(null, x, y, z, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1, 0.7f);
        level.playSound(null, x, y, z, warden ? SoundEvents.ZOMBIE_VILLAGER_AMBIENT : SoundEvents.ZOMBIE_AMBIENT,
                SoundSource.HOSTILE, 1, warden ? 0.5f : 0.9f);
    }

    private static void status(ServerPlayer player, Run run) {
        Status now = run == null ? Status.NONE
                : new Status(true, run.calm > 0 ? -1 : 0, 1, TOTAL - run.killed, TOTAL, (run.calm + 19) / 20);
        if (run == null || !now.equals(run.sent)) {
            if (run != null) {
                run.sent = now;
            }
            PacketDistributor.sendToPlayer(player, now);
        }
    }

    /** All waves cleared: back to the body, and the realm breaks through to Third-Rate Warrior. */
    private static void win(ServerPlayer player) {
        RUNS.remove(player.getUUID());
        status(player, null);
        InnerWorld.leave(player, null);
        Cultivation c = Meditation.of(player);
        if (c.breakRealm()) {
            Meditation.sync(player);
            Component rank = Component.translatable("murimblock.realm." + CultivationRealms.LADDER.get(c.realm()).rank());
            // Told to the player alone: a cultivator's trials stay secret (Enzo, 25 Oct).
            player.displayClientMessage(Component.translatable("message.murimblock.trial.won", rank)
                    .withStyle(ChatFormatting.AQUA), false);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    new CultivationPayloads.Breakthrough(player.getId(), c.realm(), c.stage()));
        }
    }

    /** Leaving in any way but victory (called as the spirit leaves, or on logout): the trial is lost. */
    static void lose(ServerPlayer player) {
        DIVES.remove(player.getUUID());
        Run run = RUNS.remove(player.getUUID());
        if (run == null) {
            return;
        }
        if (player.level() instanceof ServerLevel level) {
            run.alive.forEach(id -> {
                Entity e = level.getEntity(id);
                if (e != null) {
                    e.discard();
                }
            });
        }
        Meditation.of(player).scatter(LOSS);
        Meditation.sync(player);
        status(player, null);
        player.displayClientMessage(Component.translatable("message.murimblock.trial.lost", Math.round(LOSS * 100))
                .withStyle(ChatFormatting.RED), false);
    }

    /** A stopped server forgets its runs (an integrated server can restart in the same game). */
    static void clear() {
        RUNS.clear();
        DIVES.clear();
    }
}
