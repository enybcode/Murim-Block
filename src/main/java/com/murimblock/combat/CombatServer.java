package com.murimblock.combat;

import com.murimblock.cultivation.Meditation;
import com.murimblock.registry.MurimAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/** The server's side of a fight: it checks each strike, finds who it reaches and deals the damage. */
public final class CombatServer {
    /** Ticks of recovery a new strike may cut, since the client's timeline runs a little ahead. */
    private static final int LAG_SLACK = 3;
    /** The knockback of the blow being dealt now, read by {@link CombatEvents} when vanilla pushes the target. */
    private static double dealingKnockback = -1;

    private CombatServer() {
    }

    public static Fighter fighter(LivingEntity entity) {
        return entity.getData(MurimAttachments.FIGHTER);
    }

    /** Whether this player fights with bare hands now. */
    public static boolean bareHanded(ServerPlayer player) {
        return player.getMainHandItem().isEmpty();
    }

    public static void request(ServerPlayer player, ResourceLocation id) {
        CombatMove move = CombatMoves.get(id);
        Fighter fighter = fighter(player);
        if (move == null || !player.isAlive() || player.isSpectator() || player.isSleeping() || !bareHanded(player)
                || Meditation.of(player).meditating() || !fighter.clock().canStrike(LAG_SLACK)) {
            PacketDistributor.sendToPlayer(player, new CombatPayloads.StrikeRefused());
            return;
        }
        fighter.strike(id, move);
        fighter.look(player.getYRot(), player.getXRot());
        PacketDistributor.sendToPlayersTrackingEntity(player, new CombatPayloads.Strike(player.getId(), id));
    }

    public static void tick(ServerPlayer player) {
        Fighter fighter = fighter(player);
        if (fighter.clock().active()) {
            CombatMove move = fighter.move();
            for (LivingEntity target : StrikeReach.targets(player, move, fighter.lastYaw(), fighter.lastPitch())) {
                if (fighter.clock().markStruck(target.getId()) && hit(player, target, move)) {
                    fighter.clock().hitStop(move.hitStop());
                    PacketDistributor.sendToPlayersTrackingEntity(player,
                            new CombatPayloads.Impact(player.getId(), target.getId(), move.hitStop()));
                }
            }
        }
        fighter.look(player.getYRot(), player.getXRot());
        fighter.clock().advance();
    }

    /** One damage roll per blow: no vanilla attack, no attack cooldown, and the target's hurt delay is skipped. */
    private static boolean hit(ServerPlayer attacker, LivingEntity target, CombatMove move) {
        target.invulnerableTime = 0;
        dealingKnockback = move.knockback();
        try {
            boolean hurt = target.hurt(attacker.damageSources().playerAttack(attacker), move.damage());
            if (hurt) {
                attacker.setLastHurtMob(target);
                attacker.causeFoodExhaustion(0.1f);
            }
            return hurt;
        } finally {
            dealingKnockback = -1;
        }
    }

    /** The knockback to use instead of vanilla's, or -1 when no strike of ours is being dealt. */
    public static double dealingKnockback() {
        return dealingKnockback;
    }

    public static void stop(ServerPlayer player) {
        fighter(player).reset();
    }
}
