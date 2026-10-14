package com.murimblock.cultivation.inner;

import com.murimblock.cultivation.CultivationJournal;
import com.murimblock.cultivation.CultivationRules;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.mob.MurimEntities;
import com.murimblock.qi.QiFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * The day's missions in the inner world, given by the remnant soul (docs/cultivation/qi-cultivation.md), each once per
 * in-game day, each worth a share of the realm's span of Qi Max:
 * <ul>
 *   <li><b>Gather the Scattered Qi</b>: motes of Qi drift over the island; touch them all within a minute;</li>
 *   <li><b>Spar with Your Shadow</b>: a double of yourself, half as strong; beat it (losing costs nothing).</li>
 * </ul>
 */
public final class InnerMissions {
    public enum Mission {
        STORM("storm", CultivationRules.STORM_SHARE), SPARRING("sparring", CultivationRules.SPARRING_SHARE);

        public final String id;
        final double share;

        Mission(String id, double share) {
            this.id = id;
            this.share = share;
        }
    }

    /** Motes to gather, and how long the storm lasts (ticks). */
    public static final int MOTES = 10, STORM_TICKS = 60 * 20;

    private static final class Running {
        final Mission mission;
        final long until;
        int gathered;

        Running(Mission mission, long until) {
            this.mission = mission;
            this.until = until;
        }
    }

    private static final Map<UUID, Running> RUNNING = new HashMap<>();

    private InnerMissions() {
    }

    /** The remnant soul spoken to: the mission under way, or the next one of the day, or nothing more today. */
    public static void talk(ServerPlayer player, RemnantSoulEntity soul) {
        if (!InnerWorldService.isInside(player)) return;
        if (InnerWorldService.session(player).breakthrough()) {
            player.sendSystemMessage(Component.translatable("cultivation.murimblock.soul.demon_first"));
            return;
        }
        Running running = RUNNING.get(player.getUUID());
        if (running != null) {
            player.sendSystemMessage(Component.translatable("cultivation.murimblock.soul.busy." + running.mission.id));
            return;
        }
        long now = player.level().getGameTime();
        CultivationJournal journal = CultivationService.journal(player);
        for (Mission mission : Mission.values()) {
            if (journal.missionDone(mission.id, now)) continue;
            start(player, mission);
            return;
        }
        player.sendSystemMessage(Component.translatable("cultivation.murimblock.soul.tomorrow"));
    }

    public static void start(ServerPlayer player, Mission mission) {
        ServerLevel level = player.serverLevel();
        InnerIsland.Layout layout = InnerWorldService.layoutOf(player);
        RUNNING.put(player.getUUID(), new Running(mission, level.getGameTime() + STORM_TICKS));
        double reward = CultivationRules.missionReward(CultivationService.getRealm(player), mission.share);
        player.sendSystemMessage(Component.translatable("cultivation.murimblock.soul.start." + mission.id, QiFormat.format(reward)));
        if (mission == Mission.STORM) {
            BlockPos c = layout.centre();
            for (int i = 0; i < MOTES; i++) {
                QiMoteEntity mote = MurimEntities.QI_MOTE.get().create(level);
                if (mote == null) continue;
                double angle = i * (Math.PI * 2 / MOTES) + InnerIsland.noise(i, 3, 11) * 0.5;
                double r = 3 + InnerIsland.noise(i, 5, 12) * Math.max(1, layout.radius() - 4.5);
                mote.moveTo(c.getX() + 0.5 + Math.cos(angle) * r, c.getY() + 1.6 + InnerIsland.noise(i, 7, 13) * 2.2, c.getZ() + 0.5 + Math.sin(angle) * r);
                mote.setOwner(player.getUUID());
                level.addFreshEntity(mote);
            }
            level.playSound(null, c, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
        } else {
            InnerWorldService.summonDemon(player, level, layout, CultivationRules.SHADOW_STRENGTH, true);
        }
    }

    public static boolean sparring(ServerPlayer player) {
        Running running = RUNNING.get(player.getUUID());
        return running != null && running.mission == Mission.SPARRING;
    }

    public static void moteGathered(ServerPlayer player, QiMoteEntity mote) {
        Running running = RUNNING.get(player.getUUID());
        if (running == null || running.mission != Mission.STORM) return;
        running.gathered++;
        player.level().playSound(null, mote.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F + running.gathered * 0.08F);
        player.displayClientMessage(Component.translatable("cultivation.murimblock.soul.gathered", running.gathered, MOTES), true);
        if (running.gathered >= MOTES) complete(player, running.mission);
    }

    public static void shadowDefeated(ServerPlayer player) {
        if (sparring(player)) complete(player, Mission.SPARRING);
    }

    public static void sparringLost(ServerPlayer player) {
        RUNNING.remove(player.getUUID());
        InnerWorldService.clearIsland(player);
        player.setHealth(player.getMaxHealth());
        player.sendSystemMessage(Component.translatable("cultivation.murimblock.soul.lost.sparring"));
    }

    public static void tick(ServerPlayer player) {
        Running running = RUNNING.get(player.getUUID());
        if (running == null || running.mission != Mission.STORM || player.level().getGameTime() < running.until) return;
        RUNNING.remove(player.getUUID());
        InnerWorldService.clearIsland(player);
        player.sendSystemMessage(Component.translatable("cultivation.murimblock.soul.lost.storm", running.gathered, MOTES));
    }

    static void complete(ServerPlayer player, Mission mission) {
        RUNNING.remove(player.getUUID());
        long now = player.level().getGameTime();
        CultivationService.setJournal(player, CultivationService.journal(player).withMission(mission.id, now));
        double reward = CultivationRules.missionReward(CultivationService.getRealm(player), mission.share);
        double gained = CultivationService.gainQiMax(player, reward, CultivationService.Source.MISSION);
        player.sendSystemMessage(Component.translatable("cultivation.murimblock.soul.done." + mission.id, QiFormat.format(gained)));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.3F);
    }

    public static void end(ServerPlayer player) {
        RUNNING.remove(player.getUUID());
    }

    public static void clearForTests() {
        RUNNING.clear();
    }
}
