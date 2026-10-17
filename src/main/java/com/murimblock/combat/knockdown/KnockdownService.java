package com.murimblock.combat.knockdown;

import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.martial.AzureCloudSword;
import com.murimblock.martial.LongFist;
import com.murimblock.network.FinishCinematicPayload;
import com.murimblock.cultivation.MeditationService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Knock-down in PvP (docs/combat/STYLES.md, "Mise à terre et finish"): a player brought to zero health by another
 * player is not killed but falls K.O. They crawl very slowly and can do nothing else; a player next to them can carry
 * them (V) or execute them with a finish (B); otherwise they get up after {@link #DOWNED_TICKS}. All on the server; the
 * state is synced to every client, which only plays the matching animations.
 */
public final class KnockdownService {
    /** How long a knock-down lasts (30 s). The count pauses while carried or being executed. */
    public static final int DOWNED_TICKS = 600;
    /** Share of the maximum health a player gets up with. */
    public static final float RECOVER_HEALTH = 0.25F;
    /** Reach of the carry and execute actions, in blocks, and how far a carried or executed player may drift. */
    public static final double ACTION_REACH = 3.0;
    /** The finish: the blow lands this many ticks after it starts (the downward strike of finish_generic). */
    public static final int FINISH_HIT_TICK = 18;

    public static final ResourceKey<DamageType> EXECUTION = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "execution"));

    private static final ResourceLocation CRAWL_SLOW = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "knockdown_crawl");
    private static final ResourceLocation NO_JUMP = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "knockdown_no_jump");
    private static final ResourceLocation CARRY_SLOW = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "knockdown_carry");
    /** Held still while being executed. */
    private static final ResourceLocation HELD = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "knockdown_held");

    /** Finishes in progress, by executor. */
    private static final Map<UUID, Execution> EXECUTIONS = new HashMap<>();
    /** Carried players their carrier is putting down (the one dismount we allow). */
    private static final java.util.Set<UUID> RELEASING = new java.util.HashSet<>();

    /**
     * A finish being played: on whom, since when, and which (FinishCinematicPayload's GENERIC, AZURE_CLOUD, LONG_FIST); its
     * blow lands at its own tick.
     */
    private record Execution(UUID victim, int started, int finish) {
        int hitTick() {
            return switch (finish) {
                case FinishCinematicPayload.AZURE_CLOUD -> AzureCloudSword.FINISH_HIT_TICK;
                case FinishCinematicPayload.LONG_FIST -> LongFist.FINISH_HIT_TICK;
                case FinishCinematicPayload.SCARLET_BLOOM -> com.murimblock.martial.ScarletBloom.HIT_TICK;
                case FinishCinematicPayload.MOUNTAIN_BURIAL -> com.murimblock.martial.MountainShatteringFist.FINISH_HIT_TICK;
                case FinishCinematicPayload.SEVEN_STARS -> com.murimblock.martial.SevenStarsMoves.FINISH_HIT_TICK;
                case FinishCinematicPayload.THUNDER_SABER -> com.murimblock.martial.ThunderSaberMoves.FINISH_HIT_TICK;
                case FinishCinematicPayload.LAST_BELL -> com.murimblock.martial.GoldenBell.FINISH_HIT_TICK;
                default -> FINISH_HIT_TICK;
            };
        }
    }

    private KnockdownService() {
    }

    // ---------------------------------------------------------------- state

    public static KnockdownData data(Player player) {
        return player.getData(KnockdownAttachments.PLAYER_KNOCKDOWN);
    }

    public static boolean isDowned(Player player) {
        return data(player).downed();
    }

    /** The player carrying this one, if any. */
    @Nullable public static Player carrier(Player player) {
        return isDowned(player) && player.getVehicle() instanceof Player carrier ? carrier : null;
    }

    public static boolean isCarried(Player player) {
        return carrier(player) != null;
    }

    /** The knocked-down player this one carries, if any. */
    @Nullable public static Player carried(Player player) {
        for (Entity passenger : player.getPassengers()) if (passenger instanceof Player p && isDowned(p)) return p;
        return null;
    }

    public static boolean isCarrying(Player player) {
        return carried(player) != null;
    }

    public static boolean isExecuting(Player player) {
        return EXECUTIONS.containsKey(player.getUUID());
    }

    public static boolean isBeingExecuted(Player player) {
        for (Execution e : EXECUTIONS.values()) if (e.victim.equals(player.getUUID())) return true;
        return false;
    }

    /**
     * Whether the player may fight, use items or skills: not while down, carried, carrying or finishing someone, nor
     * while the Crab King holds them in his pincer.
     */
    public static boolean isHindered(Player player) {
        return isDowned(player) || isCarrying(player) || isExecuting(player) || com.murimblock.mob.CrabKingEntity.isHeld(player);
    }

    // ---------------------------------------------------------------- knock-down and recovery

    /** Damage that would kill this player knocks them down instead: lethal, from another player, not a finish. */
    public static boolean knocksDown(Player victim, DamageSource source) {
        return !isDowned(victim) && !source.is(EXECUTION) && source.getEntity() instanceof Player attacker && attacker != victim;
    }

    public static void knockDown(ServerPlayer player) {
        player.setHealth(1.0F);
        player.setData(KnockdownAttachments.PLAYER_KNOCKDOWN, KnockdownData.downedFor(DOWNED_TICKS));
        MeditationService.stop(player, false);
        player.stopUsingItem();
        player.setSprinting(false);
        // Drops whoever they carried (allowed dismount), gets off any mount, and stops any finish they were doing.
        if (isCarrying(player)) release(player);
        if (player.isPassenger()) player.stopRiding();
        EXECUTIONS.remove(player.getUUID());
        applyModifiers(player);
        player.displayClientMessage(Component.translatable("knockdown.murimblock.downed"), true);
    }

    public static void recover(ServerPlayer player) {
        if (!isDowned(player)) return;
        Player carrier = carrier(player);
        player.setData(KnockdownAttachments.PLAYER_KNOCKDOWN, KnockdownData.STANDING);
        if (carrier != null) {
            RELEASING.add(player.getUUID());
            player.stopRiding();
            RELEASING.remove(player.getUUID());
            if (carrier instanceof ServerPlayer c) syncPassengers(c);
        }
        player.setForcedPose(null);
        removeModifiers(player);
        if (carrier instanceof ServerPlayer c) removeModifiers(c);
        player.setHealth(Math.max(player.getHealth(), player.getMaxHealth() * RECOVER_HEALTH));
        player.displayClientMessage(Component.translatable("knockdown.murimblock.recovered"), true);
    }

    /** Keeps the knock-down applied (pose, slowness), counts it down, and settles carries and finishes. */
    public static void tick(ServerPlayer player) {
        KnockdownData data = data(player);
        if (data.downed()) {
            // Swept into the sky by Scarlet Bloom, the body is shown in its own animation, not lying down.
            player.setForcedPose(com.murimblock.martial.ScarletBloom.sweeps(player) ? null : Pose.SWIMMING);
            applyModifiers(player);
            if (isBeingExecuted(player)) add(player.getAttribute(Attributes.MOVEMENT_SPEED), HELD, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            else remove(player.getAttribute(Attributes.MOVEMENT_SPEED), HELD);
            if (!isCarried(player) && !isBeingExecuted(player)) {
                int left = data.ticksLeft() - 1;
                if (left <= 0) recover(player);
                else player.setData(KnockdownAttachments.PLAYER_KNOCKDOWN, KnockdownData.downedFor(left));
            }
        } else if (player.getForcedPose() == Pose.SWIMMING) {
            player.setForcedPose(null);
            removeModifiers(player);
        }
        if (isCarrying(player)) applyCarrySlowness(player);
        else removeCarrySlowness(player);
    }

    // ---------------------------------------------------------------- carrying

    /** Validates and starts a carry: the carrier stands next to a knocked-down player nobody carries. */
    public static boolean carry(ServerPlayer carrier, ServerPlayer target) {
        if (!canActOn(carrier, target) || isCarried(target)) return false;
        if (!target.startRiding(carrier, true)) return false;
        applyCarrySlowness(carrier);
        syncPassengers(carrier);
        return true;
    }

    /** Puts the carried player down in front of the carrier. */
    public static boolean release(ServerPlayer carrier) {
        Player target = carried(carrier);
        if (target == null) return false;
        RELEASING.add(target.getUUID());
        target.stopRiding();
        RELEASING.remove(target.getUUID());
        Vec3 front = carrier.position().add(Vec3.directionFromRotation(0, carrier.getYRot()).scale(1.0));
        target.teleportTo(front.x, carrier.getY(), front.z);
        removeCarrySlowness(carrier);
        syncPassengers(carrier);
        return true;
    }

    /** A carried player may only leave their carrier when the carrier puts them down. */
    public static boolean mayDismount(Player passenger) {
        return !isCarried(passenger) || RELEASING.contains(passenger.getUUID());
    }

    // ---------------------------------------------------------------- finishing

    /**
     * Validates and starts a finish on a knocked-down player within reach: the finish of the executor's main style
     * (Last Rain Over the Clouds for the Azure Cloud Sword Art, a sword in hand; Final Palm for the Long Fist, bare
     * hands), or the generic one.
     */
    public static boolean execute(ServerPlayer executor, ServerPlayer target) {
        if (!canActOn(executor, target) || isCarried(target) || isBeingExecuted(target)) return false;
        int finish = com.murimblock.martial.ScarletBloom.finishes(executor) ? FinishCinematicPayload.SCARLET_BLOOM
                : AzureCloudSword.finishes(executor) ? FinishCinematicPayload.AZURE_CLOUD
                : LongFist.finishes(executor) ? FinishCinematicPayload.LONG_FIST
                : com.murimblock.martial.MountainShatteringFist.finishes(executor) ? FinishCinematicPayload.MOUNTAIN_BURIAL
                : com.murimblock.martial.SevenStarsMoves.finishes(executor) ? FinishCinematicPayload.SEVEN_STARS
                : com.murimblock.martial.ThunderSaberMoves.finishes(executor) ? FinishCinematicPayload.THUNDER_SABER
                : com.murimblock.martial.GoldenBell.finishes(executor) ? FinishCinematicPayload.LAST_BELL : FinishCinematicPayload.GENERIC;
        EXECUTIONS.put(executor.getUUID(), new Execution(target.getUUID(), executor.server.getTickCount(), finish));
        PlayerPatch<?> patch = EpicFightBridge.patch(executor);
        if (patch != null) patch.playAnimationSynchronized(switch (finish) {
            case FinishCinematicPayload.AZURE_CLOUD -> MurimAnimations.FINISH_AZURE;
            case FinishCinematicPayload.LONG_FIST -> MurimAnimations.FINISH_LONG_FIST;
            case FinishCinematicPayload.SCARLET_BLOOM -> MurimAnimations.FINISH_SCARLET_BLOOM;
            case FinishCinematicPayload.MOUNTAIN_BURIAL -> MurimAnimations.FINISH_MOUNTAIN_BURIAL;
            case FinishCinematicPayload.SEVEN_STARS -> MurimAnimations.FINISH_SEVEN_STARS;
            case FinishCinematicPayload.THUNDER_SABER -> MurimAnimations.FINISH_THUNDER_SABER;
            case FinishCinematicPayload.LAST_BELL -> MurimAnimations.FINISH_LAST_BELL;
            default -> MurimAnimations.FINISH_GENERIC;
        }, 0.0F);
        if (finish == FinishCinematicPayload.SCARLET_BLOOM) com.murimblock.martial.ScarletBloom.start(executor, target);
        cinematic(true, executor, target, finish);
        return true;
    }

    /** Lands or abandons the finishes in progress. Called every server tick. */
    public static void tickExecutions(net.minecraft.server.MinecraftServer server) {
        // A copy: the blow kills, and a death clears the finishes the victim was part of.
        for (Map.Entry<UUID, Execution> entry : List.copyOf(EXECUTIONS.entrySet())) {
            ServerPlayer executor = server.getPlayerList().getPlayer(entry.getKey());
            ServerPlayer victim = server.getPlayerList().getPlayer(entry.getValue().victim);
            // Scarlet Bloom throws the body into the sky: only how far it is across the ground counts.
            double apart = executor == null || victim == null ? 0 : entry.getValue().finish == FinishCinematicPayload.SCARLET_BLOOM
                    ? Math.hypot(executor.getX() - victim.getX(), executor.getZ() - victim.getZ()) : executor.distanceTo(victim);
            if (executor == null || victim == null || !executor.isAlive() || !isDowned(victim) || apart > ACTION_REACH + 1) {
                EXECUTIONS.remove(entry.getKey());
                com.murimblock.martial.ScarletBloom.abort(entry.getKey(), server);
                continue;
            }
            int elapsed = server.getTickCount() - entry.getValue().started;
            if (entry.getValue().finish == FinishCinematicPayload.AZURE_CLOUD) AzureCloudSword.finishTick(executor, victim, elapsed);
            if (entry.getValue().finish == FinishCinematicPayload.LONG_FIST) LongFist.finishTick(executor, victim, elapsed);
            if (entry.getValue().finish == FinishCinematicPayload.MOUNTAIN_BURIAL) com.murimblock.martial.MountainShatteringFist.finishTick(executor, victim, elapsed);
            if (entry.getValue().finish == FinishCinematicPayload.SEVEN_STARS) com.murimblock.martial.SevenStarsMoves.finishTick(executor, victim, elapsed);
            if (entry.getValue().finish == FinishCinematicPayload.THUNDER_SABER) com.murimblock.martial.ThunderSaberMoves.finishTick(executor, victim, elapsed);
            if (entry.getValue().finish == FinishCinematicPayload.LAST_BELL) com.murimblock.martial.GoldenBell.finishTick(executor, victim, elapsed);
            if (elapsed < entry.getValue().hitTick()) continue;
            // The blow lands: the finish is over (the animation plays out on its own).
            EXECUTIONS.remove(entry.getKey());
            finish(executor, victim);
        }
    }

    /**
     * The blow of a finish kills outright: recorded as the cause of death ("executed by"), but not dealt as a hit, so
     * the body is not thrown into a hurt reaction and dies where it lies.
     */
    static void finish(ServerPlayer executor, ServerPlayer victim) {
        DamageSource source = new DamageSource(executor.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(EXECUTION), executor);
        victim.getCombatTracker().recordDamage(source, victim.getHealth());
        victim.setLastHurtByPlayer(executor);
        victim.setHealth(0.0F);
        victim.die(source);
    }

    /** A death by execution at once (the crescent's slam landing on a knocked-down player): as a finish's blow. */
    public static void executeNow(ServerPlayer executor, ServerPlayer victim) {
        finish(executor, victim);
    }

    /**
     * Damage as an execution: a blow that would knock a player down kills them instead (an execution never knocks down,
     * {@link #knocksDown}).
     */
    public static DamageSource execution(ServerPlayer executor) {
        return new DamageSource(executor.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(EXECUTION), executor);
    }

    /** A hit on the executor breaks the finish before the blow lands. */
    public static void interrupt(Player executor) {
        Execution e = EXECUTIONS.get(executor.getUUID());
        if (e != null && executor instanceof ServerPlayer ex && ex.server.getTickCount() - e.started < e.hitTick()) {
            EXECUTIONS.remove(executor.getUUID());
            com.murimblock.martial.ScarletBloom.abort(executor.getUUID(), ex.server);
            ServerPlayer victim = ex.server.getPlayerList().getPlayer(e.victim);
            if (victim != null) cinematic(false, ex, victim);
        }
    }

    private static void cinematic(boolean start, ServerPlayer executor, ServerPlayer victim) {
        cinematic(start, executor, victim, FinishCinematicPayload.GENERIC);
    }

    /** Starts or breaks off the cinematic camera of a finish, for its two players only. */
    private static void cinematic(boolean start, ServerPlayer executor, ServerPlayer victim, int finish) {
        FinishCinematicPayload payload = new FinishCinematicPayload(start, executor.getId(), victim.getId(), finish);
        for (ServerPlayer p : new ServerPlayer[]{executor, victim}) if (p.connection != null) PacketDistributor.sendToPlayer(p, payload);
    }

    /** Forgets everything about a player leaving the game or dying: carries and finishes end. */
    public static void forget(ServerPlayer player) {
        // A finish broken off by a departure: the other player's film stops too (a finish that lands is not broken off).
        Execution own = EXECUTIONS.remove(player.getUUID());
        if (own != null && player.server.getPlayerList().getPlayer(own.victim) instanceof ServerPlayer v && isDowned(v)) cinematic(false, player, v);
        if (own != null) com.murimblock.martial.ScarletBloom.abort(player.getUUID(), player.server);
        EXECUTIONS.entrySet().removeIf(entry -> {
            if (!entry.getValue().victim.equals(player.getUUID())) return false;
            ServerPlayer ex = player.server.getPlayerList().getPlayer(entry.getKey());
            if (ex != null && !player.isDeadOrDying()) cinematic(false, ex, player);
            if (!player.isDeadOrDying()) com.murimblock.martial.ScarletBloom.abort(entry.getKey(), player.server);
            return true;
        });
        if (isCarrying(player)) release(player);
        Player carrier = carrier(player);
        if (carrier != null) {
            RELEASING.add(player.getUUID());
            player.stopRiding();
            RELEASING.remove(player.getUUID());
            if (carrier instanceof ServerPlayer c) syncPassengers(c);
        }
    }

    /**
     * Minecraft tells the players who see a vehicle about its passengers, but not the vehicle itself when it is a
     * player: the carrier's own client must be told whom it carries (for its carrying animation and the prompt).
     */
    private static void syncPassengers(ServerPlayer carrier) {
        if (carrier.connection != null) carrier.connection.send(new ClientboundSetPassengersPacket(carrier));
    }

    /** The actor stands, free, within reach and sight of a knocked-down player. */
    static boolean canActOn(ServerPlayer actor, ServerPlayer target) {
        return actor != target && actor.isAlive() && target.isAlive() && !actor.isSpectator()
                && !isHindered(actor) && actor.getVehicle() == null && isDowned(target)
                && actor.level() == target.level() && actor.distanceTo(target) <= ACTION_REACH && actor.hasLineOfSight(target);
    }

    /** For tests: knocks down for a given time. */
    public static void knockDownFor(ServerPlayer player, int ticks) {
        knockDown(player);
        player.setData(KnockdownAttachments.PLAYER_KNOCKDOWN, KnockdownData.downedFor(ticks));
    }

    public static void clearForTests() {
        EXECUTIONS.clear();
        RELEASING.clear();
    }

    // ---------------------------------------------------------------- movement

    private static void applyModifiers(Player player) {
        add(player.getAttribute(Attributes.MOVEMENT_SPEED), CRAWL_SLOW, -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(player.getAttribute(Attributes.JUMP_STRENGTH), NO_JUMP, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void removeModifiers(Player player) {
        remove(player.getAttribute(Attributes.MOVEMENT_SPEED), CRAWL_SLOW);
        remove(player.getAttribute(Attributes.JUMP_STRENGTH), NO_JUMP);
        remove(player.getAttribute(Attributes.MOVEMENT_SPEED), HELD);
    }

    private static void applyCarrySlowness(Player player) {
        add(player.getAttribute(Attributes.MOVEMENT_SPEED), CARRY_SLOW, -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void removeCarrySlowness(Player player) {
        remove(player.getAttribute(Attributes.MOVEMENT_SPEED), CARRY_SLOW);
    }

    private static void add(@Nullable AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        if (attribute != null && !attribute.hasModifier(id)) attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
    }

    private static void remove(@Nullable AttributeInstance attribute, ResourceLocation id) {
        if (attribute != null) attribute.removeModifier(id);
    }
}
