package com.murimblock.combat;

import com.murimblock.Murimblock;
import com.murimblock.client.combat.CombatClient;
import com.murimblock.cultivation.Cultivation;
import com.murimblock.registry.MurimAttachments;
import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.AttackData;
import com.murimblock.shenfa.ShenfaLibrary;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The fight on the server, which decides it (Epic Fight's model, with Shenfa's moves): players' inputs start moves,
 * each body's move goes on tick by tick, a strike's colliders sweep its hit window and each body they cross is hit
 * once per phase; a guard facing the blow blocks it for Qi or parries it if just raised; a dodge's window lets blows
 * through; a blow staggers by its impact against the target's stun armor and freezes both bodies a few ticks. Mobs
 * fight as vanilla made them (they are not on Shenfa); our blows stagger them and their blows meet our guard.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class CombatServer {
    private static final String PROTOCOL = "2";
    private static final Map<LivingEntity, CombatState> STATES = new WeakHashMap<>();
    /** The blow being dealt, for the damage event: who deals it and how hard. */
    private static Entity pendingAttacker;
    private static float pendingImpact = 1;
    /** What the damage event made of the blow. */
    private static CombatPayloads.Result outcome = CombatPayloads.Result.HIT;

    private CombatServer() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(CombatPayloads.Anim.TYPE, CombatPayloads.Anim.STREAM_CODEC,
                (payload, context) -> CombatClient.anim(payload));
        registrar.playToClient(CombatPayloads.Hit.TYPE, CombatPayloads.Hit.STREAM_CODEC,
                (payload, context) -> CombatClient.hit(payload));
        registrar.playToClient(CombatPayloads.Mode.TYPE, CombatPayloads.Mode.STREAM_CODEC,
                (payload, context) -> CombatClient.mode(payload));
        registrar.playToClient(CombatPayloads.Self.TYPE, CombatPayloads.Self.STREAM_CODEC,
                (payload, context) -> CombatClient.self(payload));
        registrar.playToServer(CombatPayloads.Input.TYPE, CombatPayloads.Input.STREAM_CODEC,
                (payload, context) -> input((ServerPlayer) context.player(), payload));
    }

    /** The fight of {@code body} (made on first use). */
    public static CombatState state(LivingEntity body) {
        return STATES.computeIfAbsent(body, key -> new CombatState());
    }

    /** The fight of {@code body}, or null when it never fought. */
    public static CombatState existing(LivingEntity body) {
        return STATES.get(body);
    }

    // Players.

    private static void input(ServerPlayer player, CombatPayloads.Input input) {
        CombatState state = state(player);
        long now = player.level().getGameTime();
        switch (input.action()) {
            case MODE_ON, MODE_OFF -> {
                state.mode = input.action() == CombatPayloads.Action.MODE_ON;
                if (!state.mode && state.guarding) {
                    state.guard(false, now);
                    anim(player, Animator.Priority.MIDDLE, null, true);
                }
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                        new CombatPayloads.Mode(player.getId(), state.mode));
            }
            case GUARD_UP -> {
                if (state.mode && !state.guarding) {
                    state.guard(true, now);
                    anim(player, Animator.Priority.MIDDLE, Moveset.of(player).guard(), true);
                }
            }
            case GUARD_DOWN -> {
                if (state.guarding) {
                    state.guard(false, now);
                    anim(player, Animator.Priority.MIDDLE, null, true);
                }
            }
            case ATTACK, DODGE -> {
                if (state.mode) {
                    CombatState.Input wanted = input.action() == CombatPayloads.Action.DODGE ? CombatState.Input.DODGE
                            : CombatState.Input.ATTACK;
                    begin(player, state, wanted, input.direction(), now);
                    PacketDistributor.sendToPlayer(player, new CombatPayloads.Self(state.clip == null ? ""
                            : state.clip.toString(), state.time, state.stunTicks));
                }
            }
        }
    }

    private static boolean begin(ServerPlayer player, CombatState state, CombatState.Input input, int direction,
                                 long now) {
        Cultivation cultivation = player.getData(MurimAttachments.CULTIVATION);
        CombatMoves.Choice choice = CombatMoves.decide(state, input, direction, player, now, cultivation.qi());
        if (choice == null || choice.qi() > 0 && !player.isCreative() && !cultivation.spend(choice.qi())) {
            return false;
        }
        CombatMoves.begin(state, choice, player);
        // A move starts facing where the player looks.
        player.setYBodyRot(player.getYRot());
        anim(player, Animator.Priority.HIGH, choice.clip(), true);
        return true;
    }

    // Every fighting body, each tick.

    @SubscribeEvent
    public static void tick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity body) || body.level().isClientSide) {
            return;
        }
        CombatState state = STATES.get(body);
        if (state == null) {
            return;
        }
        long now = body.level().getGameTime();
        if (now - state.lastGuarded > 40) {
            state.guardPenalty = 1;
        }
        if (body.onGround()) {
            state.airStrike = false;
        }
        boolean frozen = state.hitStop > 0;
        float before = state.time;
        Animation playing = state.animation;
        state.tick(now);
        if (!frozen && playing != null) {
            float after = state.animation == playing ? state.time : playing.clip().duration();
            if (playing.attack() != null) {
                hits(body, state, playing, before, after);
            }
        }
        if (body instanceof ServerPlayer player && state.buffered != null && state.free()) {
            CombatState.Input input = state.takeBuffered(now);
            if (input != null) {
                begin(player, state, input, state.bufferedDirection, now);
            }
        }
        if (body instanceof Mob mob) {
            steer(mob, state, frozen);
            if (!state.playing() && state.stunTicks == 0 && !state.guarding) {
                STATES.remove(body);
            }
        }
    }

    /** A mob while it reels or is frozen by a blow: it stands. */
    private static void steer(Mob mob, CombatState state, boolean frozen) {
        if (state.stunTicks > 0 || frozen) {
            mob.getNavigation().stop();
        }
        if (frozen) {
            mob.setDeltaMovement(mob.getDeltaMovement().multiply(0, 1, 0));
        }
    }

    /** The strikes whose hit window the move crossed this tick. */
    private static void hits(LivingEntity body, CombatState state, Animation animation, float before, float after) {
        float yaw = body instanceof Player ? body.getYRot() : body.yBodyRot;
        for (AttackData.Phase phase : animation.attack().phases()) {
            float from = Math.max(before, phase.activeFrom()), to = Math.min(after, phase.activeTo());
            if (from > to) {
                continue;
            }
            if (state.phase != phase) {
                state.phase = phase;
                state.hit.clear();
            }
            List<AABB> boxes = Colliders.sweep(body, animation, phase, from, to, yaw);
            AABB around = boxes.get(0);
            for (AABB box : boxes) {
                around = around.minmax(box);
            }
            List<LivingEntity> found = new ArrayList<>(body.level().getEntitiesOfClass(LivingEntity.class,
                    around, other -> other != body && other.isAlive() && !state.hit.contains(other.getId())
                            && targetable(body, other)));
            found.sort((a, b) -> Double.compare(a.distanceToSqr(body), b.distanceToSqr(body)));
            for (LivingEntity target : found) {
                if (state.hit.size() >= phase.maxTargets()) {
                    break;
                }
                if (boxes.stream().anyMatch(box -> box.intersects(target.getBoundingBox())) && clear(body, target)) {
                    state.hit.add(target.getId());
                    strike(body, target, phase, state);
                }
            }
        }
    }

    /** Who a body may hit: anything but itself, its pets and its allies. */
    private static boolean targetable(LivingEntity body, LivingEntity other) {
        if (other instanceof OwnableEntity pet && pet.getOwner() == body) {
            return false;
        }
        return !body.isAlliedTo(other) && !other.isSpectator() && other.isPickable();
    }

    /** No wall between the attacker's chest and the target's middle. */
    private static boolean clear(LivingEntity body, LivingEntity target) {
        Vec3 from = body.position().add(0, body.getBbHeight() * 0.6, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
        return body.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, body))
                .getType() == HitResult.Type.MISS;
    }

    /** A blow of {@code phase} reaching {@code target}. */
    private static void strike(LivingEntity attacker, LivingEntity target, AttackData.Phase phase, CombatState state) {
        CombatState struck = state(target);
        pendingAttacker = attacker;
        pendingImpact = phase.impact();
        outcome = CombatPayloads.Result.HIT;
        boolean landed;
        try {
            landed = playerBlow(attacker, target, phase);
        } finally {
            pendingAttacker = null;
            pendingImpact = 1;
        }
        CombatPayloads.Result result = outcome;
        if (result == CombatPayloads.Result.HIT && !landed) {
            return;
        }
        int stop = switch (result) {
            case HIT -> Math.round(Mth.clamp(2 + phase.impact() * 1.5f, 2, 6));
            case GUARD -> 3;
            case PARRY, GUARD_BREAK -> 5;
            case DODGED -> 0;
        };
        state.hitStop = Math.max(state.hitStop, stop);
        struck.hitStop = Math.max(struck.hitStop, stop);
        AttackData.Stun stun = AttackData.Stun.NONE;
        if (result == CombatPayloads.Result.HIT && !struck.superArmor()) {
            stun = stagger(target, phase.stun(), phase.impact());
        }
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0)
                .add(attacker.position().subtract(target.position()).normalize().scale(target.getBbWidth() * 0.5));
        hit(attacker, target, result, stun, phase.impact(), phase.kind().equals("cut"), stop, at);
    }

    /** A player's blow: its attack damage (the weapon's) times the strike's, grown with cultivation. */
    private static boolean playerBlow(LivingEntity attacker, LivingEntity target, AttackData.Phase phase) {
        ServerLevel level = (ServerLevel) attacker.level();
        DamageSource source = attacker instanceof Player player ? attacker.damageSources().playerAttack(player)
                : attacker.damageSources().mobAttack(attacker);
        ItemStack weapon = attacker.getMainHandItem();
        float damage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * phase.damage() * cultivation(attacker);
        damage = EnchantmentHelper.modifyDamage(level, weapon, target, source, damage);
        // Our strikes come faster than vanilla's half second of invulnerability after a blow.
        target.invulnerableTime = 0;
        boolean landed = target.hurt(source, damage);
        if (landed) {
            float extra = phase.knockback() - 0.4f;
            if (extra > 0) {
                float yaw = attacker.getYRot() * Mth.DEG_TO_RAD;
                target.knockback(extra, Mth.sin(yaw), -Mth.cos(yaw));
            }
            if (attacker instanceof Player player) {
                weapon.hurtEnemy(target, player);
                weapon.postHurtEnemy(target, player);
                player.setLastHurtMob(target);
                player.causeFoodExhaustion(0.1f);
            }
            EnchantmentHelper.doPostAttackEffects(level, target, source);
        }
        return landed;
    }

    /** How much stronger a cultivator strikes: half again per realm, a twentieth per stage. */
    private static float cultivation(LivingEntity attacker) {
        if (!(attacker instanceof Player player)) {
            return 1;
        }
        Cultivation c = player.getData(MurimAttachments.CULTIVATION);
        return 1 + 0.5f * c.realm() + 0.05f * (c.stage() - 1);
    }

    /**
     * Staggers {@code target} by a blow of {@code impact}: weaker against stun armor (a short stagger instead of a
     * knockdown, none for a golem), longer for a harder blow. The move it played is broken. Returns the stagger.
     */
    public static AttackData.Stun stagger(LivingEntity target, AttackData.Stun stun, float impact) {
        if (stun == AttackData.Stun.NONE) {
            return stun;
        }
        float power = impact / (1 + stunArmor(target));
        if (power < 0.35f) {
            return AttackData.Stun.NONE;
        }
        AttackData.Stun type = stun;
        if ((type == AttackData.Stun.KNOCKDOWN || type == AttackData.Stun.LAUNCH) && power < 1) {
            type = AttackData.Stun.LONG;
        }
        if (type == AttackData.Stun.LONG && power < 0.6f) {
            type = AttackData.Stun.SHORT;
        }
        int ticks = Math.round(switch (type) {
            case SHORT -> 8;
            case LONG -> 18;
            case KNOCKDOWN -> 36;
            case LAUNCH -> 30;
            case NONE -> 0;
        } * Mth.clamp(power, 0.6f, 1.5f));
        CombatState state = state(target);
        state.stun = type;
        state.stunTicks = Math.max(state.stunTicks, ticks);
        if (state.playing() && state.kind != CombatState.Kind.REACTION) {
            state.end();
            anim(target, Animator.Priority.HIGH, null, false);
        }
        if (type == AttackData.Stun.LAUNCH) {
            target.setDeltaMovement(target.getDeltaMovement().add(0, 0.55, 0));
            target.hurtMarked = true;
        }
        if (target instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new CombatPayloads.Self("", 0, state.stunTicks));
        }
        return type;
    }

    /** How hard {@code body} is to stagger: a player's armor weighs (Epic Fight), a mob's size. */
    public static float stunArmor(LivingEntity body) {
        if (body instanceof Player) {
            return body.getArmorValue() / 8f;
        }
        return Math.max(0, body.getBbHeight() * body.getBbWidth() - 1);
    }

    // The damage event: dodges, guards and parries, for our blows and vanilla's alike.

    @SubscribeEvent
    public static void incoming(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        CombatState state = STATES.get(target);
        DamageSource source = event.getSource();
        if (state == null || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        Entity direct = source.getDirectEntity();
        boolean melee = direct instanceof LivingEntity, projectile = direct instanceof Projectile;
        if (!melee && !projectile) {
            return;
        }
        boolean ours = direct == pendingAttacker;
        float impact = ours ? pendingImpact : 1;
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
        if (state.invulnerable()) {
            event.setCanceled(true);
            outcome = CombatPayloads.Result.DODGED;
            if (!ours) {
                hit(direct, target, CombatPayloads.Result.DODGED, AttackData.Stun.NONE, impact, false, 0, at);
            }
            return;
        }
        if (!state.guarding || !facing(target, direct.position())) {
            return;
        }
        long now = target.level().getGameTime();
        if (state.parrying(now)) {
            event.setCanceled(true);
            outcome = CombatPayloads.Result.PARRY;
            if (melee) {
                stagger((LivingEntity) direct, AttackData.Stun.LONG, 2.5f);
            }
            if (!ours) {
                hit(direct, target, CombatPayloads.Result.PARRY, AttackData.Stun.NONE, impact, false, 5, at);
            }
            return;
        }
        float cost = 2 * impact * state.guardPenalty;
        Cultivation cultivation = target instanceof Player player ? player.getData(MurimAttachments.CULTIVATION) : null;
        if (cultivation == null || (target instanceof Player player && player.isCreative())
                || cultivation.spend(cost)) {
            event.setCanceled(true);
            state.guardPenalty += 0.25f;
            state.lastGuarded = now;
            outcome = CombatPayloads.Result.GUARD;
            if (!ours) {
                hit(direct, target, CombatPayloads.Result.GUARD, AttackData.Stun.NONE, impact, false, 3, at);
            }
            return;
        }
        // Out of Qi: the guard breaks, half the blow goes through and the body reels.
        cultivation.drain();
        state.guard(false, now);
        anim(target, Animator.Priority.MIDDLE, null, false);
        event.setAmount(event.getAmount() * 0.5f);
        outcome = CombatPayloads.Result.GUARD_BREAK;
        stagger(target, AttackData.Stun.LONG, 10);
        if (!ours) {
            hit(direct, target, CombatPayloads.Result.GUARD_BREAK, AttackData.Stun.LONG, impact, false, 5, at);
        }
    }

    /** Whether {@code body} faces {@code from} (in front of it, within a half turn). */
    private static boolean facing(LivingEntity body, Vec3 from) {
        Vec3 toward = from.subtract(body.position()).multiply(1, 0, 1);
        Vec3 look = Vec3.directionFromRotation(0, body.getYRot());
        return toward.lengthSqr() < 1e-6 || toward.normalize().dot(look) > 0;
    }

    // Sending.

    /** Plays (or stops, with a null clip) a layer of {@code body} on every client that sees it. */
    public static void anim(LivingEntity body, Animator.Priority layer, ResourceLocation clip, boolean notSelf) {
        CombatPayloads.Anim payload = new CombatPayloads.Anim(body.getId(), layer.ordinal(),
                clip == null ? "" : clip.toString(), 1, false);
        if (notSelf) {
            PacketDistributor.sendToPlayersTrackingEntity(body, payload);
        } else {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(body, payload);
        }
    }

    private static void hit(Entity attacker, LivingEntity target, CombatPayloads.Result result, AttackData.Stun stun,
                            float impact, boolean cut, int hitStop, Vec3 at) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new CombatPayloads.Hit(attacker.getId(),
                target.getId(), result, stun.ordinal(), impact, cut, hitStop, at));
    }

    /** {@code /reload} reads the clips again (their timing and attack data). */
    @SubscribeEvent
    public static void reload(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
        event.addListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager
                -> ShenfaLibrary.reload());
    }

    /** A player who starts seeing another learns its combat mode. */
    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player other && event.getEntity() instanceof ServerPlayer viewer) {
            CombatState state = STATES.get(other);
            if (state != null && state.mode) {
                PacketDistributor.sendToPlayer(viewer, new CombatPayloads.Mode(other.getId(), true));
            }
        }
    }
}
