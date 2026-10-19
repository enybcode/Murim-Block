package com.murimblock.cultivation.inner;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.CultivationAttachments;
import com.murimblock.cultivation.CultivationData;
import com.murimblock.cultivation.CultivationEffects;
import com.murimblock.cultivation.CultivationRules;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialService;
import com.murimblock.mob.MurimEntities;
import com.murimblock.network.CultivationPayloads;
import com.murimblock.qi.QiService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The inner world (docs/cultivation/qi-cultivation.md): seated in meditation with at least 80% Qi, a cultivator holding
 * the meditation key leaves their body in the lotus and opens their eyes on their own island, built to their image
 * ({@link InnerIsland}). At the bottleneck of a realm, the remnant soul offers them the trial (SoulDialogue): their
 * Heart Demon, a double with all their strength, rises before them; slain, the great breakthrough happens when they
 * wake; their own defeat (no death here), fleeing it, or a blow on their body left behind, and the Qi deviates. Otherwise the remnant soul of an old master gives them the day's missions ({@link InnerMissions}). Holding
 * the key again (or falling off the island) brings them back to their body.
 */
public final class InnerWorldService {
    public static final ResourceKey<Level> INNER_WORLD = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "inner_world"));

    /** How one leaves the inner world. */
    public enum Exit { WOKE, VICTORY, DEFEAT, BODY_HIT, FELL, LOGIN }

    private record Pending(Exit exit, long at) { }

    /** Exits to carry out on the player's next ticks (never inside a damage event). */
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    /** Trials accepted: when the Heart Demon rises (game time). */
    private static final Map<UUID, Long> TRIALS = new HashMap<>();
    /** How long the Heart Demon takes to rise once the trial is accepted (ticks). */
    public static final int TRIAL_RISE_TICKS = 60;

    private InnerWorldService() {
    }

    /**
     * The server of GameTests loads no datapack dimension: its tests use another level instead (far from anything), as
     * if it were the inner world.
     */
    @javax.annotation.Nullable private static ServerLevel standIn;
    private static int standInZ;

    public static void useStandInForTests(@javax.annotation.Nullable ServerLevel level, int z) {
        standIn = level;
        standInZ = z;
    }

    @javax.annotation.Nullable static ServerLevel innerLevel(MinecraftServer server) {
        ServerLevel level = server.getLevel(INNER_WORLD);
        return level != null ? level : standIn;
    }

    public static boolean isInnerWorld(Level level) {
        return level.dimension() == INNER_WORLD || (standIn != null && level == standIn);
    }

    public static boolean isInside(Player player) {
        return isInnerWorld(player.level());
    }

    public static InnerSession session(Player player) {
        return player.getData(CultivationAttachments.PLAYER_INNER_SESSION);
    }

    static void setSession(ServerPlayer player, InnerSession session) {
        player.setData(CultivationAttachments.PLAYER_INNER_SESSION, session);
    }

    /** Whether this body entity is the one this player left for their current journey. */
    public static boolean isBodyOf(ServerPlayer player, UUID body) {
        InnerSession s = session(player);
        return s.active() && s.body().filter(body::equals).isPresent();
    }

    // ---------------------------------------------------------------- the island

    /** What the player's island is built from now. */
    static InnerIsland.Look lookOf(ServerPlayer player) {
        MartialData martial = MartialService.data(player);
        return new InnerIsland.Look(CultivationService.getRealm(player).ordinal(), martial.mainArt(), List.copyOf(martial.arts()),
                CultivationService.journal(player).failures());
    }

    static BlockPos centreOf(ServerPlayer player) {
        int slot = InnerWorldData.get(player.server.overworld()).slot(player.getUUID());
        return new BlockPos(slot * InnerWorldData.SPACING, InnerIsland.GROUND, player.server.getLevel(INNER_WORLD) == null ? standInZ : 0);
    }

    /** Where things stand on the player's island (as last built). */
    public static InnerIsland.Layout layoutOf(ServerPlayer player) {
        return InnerIsland.layout(centreOf(player), lookOf(player).radius());
    }

    /** Builds (or rebuilds, if the cultivator changed) the player's island, and puts the remnant soul on it. */
    public static InnerIsland.Layout island(ServerPlayer player, ServerLevel inner) {
        InnerWorldData data = InnerWorldData.get(player.server.overworld());
        BlockPos centre = centreOf(player);
        InnerIsland.Look look = lookOf(player);
        InnerIsland.Layout layout;
        boolean rebuilt = !look.signature().equals(data.look(player.getUUID())) || inner.getBlockState(centre).isAir();
        if (rebuilt) {
            layout = InnerIsland.build(inner, centre, look, data.radius(player.getUUID()));
            data.built(player.getUUID(), look.signature(), layout.radius());
        } else {
            layout = InnerIsland.layout(centre, look.radius());
        }
        // One remnant soul per island, remembered by id: its chunk may not be in view yet when its owner arrives, so a
        // new one is made only for a new (or rebuilt) island; any other soul there takes itself away (RemnantSoulEntity).
        if (rebuilt || data.soul(player.getUUID()) == null) {
            RemnantSoulEntity soul = MurimEntities.REMNANT_SOUL.get().create(inner);
            if (soul != null) {
                BlockPos at = layout.soul();
                soul.moveTo(at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, -135, 0);
                soul.setOwner(player.getUUID());
                inner.addFreshEntity(soul);
                data.soul(player.getUUID(), soul.getUUID());
                // The old soul of a rebuilt island, if already in view, goes at once (otherwise on its first tick).
                AABB area = new AABB(centre).inflate(layout.radius() + 40, 60, layout.radius() + 40);
                for (RemnantSoulEntity old : inner.getEntitiesOfClass(RemnantSoulEntity.class, area, s -> s != soul)) old.discard();
            }
        }
        return layout;
    }

    // ---------------------------------------------------------------- going in

    /** The share of the Qi reserve one must hold to open the inner world (Enzo, 18/10). */
    public static final double ENTRY_QI_SHARE = 0.8;

    /** Into the inner world, from meditation with at least {@link #ENTRY_QI_SHARE} of the reserve; at a bottleneck, the old master waits with the trial. */
    public static boolean enter(ServerPlayer player) {
        if (isInside(player) || !MeditationService.isMeditating(player)) return false;
        ServerLevel inner = innerLevel(player.server);
        if (inner == null) return false;
        if (QiService.getQi(player) < QiService.getQiMax(player) * ENTRY_QI_SHARE - 1e-6) {
            player.displayClientMessage(Component.translatable("cultivation.murimblock.inner.reserve"), true);
            return false;
        }
        CultivationData data = CultivationService.getCultivation(player);
        boolean breakthrough = CultivationRules.atBottleneck(data, QiService.getQiMax(player));
        double strength = CultivationService.demonStrength(player, CultivationService.placeOf(player));
        MeditatingBodyEntity body = MurimEntities.MEDITATING_BODY.get().create(player.serverLevel());
        if (body == null) return false;
        body.setup(player);
        player.serverLevel().addFreshEntity(body);
        setSession(player, new InnerSession(true, player.level().dimension().location(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), Optional.of(body.getUUID()), breakthrough ? InnerSession.BREAKTHROUGH : InnerSession.VISIT, strength));
        CultivationEffects.show(body, CultivationPayloads.Breakthrough.ENTER);
        CultivationEffects.show(player, CultivationPayloads.Breakthrough.ENTER);
        MeditationService.forget(player);
        InnerIsland.Layout layout = island(player, inner);
        BlockPos arrival = layout.arrival();
        player.teleportTo(inner, arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5, InnerIsland.arrivalYaw(), 0);
        player.resetFallDistance();
        inner.playSound(null, arrival, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.25F, 1.6F);
        if (breakthrough) {
            player.sendSystemMessage(Component.translatable("cultivation.murimblock.inner.gate"));
        } else {
            player.sendSystemMessage(Component.translatable("cultivation.murimblock.inner.arrived"));
        }
        return true;
    }

    /**
     * The trial accepted (at a bottleneck, from the old master): the Heart Demon rises a few seconds later, a double with
     * all its owner's strength. Fleeing it once it stands, or falling to it, and the Qi deviates.
     */
    public static boolean beginTrial(ServerPlayer player) {
        if (!isInside(player) || !session(player).breakthrough() || trialUnderway(player)) return false;
        ServerLevel inner = player.serverLevel();
        BlockPos at = layoutOf(player).demon();
        TRIALS.put(player.getUUID(), inner.getGameTime() + TRIAL_RISE_TICKS);
        inner.playSound(null, at, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), SoundSource.HOSTILE, 1.0F, 0.7F);
        return true;
    }

    /** Whether the trial was accepted and its demon not yet slain (rising, or standing). */
    public static boolean trialUnderway(ServerPlayer player) {
        if (TRIALS.containsKey(player.getUUID())) return true;
        return isInside(player) && !player.serverLevel().getEntitiesOfClass(HeartDemonEntity.class, player.getBoundingBox().inflate(80, 60, 80),
                d -> !d.shadow() && d.isAlive() && player.getUUID().equals(d.owner())).isEmpty();
    }

    /** While it rises: dark Qi gathering where it will stand; then the demon itself. */
    private static void tickTrial(ServerPlayer player) {
        Long at = TRIALS.get(player.getUUID());
        if (at == null) return;
        ServerLevel inner = player.serverLevel();
        BlockPos spot = layoutOf(player).demon();
        long left = at - inner.getGameTime();
        if (left > 0) {
            double r = 0.4 + left / (double) TRIAL_RISE_TICKS * 1.6;
            for (int i = 0; i < 6; i++) {
                double a = inner.random.nextDouble() * Math.PI * 2;
                inner.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, spot.getX() + 0.5 + Math.cos(a) * r, spot.getY() + 0.2 + inner.random.nextDouble() * 1.8,
                        spot.getZ() + 0.5 + Math.sin(a) * r, 0, -Math.cos(a), 0.1, -Math.sin(a), 0.08);
            }
            return;
        }
        TRIALS.remove(player.getUUID());
        HeartDemonEntity demon = summonDemon(player, inner, layoutOf(player), false);
        if (demon == null) return;
        inner.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, spot.getX() + 0.5, spot.getY() + 1, spot.getZ() + 0.5, 40, 0.5, 0.9, 0.5, 0.02);
        player.sendSystemMessage(Component.translatable("cultivation.murimblock.inner.demon", Math.round(session(player).strength() * 100)));
    }

    /**
     * A blow of the player's own Heart Demon lands as hard as their preparation leaves it (the session's weight, 40 % to
     * 130 %): the demon has all their strength, but a cultivator who prepared well is shaken less.
     */
    public static float demonBlow(ServerPlayer player, DamageSource source, float amount) {
        if (!isInside(player) || !(source.getEntity() instanceof HeartDemonEntity demon) || demon.shadow() || !player.getUUID().equals(demon.owner())) {
            return amount;
        }
        return (float) (amount * session(player).strength());
    }

    /** How rich in Qi the place where the player's body sits (where they are, outside). */
    public static CultivationRules.Place bodyPlace(ServerPlayer player) {
        InnerSession s = session(player);
        if (!isInside(player) || !s.active()) return CultivationService.placeOf(player);
        ServerLevel level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, s.dimension()));
        if (level == null) return CultivationRules.Place.ORDINARY;
        return CultivationService.placeAt(level, BlockPos.containing(s.x(), s.y(), s.z()), s.y());
    }

    static HeartDemonEntity summonDemon(ServerPlayer player, ServerLevel inner, InnerIsland.Layout layout, boolean shadow) {
        HeartDemonEntity demon = MurimEntities.HEART_DEMON.get().create(inner);
        if (demon == null) return null;
        BlockPos at = layout.demon();
        demon.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        demon.setYHeadRot(0);
        demon.setup(player, shadow);
        // Its spot loaded (a player's view keeps it so; not always one made for a test).
        inner.getChunkAt(at);
        if (!inner.addFreshEntity(demon)) return null;
        inner.playSound(null, at, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.5F, 1.4F);
        return demon;
    }

    // ---------------------------------------------------------------- coming back

    /** Carries out an exit on the player's next tick (a blow, a fall, a victory are not handled in the middle of an event). */
    static void schedule(ServerPlayer player, Exit exit, int delay) {
        Pending now = PENDING.get(player.getUUID());
        if (now != null && now.exit != Exit.WOKE) return;
        PENDING.put(player.getUUID(), new Pending(exit, player.level().getGameTime() + delay));
    }

    /** Back to the body: the island's creatures gone, the body taken up again, and what the journey brought. */
    public static void leave(ServerPlayer player, Exit exit) {
        PENDING.remove(player.getUUID());
        InnerSession s = session(player);
        boolean demonAlive = clearIsland(player);
        InnerMissions.end(player);
        com.murimblock.dialogue.DialogueService.forget(player);
        if (!s.active()) {
            if (isInside(player)) sendHome(player);
            return;
        }
        setSession(player, InnerSession.NONE);
        MinecraftServer server = player.server;
        ServerLevel back = server.getLevel(ResourceKey.create(Registries.DIMENSION, s.dimension()));
        if (back == null) back = server.overworld();
        player.teleportTo(back, s.x(), s.y(), s.z(), s.yaw(), s.pitch());
        player.resetFallDistance();
        player.setDeltaMovement(0, 0, 0);
        player.hurtMarked = true;
        // Back where the body sat, on the ground (the flag would only come with the next move).
        player.setOnGround(true);
        if (player.getHealth() < 4) player.setHealth(4);
        ServerLevel bodyLevel = back;
        s.body().ifPresent(id -> {
            Entity body = bodyLevel.getEntity(id);
            if (body != null) body.discard();
        });
        CultivationEffects.show(player, CultivationPayloads.Breakthrough.RETURN);
        boolean failed = switch (exit) {
            case DEFEAT, BODY_HIT -> true;
            case WOKE, FELL -> s.breakthrough() && demonAlive;
            default -> false;
        };
        if (exit == Exit.VICTORY) {
            MeditationService.resume(player);
            CultivationService.greatBreakthrough(player);
        } else if (failed) {
            if (exit == Exit.BODY_HIT) player.sendSystemMessage(Component.translatable("cultivation.murimblock.inner.torn_out"));
            CultivationService.deviation(player);
        } else if (exit != Exit.LOGIN) {
            MeditationService.resume(player);
        }
    }

    /** A player found in the inner world with no journey (a command, a teleport): sent to the world's spawn. */
    static void sendHome(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawn.getX() + 0.5, overworld.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ()),
                spawn.getZ() + 0.5, player.getYRot(), 0);
    }

    /** Takes the player's demons and motes off their island; true if a Heart Demon (not a shadow) was still standing. */
    static boolean clearIsland(ServerPlayer player) {
        TRIALS.remove(player.getUUID());
        if (!isInside(player)) return false;
        boolean alive = false;
        AABB area = player.getBoundingBox().inflate(80, 60, 80);
        List<Entity> gone = new ArrayList<>();
        for (HeartDemonEntity demon : player.serverLevel().getEntitiesOfClass(HeartDemonEntity.class, area, d -> player.getUUID().equals(d.owner()))) {
            if (!demon.shadow() && demon.isAlive()) alive = true;
            gone.add(demon);
        }
        gone.addAll(player.serverLevel().getEntitiesOfClass(QiMoteEntity.class, area, m -> player.getUUID().equals(m.owner())));
        gone.forEach(Entity::discard);
        return alive;
    }

    // ---------------------------------------------------------------- what happens inside

    /** The Heart Demon slain: a moment of victory, then the cultivator wakes into the great breakthrough. */
    public static void demonSlain(ServerPlayer player, HeartDemonEntity demon) {
        if (demon.shadow()) {
            InnerMissions.shadowDefeated(player);
            return;
        }
        if (!session(player).breakthrough()) return;
        player.sendSystemMessage(Component.translatable("cultivation.murimblock.inner.demon_slain"));
        player.level().playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0F, 0.8F);
        schedule(player, Exit.VICTORY, 40);
    }

    /** The body left behind took a blow: the cultivator is torn back out. */
    public static void bodyHit(ServerPlayer player) {
        if (isInside(player) && session(player).active()) schedule(player, Exit.BODY_HIT, 0);
    }

    /**
     * Damage taken in the inner world: no fall damage, no falling out of the world, no death (a lethal blow is a defeat,
     * a shadow's only ends the sparring). True to cancel it.
     */
    public static boolean onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
        if (!isInside(player)) return false;
        if (source.is(DamageTypeTags.IS_FALL)) return true;
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            schedule(player, Exit.FELL, 0);
            return true;
        }
        if (amount < player.getHealth()) return false;
        if (InnerMissions.sparring(player)) {
            InnerMissions.sparringLost(player);
        } else {
            schedule(player, Exit.DEFEAT, 0);
        }
        player.setHealth(Math.max(1, player.getHealth()));
        return true;
    }

    public static void tick(ServerPlayer player) {
        Pending pending = PENDING.get(player.getUUID());
        if (pending != null && player.level().getGameTime() >= pending.at) {
            leave(player, pending.exit);
            return;
        }
        if (!isInside(player)) return;
        if (!session(player).active()) {
            if (player.tickCount % 20 == 0) sendHome(player);
            return;
        }
        if (player.getY() < InnerIsland.GROUND - 30) schedule(player, Exit.FELL, 0);
        tickTrial(player);
        InnerMissions.tick(player);
    }

    public static void serverTick(MinecraftServer server) {
        // Exits are carried out from the players' own ticks; nothing to do between them.
    }

    /** Logged in inside (their journey interrupted): back to the body, without a penalty. */
    public static void onLogin(ServerPlayer player) {
        if (isInside(player)) leave(player, Exit.LOGIN);
        else if (session(player).active()) {
            // Back home already (a crash between the two): forget the journey, the body takes itself away.
            setSession(player, InnerSession.NONE);
        }
    }

    public static void onLogout(ServerPlayer player) {
        PENDING.remove(player.getUUID());
        TRIALS.remove(player.getUUID());
        InnerMissions.end(player);
        com.murimblock.dialogue.DialogueService.forget(player);
    }

    public static void clearForTests() {
        PENDING.clear();
        TRIALS.clear();
        com.murimblock.dialogue.DialogueService.clearForTests();
    }
}
