package com.murimblock.cultivation;

import com.murimblock.registry.MurimAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server side of cultivation: meditation rules, stage advance and syncing to clients. */
public final class Meditation {
    /** Hostile mobs closer than this forbid meditation. */
    public static final double ENEMY_RANGE = 8;
    /** Moving farther than this from where the player sat ends meditation. */
    public static final double MAX_DRIFT = 0.3;
    /**
     * Fewest ticks between two breaths of the QTE, so a client cannot spam good beats. Two marks are at least 0.75 s
     * apart and each may be pressed 0.16 s off, so honest breaths can come 0.43 s apart.
     */
    public static final int BREATH_INTERVAL = 8;
    private static final int SYNC_INTERVAL = 10;

    private Meditation() {
    }

    public static Cultivation of(ServerPlayer player) {
        return player.getData(MurimAttachments.CULTIVATION);
    }

    public static void toggle(ServerPlayer player) {
        if (of(player).meditating()) {
            stop(player, "message.murimblock.meditation.stop");
        } else {
            start(player);
        }
    }

    public static void start(ServerPlayer player) {
        String blocked = blockedReason(player);
        if (blocked != null) {
            system(player, blocked, ChatFormatting.GOLD);
            return;
        }
        of(player).startMeditation(player.getX(), player.getY(), player.getZ());
        system(player, "message.murimblock.meditation.start", ChatFormatting.AQUA);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, pose(player, true));
        sync(player);
    }

    public static void stop(ServerPlayer player, String reasonKey) {
        Cultivation c = of(player);
        if (!c.meditating()) {
            return;
        }
        c.stopMeditation();
        if (reasonKey != null) {
            system(player, reasonKey, ChatFormatting.AQUA);
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, pose(player, false));
        sync(player);
    }

    public static void tick(ServerPlayer player) {
        Cultivation c = of(player);
        if (c.meditating()) {
            if (c.driftFrom(player.getX(), player.getY(), player.getZ()) > MAX_DRIFT) {
                stop(player, "message.murimblock.meditation.moved");
                return;
            }
            if (player.tickCount % 20 == 0) {
                String blocked = blockedReason(player);
                if (blocked != null) {
                    stop(player, blocked);
                    return;
                }
                // Watchers size the Qi smoke on the Qi held.
                PacketDistributor.sendToPlayersTrackingEntity(player,
                        new CultivationPayloads.MeditatorQi(player.getId(), (int) c.qi()));
            }
        }
        boolean wasFull = c.gaugeFull();
        c.gather(1 / 20.0);
        if (!wasFull && c.gaugeFull()) {
            system(player, c.stage() < CultivationRules.STAGES ? "message.murimblock.cultivation.full"
                    : "message.murimblock.cultivation.full_trial", ChatFormatting.AQUA);
            sync(player);
        }
        if (player.tickCount % SYNC_INTERVAL == 0 && c.takeDirty()) {
            send(player, c);
        }
    }

    public static void advance(ServerPlayer player) {
        Cultivation c = of(player);
        if (!c.meditating()) {
            system(player, "message.murimblock.cultivation.advance_meditate", ChatFormatting.GOLD);
        } else if (c.stage() >= CultivationRules.STAGES && c.gaugeFull()) {
            system(player, "message.murimblock.cultivation.trial_required", ChatFormatting.GOLD);
        } else if (c.advance()) {
            player.displayClientMessage(Component.translatable("message.murimblock.cultivation.advanced",
                    Component.translatable("murimblock.cultivation_realm." + CultivationRealms.realmKey(c.realm())), c.stage())
                    .withStyle(ChatFormatting.AQUA), false);
            sync(player);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    new CultivationPayloads.Breakthrough(player.getId(), c.realm(), c.stage()));
        }
    }

    public static void breathe(ServerPlayer player, BreathResult result) {
        Cultivation c = of(player);
        if (c.meditating() && player.tickCount - c.lastBreathTick() >= BREATH_INTERVAL) {
            c.breathe(result, player.tickCount);
        }
    }

    /** Why the player cannot meditate here, as a message key, or null. */
    public static String blockedReason(ServerPlayer player) {
        if (player.isSpectator() || player.isPassenger() || player.isSleeping()) {
            return "message.murimblock.meditation.blocked";
        }
        if (player.isInWater() || player.isInLava()) {
            return "message.murimblock.meditation.water";
        }
        if (!player.onGround()) {
            return "message.murimblock.meditation.ground";
        }
        boolean enemy = !player.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                player.getBoundingBox().inflate(ENEMY_RANGE), m -> m instanceof Enemy && m.isAlive()).isEmpty();
        return enemy ? "message.murimblock.meditation.enemy" : null;
    }

    public static CultivationPayloads.Pose pose(ServerPlayer player, boolean meditating) {
        return new CultivationPayloads.Pose(player.getId(), meditating, (int) of(player).qi());
    }

    public static void sync(ServerPlayer player) {
        Cultivation c = of(player);
        c.takeDirty();
        send(player, c);
    }

    private static void send(ServerPlayer player, Cultivation c) {
        PacketDistributor.sendToPlayer(player, new CultivationPayloads.Sync(c.snapshot()));
    }

    static void system(ServerPlayer player, String key, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(key).withStyle(color), true);
    }
}
