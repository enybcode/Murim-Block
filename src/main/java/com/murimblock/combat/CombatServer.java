package com.murimblock.combat;

import com.murimblock.cultivation.Meditation;
import com.murimblock.registry.MurimAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/** The server's side of a fight: it checks each strike, finds who it reaches and deals the damage. */
public final class CombatServer {
    /** Ticks of recovery a new strike may cut, since the client's timeline runs a little ahead. */
    private static final int LAG_SLACK = 3;
    /** Ticks a blow taken keeps a fighter from acting. */
    public static final int FLINCH_TICKS = 8;
    /** The knockback of the blow being dealt now, read by {@link CombatEvents} when vanilla pushes the target. */
    private static double dealingKnockback = -1;

    private CombatServer() {
    }

    public static Fighter fighter(LivingEntity entity) {
        return entity.getData(MurimAttachments.FIGHTER);
    }

    /** The fighting style of what this player holds, or null when it has none. */
    public static ResourceLocation style(ServerPlayer player) {
        return CombatMovesets.forHeld(player.getMainHandItem());
    }

    /** Why combat mode cannot be on now (a lang key, empty to say nothing), or null when it can. */
    private static String blocker(ServerPlayer player) {
        if (style(player) == null) {
            return "message.murimblock.combat.no_style";
        }
        if (Meditation.of(player).meditating()) {
            return "message.murimblock.combat.meditating";
        }
        if (!player.isAlive() || player.isSpectator() || player.isSleeping()) {
            return "";
        }
        return null;
    }

    public static boolean inCombatMode(ServerPlayer player) {
        return fighter(player).combatMode();
    }

    public static void toggleCombatMode(ServerPlayer player) {
        Fighter fighter = fighter(player);
        if (fighter.combatMode()) {
            setCombatMode(player, false, "");
            return;
        }
        String blocked = blocker(player);
        if (blocked != null) {
            PacketDistributor.sendToPlayer(player, new CombatPayloads.CombatMode(false, blocked));
            return;
        }
        setCombatMode(player, true, "");
    }

    public static void setCombatMode(ServerPlayer player, boolean on, String reason) {
        fighter(player).combatMode(on);
        PacketDistributor.sendToPlayer(player, new CombatPayloads.CombatMode(on, reason));
    }

    public static void request(ServerPlayer player, ResourceLocation id) {
        CombatMove move = CombatMoves.get(id);
        Fighter fighter = fighter(player);
        CombatMoveset style = CombatMovesets.get(style(player));
        if (move == null || style == null || !style.chain().contains(id) || !fighter.combatMode()
                || blocker(player) != null || !fighter.clock().canEnter(FighterState.ATTACK, LAG_SLACK)) {
            PacketDistributor.sendToPlayer(player, new CombatPayloads.StrikeRefused());
            return;
        }
        fighter.strike(id, move);
        fighter.look(player.getYRot(), player.getXRot());
        PacketDistributor.sendToPlayersTrackingEntity(player, new CombatPayloads.Strike(player.getId(), id));
    }

    public static void tick(ServerPlayer player) {
        Fighter fighter = fighter(player);
        if (fighter.combatMode()) {
            String blocked = blocker(player);
            if (blocked != null) {
                setCombatMode(player, false, blocked);
            } else {
                // Another weapon, another style: what was being thrown with the old one stops.
                ResourceLocation style = style(player);
                if (!style.equals(fighter.style())) {
                    fighter.reset();
                    fighter.style(style);
                }
            }
        }
        if (fighter.clock().active()) {
            CombatMove move = fighter.move();
            for (LivingEntity target : StrikeReach.targets(player, move, fighter.lastYaw(), fighter.lastPitch(),
                    fighter.clock().activeIndex(), fighter.clock().activeCount())) {
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

    /**
     * A blow taken in combat mode: from a creature or a projectile, not fire, poison, a fall or hunger. The move in
     * progress and the chain stop, unless the move shrugs it off while it winds up.
     */
    public static void blowTaken(ServerPlayer player, DamageSource source) {
        Fighter fighter = fighter(player);
        if (!fighter.combatMode() || source.getEntity() == null && source.getDirectEntity() == null) {
            return;
        }
        if (fighter.flinch(FLINCH_TICKS)) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    new CombatPayloads.Flinch(player.getId(), FLINCH_TICKS));
        }
    }

    public static void stop(ServerPlayer player) {
        setCombatMode(player, false, "");
    }
}
