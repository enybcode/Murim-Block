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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The trial out of the mortal realm (Enzo, 25 Oct 2026): from Body Tempering stage 9 with a full gauge and the
 * meridians the realm asks for, BEGIN TRIAL sends the spirit to a graveyard island at night (the body stays seated).
 * Five waves of zombies climb out of the graves; the last brings the Grave Warden out of the mausoleum. Clearing
 * them all breaks through to Third-Rate Warrior. Being struck down, falling off, leaving, logging out or a blow on
 * the body loses the trial and scatters 30 % of the gathered stage Qi. Runs live in memory only.
 */
public final class MortalTrial {
    /** Zombies per wave; the last one adds the Grave Warden. */
    public static final int[] WAVES = {3, 5, 7, 9, 9};
    public static final double LOSS = 0.30;
    private static final int FIRST_CALM = 100;
    private static final int CALM = 80;
    private static final int RETARGET = 20;

    /** One player's trial in progress. */
    private static final class Run {
        int wave = -1;
        int calm = FIRST_CALM;
        int total;
        final List<UUID> alive = new ArrayList<>();
        Status sent;
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();

    /** Server to the player: the wave counter (wave 0-based, -1 before the first; calm in seconds; inactive ends it). */
    public record Status(boolean active, int wave, int waves, int left, int total, int calm) implements CustomPacketPayload {
        public static final Type<Status> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "trial_status"));
        public static final StreamCodec<ByteBuf, Status> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Status::active, ByteBufCodecs.VAR_INT, s -> s.wave() + 1, ByteBufCodecs.VAR_INT, Status::waves,
                ByteBufCodecs.VAR_INT, Status::left, ByteBufCodecs.VAR_INT, Status::total, ByteBufCodecs.VAR_INT, Status::calm,
                (active, wave, waves, left, total, calm) -> new Status(active, wave - 1, waves, left, total, calm));
        public static final Status NONE = new Status(false, -1, WAVES.length, 0, 0, 0);

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

    public static boolean active(ServerPlayer player) {
        return RUNS.containsKey(player.getUUID());
    }

    public static void begin(ServerPlayer player) {
        Cultivation c = Meditation.of(player);
        ServerLevel trial = player.server.getLevel(InnerWorld.TRIAL_LEVEL);
        if (active(player) || InnerWorld.state(player).inside() || InnerWorld.isInnerWorld(player.level())) {
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
        BlockPos center = InnerIslands.get(player.server).prepareTrial(player.getUUID(), trial);
        trial.getEntitiesOfClass(TrialZombie.class, new AABB(center).inflate(InnerIsland.CLEAR_RADIUS))
                .forEach(Entity::discard);
        InnerWorld.sendSpirit(player, trial, center);
        RUNS.put(player.getUUID(), new Run());
        player.displayClientMessage(Component.translatable("message.murimblock.trial.begin").withStyle(ChatFormatting.DARK_AQUA), false);
        trial.playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1, 0.6f);
        status(player, RUNS.get(player.getUUID()));
    }

    /** Every tick while the player is in the trial dimension. */
    static void tick(ServerPlayer player) {
        Run run = RUNS.get(player.getUUID());
        if (run == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        run.alive.removeIf(id -> !(level.getEntity(id) instanceof TrialZombie z) || !z.isAlive());
        if (run.calm > 0) {
            if (--run.calm == 0) {
                spawnWave(player, level, run);
            }
        } else if (run.alive.isEmpty()) {
            if (run.wave >= WAVES.length - 1) {
                win(player);
                return;
            }
            run.calm = CALM;
            player.displayClientMessage(Component.translatable("message.murimblock.trial.wave_cleared", run.wave + 1, WAVES.length)
                    .withStyle(ChatFormatting.AQUA), true);
        } else if (player.tickCount % RETARGET == 0) {
            for (UUID id : run.alive) {
                if (level.getEntity(id) instanceof TrialZombie z && z.getTarget() != player) {
                    z.setTarget(player);
                }
            }
        }
        status(player, run);
    }

    private static void spawnWave(ServerPlayer player, ServerLevel level, Run run) {
        run.wave++;
        BlockPos center = InnerIslands.get(player.server).existingCenter(player.getUUID());
        RandomSource random = player.getRandom();
        // Graves away from the player first, so nothing climbs out at their feet.
        List<BlockPos> graves = new ArrayList<>(TrialIsland.GRAVES);
        Collections.shuffle(graves, new Random(random.nextLong()));
        graves.sort((a, b) -> Boolean.compare(near(player, center, a), near(player, center, b)));
        int count = WAVES[run.wave];
        for (int i = 0; i < count; i++) {
            BlockPos g = graves.get(i % graves.size());
            spawn(player, level, run, center.getX() + g.getX() + 0.5, g.getY() + 1, center.getZ() + g.getZ() + 0.5, false);
        }
        if (run.wave == WAVES.length - 1) {
            BlockPos w = TrialIsland.WARDEN;
            spawn(player, level, run, center.getX() + w.getX() + 0.5, w.getY(), center.getZ() + w.getZ() + 0.5, true);
        }
        run.total = run.alive.size();
        level.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.5f, 0.5f);
        player.displayClientMessage(Component.translatable("message.murimblock.trial.wave", run.wave + 1, WAVES.length)
                .withStyle(ChatFormatting.RED), true);
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
        // Earth thrown up as it climbs out.
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.defaultBlockState()),
                x, y + 0.2, z, warden ? 50 : 25, 0.35, 0.25, 0.35, 0.15);
        level.playSound(null, x, y, z, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1, 0.7f);
        level.playSound(null, x, y, z, warden ? SoundEvents.ZOMBIE_VILLAGER_AMBIENT : SoundEvents.ZOMBIE_AMBIENT,
                SoundSource.HOSTILE, 1, warden ? 0.5f : 0.9f);
    }

    private static void status(ServerPlayer player, Run run) {
        Status now = run == null ? Status.NONE
                : new Status(true, run.wave, WAVES.length, run.alive.size(), run.total, (run.calm + 19) / 20);
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
            player.server.getPlayerList().broadcastSystemMessage(Component.translatable("message.murimblock.trial.won",
                    player.getDisplayName(), rank).withStyle(ChatFormatting.AQUA), false);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    new CultivationPayloads.Breakthrough(player.getId(), c.realm(), c.stage()));
        }
    }

    /** Leaving in any way but victory (called as the spirit leaves, or on logout): the trial is lost. */
    static void lose(ServerPlayer player) {
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
    }
}
