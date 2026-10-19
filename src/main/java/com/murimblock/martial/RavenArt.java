package com.murimblock.martial;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.integration.epicfight.MurimWeaponCategories;
import com.murimblock.integration.epicfight.SectWeaponAnimations;
import com.murimblock.item.MurimItems;
import com.murimblock.world.raven.RavenHalls;
import net.minecraft.ChatFormatting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The Raven Messenger Art in play (docs/combat/styles/raven-art.md): an iron fan in hand, everything goes through the
 * Raven Halls ({@link RavenHalls}). Its passive, Between Worlds, opens a rift on whatever a fan strike lands on; at three,
 * the next strike tips the body through and sets it behind the player. Its techniques:
 * <ul>
 *   <li><b>Raven Gate</b>: the target falls into a closed room for 4 s, then is dropped from 10 blocks where it was;</li>
 *   <li><b>Halls Step</b>: through a door, out where one looks (16 blocks);</li>
 *   <li><b>Exchange</b>: places swapped with the target; crouching, the target's with the nearest ally's;</li>
 *   <li><b>Swallowing Gate</b>: 3 s of a door before one that swallows the projectiles, then sends them back;</li>
 *   <li><b>Closed Room</b>: a duel of 20 s in a room of the halls; crouching, home to one's own hall (and back);</li>
 *   <li><b>The Halls Unfold</b>, the ultimate: every enemy within 30 blocks scattered into rooms for 30 s.</li>
 * </ul>
 * Its secret, Return to Sender, is found on a perfect parry: the attacker swallowed and set down behind, stunned. Bosses
 * and players of a higher realm cannot be swallowed (their tag, their realm).
 */
public final class RavenArt {
    /** Bodies no door can take (the bosses). */
    public static final TagKey<EntityType<?>> UNSWALLOWABLE = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "raven_unswallowable"));
    static final double GATE_REACH = 12, STEP_REACH = 16, EXCHANGE_REACH = 16, ROOM_REACH = 10, UNFOLD_RADIUS = 30;
    public static final int GATE_TICKS = 80, GATE_DROP = 10, SWALLOW_TICKS = 60, DUEL_TICKS = 400, UNFOLD_TICKS = 600;
    /** Rifts on a body before the next strike tips it through. */
    static final int RIFTS = 3;
    /** A rift closes after this long without another strike. */
    static final int RIFT_TICKS = 100;

    private RavenArt() {
    }

    /** Whether this player fights with Between Worlds: the Raven Art is their main style. */
    public static boolean stanceActive(Player player) {
        if (player.level().isClientSide)
            return com.murimblock.cultivation.MeditationService.aura(player).mainArt().filter(MartialArts.RAVEN_ART::equals).isPresent();
        return MartialService.data(player).mainArt().filter(MartialArts.RAVEN_ART::equals).isPresent();
    }

    /** An iron fan in hand (Murimblock's, or any weapon Epic Fight counts as one). */
    public static boolean holdsFan(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return false;
        if (stack.is(MurimItems.IRON_FAN.get())) return true;
        CapabilityItem cap = EpicFightCapabilities.getItemStackCapability(stack);
        return cap != null && cap.getWeaponCategory() == MurimWeaponCategories.IRON_FAN;
    }

    /** Whether a door can take this body: not a boss, not a player of a higher realm than the student's. */
    public static boolean canSwallow(ServerPlayer player, LivingEntity body) {
        if (!body.isAlive() || body.getType().is(UNSWALLOWABLE) || body.isPassenger() || body.isVehicle()) return false;
        if (body instanceof ServerPlayer p) return CultivationService.getRealm(p).ordinal() <= CultivationService.getRealm(player).ordinal();
        return true;
    }

    // ---------------------------------------------------------------- techniques

    /** The finish's door opens under the fallen (tick), and the fan shuts on them: the blow (tick). */
    public static final int FINISH_OPEN_TICK = ticks(MurimAnimations.RAVEN_MESSAGE_OPEN), FINISH_HIT_TICK = ticks(MurimAnimations.RAVEN_MESSAGE_SHUT);

    static int ticks(float seconds) {
        return Math.round(seconds * 20);
    }

    private static void play(ServerPlayer player, PlayerPatch<?> patch, AnimationAccessor<? extends StaticAnimation> animation, MartialArts.Technique technique) {
        MartialCombat.expect(player, animation, technique.id(), false);
        patch.playAnimationSynchronized(animation, 0.0F);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.5F, 1.6F);
    }

    private static MartialArts.Art art() {
        return MartialArts.art(MartialArts.RAVEN_ART);
    }

    /**
     * Starts one of the art's techniques: what it needs is checked now (a target, a place, the halls), and it costs only
     * when it can be done; its doors open on the fan's gesture.
     */
    static boolean perform(ServerPlayer player, PlayerPatch<?> patch, MartialArts.Technique technique) {
        if (RavenHalls.dueling(player) && technique != MartialArts.SWALLOWING_GATE) return false;
        if (RavenHalls.halls(player.server) == null && technique != MartialArts.HALLS_STEP && technique != MartialArts.EXCHANGE
                && technique != MartialArts.SWALLOWING_GATE) return false;
        if (technique == MartialArts.RAVEN_GATE) {
            if (RavenHalls.isHalls(player.level())) return false;
            LivingEntity target = AzureCloudSword.targetAhead(player, GATE_REACH);
            if (target == null || !canSwallow(player, target)) return false;
            play(player, patch, MurimAnimations.RAVEN_GATE, technique);
            int open = ticks(MurimAnimations.RAVEN_GATE_OPEN);
            MartialCombat.later(player, open, () -> {
                if (target.isAlive()) RavenFx.pit(player.serverLevel(), target.position(), pitRadius(target), 16);
            });
            // It falls in a moment after the door opens under it.
            MartialCombat.later(player, open + 4, () -> {
                if (target.isAlive() && canSwallow(player, target) && RavenHalls.swallow(target, GATE_TICKS, GATE_DROP))
                    MartialProgress.addMastery(player, art(), MartialArts.RAVEN_GATE.id(), 1);
            });
            return true;
        }
        if (technique == MartialArts.HALLS_STEP) {
            if (RavenHalls.isHalls(player.level())) return false;
            Vec3 to = stepTarget(player);
            if (to == null) return false;
            play(player, patch, MurimAnimations.RAVEN_HALLS_STEP, technique);
            MartialCombat.later(player, ticks(MurimAnimations.RAVEN_STEP_THROUGH), () -> step(player, to));
            return true;
        }
        if (technique == MartialArts.EXCHANGE) {
            LivingEntity target = AzureCloudSword.targetAhead(player, EXCHANGE_REACH);
            if (target == null || !canSwallow(player, target)) return false;
            LivingEntity other = player.isShiftKeyDown() ? nearestAlly(player) : player;
            if (other == null) return false;
            play(player, patch, MurimAnimations.RAVEN_EXCHANGE, technique);
            MartialCombat.later(player, ticks(MurimAnimations.RAVEN_EXCHANGE_SWAP), () -> exchange(player, target, other));
            return true;
        }
        if (technique == MartialArts.SWALLOWING_GATE) {
            play(player, patch, MurimAnimations.RAVEN_SWALLOWING_GATE, technique);
            MartialCombat.later(player, ticks(MurimAnimations.RAVEN_SWALLOW_OPEN), () -> openSwallowingGate(player));
            return true;
        }
        if (technique == MartialArts.CLOSED_ROOM) {
            int clap = ticks(MurimAnimations.RAVEN_ROOM_CLAP);
            if (player.isShiftKeyDown()) {
                boolean inHalls = RavenHalls.isHalls(player.level());
                play(player, patch, MurimAnimations.RAVEN_CLOSED_ROOM, technique);
                MartialCombat.later(player, clap, () -> {
                    if (inHalls) RavenHalls.leave(player);
                    else RavenHalls.home(player);
                });
                return true;
            }
            if (RavenHalls.isHalls(player.level())) return false;
            LivingEntity target = AzureCloudSword.targetAhead(player, ROOM_REACH);
            if (target == null || !canSwallow(player, target)) return false;
            play(player, patch, MurimAnimations.RAVEN_CLOSED_ROOM, technique);
            MartialCombat.later(player, clap, () -> {
                if (target.isAlive() && !RavenHalls.isHalls(player.level()) && RavenHalls.duel(player, target, DUEL_TICKS))
                    MartialProgress.addMastery(player, art(), MartialArts.CLOSED_ROOM.id(), 1);
            });
            return true;
        }
        if (technique == MartialArts.HALLS_UNFOLD) {
            if (RavenHalls.isHalls(player.level()) || unfoldable(player).isEmpty()) return false;
            play(player, patch, MurimAnimations.RAVEN_HALLS_UNFOLD, technique);
            ServerLevel level = player.serverLevel();
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks(MurimAnimations.RAVEN_UNFOLD_CALL), 2, false, false));
            MartialCombat.later(player, 6, () -> RavenFx.gate(level, player.position().add(0, 0.05, 0), RavenGateEntity.PIT, 7.0F, 0,
                    ticks(MurimAnimations.RAVEN_UNFOLD_CALL) + 8));
            MartialCombat.later(player, ticks(MurimAnimations.RAVEN_UNFOLD_CALL), () -> unfold(player));
            level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5F, 1.6F);
            player.displayClientMessage(Component.translatable("technique.murimblock.halls_unfold.called").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
            return true;
        }
        return false;
    }

    private static Vec3 look(ServerPlayer player) {
        return Vec3.directionFromRotation(0, player.getYRot());
    }

    /** A body's pit: a little wider than it. */
    static float pitRadius(LivingEntity body) {
        return Math.max(0.9F, body.getBbWidth() * 0.9F + 0.5F);
    }

    /** Where Halls Step comes out: where one looks, up to 16 blocks, on the near side of a wall, on the ground below; null if there is no room. */
    @Nullable private static Vec3 stepTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(), dir = player.getLookAngle();
        BlockHitResult hit = player.level().clip(new ClipContext(eye, eye.add(dir.scale(STEP_REACH)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 at = hit.getType() == HitResult.Type.MISS ? hit.getLocation() : hit.getLocation().subtract(dir.scale(0.7));
        // Feet on the ground below (at most 6 blocks down), or where it is if nothing is below.
        BlockHitResult ground = player.level().clip(new ClipContext(at, at.add(0, -6, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 feet = ground.getType() == HitResult.Type.MISS ? at.subtract(0, player.getEyeHeight(), 0) : ground.getLocation();
        if (feet.distanceTo(player.position()) < 2) return null;
        return player.level().noCollision(player, player.getBoundingBox().move(feet.subtract(player.position()))) ? feet : null;
    }

    /** Halls Step: a door where one stands, one where one comes out, the same way. */
    private static void step(ServerPlayer player, Vec3 feet) {
        if (!player.isAlive() || !player.level().noCollision(player, player.getBoundingBox().move(feet.subtract(player.position())))) return;
        ServerLevel level = player.serverLevel();
        RavenFx.door(level, player.position(), player.getYRot(), 14);
        player.teleportTo(feet.x, feet.y, feet.z);
        player.resetFallDistance();
        RavenFx.door(level, feet, player.getYRot() + 180, 14);
        MartialProgress.addMastery(player, art(), MartialArts.HALLS_STEP.id(), 1);
    }

    @Nullable private static LivingEntity nearestAlly(ServerPlayer player) {
        LivingEntity ally = null;
        double best = EXCHANGE_REACH;
        for (Player p : player.level().players()) {
            if (p == player || !p.isAlive() || p.isSpectator() || MartialCombat.isEnemy(p, player)) continue;
            double d = p.distanceTo(player);
            if (d <= best) {
                best = d;
                ally = p;
            }
        }
        return ally;
    }

    /** Exchange: two doors at once, the places changed. */
    private static void exchange(ServerPlayer player, LivingEntity target, LivingEntity other) {
        if (!target.isAlive() || !other.isAlive() || target.level() != player.level() || other.level() != player.level()) return;
        Vec3 a = target.position(), b = other.position();
        ServerLevel level = player.serverLevel();
        RavenFx.door(level, a, target.getYRot(), 14);
        RavenFx.door(level, b, other.getYRot(), 14);
        place(target, b);
        place(other, a);
        MartialProgress.addMastery(player, art(), MartialArts.EXCHANGE.id(), 1);
    }

    private static void place(LivingEntity e, Vec3 at) {
        if (e instanceof ServerPlayer p) p.teleportTo(p.serverLevel(), at.x, at.y, at.z, p.getYRot(), p.getXRot());
        else e.teleportTo(at.x, at.y, at.z);
        e.resetFallDistance();
    }

    /** The enemies the ultimate would take now. */
    private static List<LivingEntity> unfoldable(ServerPlayer player) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(UNFOLD_RADIUS),
                e -> e != player && MartialCombat.isEnemy(e, player) && e.distanceTo(player) <= UNFOLD_RADIUS && canSwallow(player, e));
    }

    /** The Halls Unfold: a door under every enemy in reach, each to a room of its own. */
    private static void unfold(ServerPlayer player) {
        if (!player.isAlive() || RavenHalls.isHalls(player.level())) return;
        ServerLevel level = player.serverLevel();
        int taken = 0;
        for (LivingEntity e : unfoldable(player)) {
            RavenFx.pit(level, e.position(), pitRadius(e), 14);
            if (RavenHalls.swallow(e, UNFOLD_TICKS, 0)) taken++;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.8F, 1.4F);
        RavenFx.feathers(level, player.position().add(0, 1, 0), 60, 6);
        if (taken > 0) MartialProgress.addMastery(player, art(), MartialArts.HALLS_UNFOLD.id(), 1);
    }

    // ---------------------------------------------------------------- the Swallowing Gate

    /** A gate standing at `centre`, facing `facing` (flat), open until `until`; what it swallowed, by shooter. */
    private record Gate(long until, Vec3 centre, Vec3 facing, List<UUID> shooters) { }

    private static final Map<UUID, Gate> GATES = new HashMap<>();
    static final double GATE_REACH_SWALLOW = 2.2;

    /** Whether a Swallowing Gate stands before this player now. */
    public static boolean gateOpen(ServerPlayer player) {
        return GATES.containsKey(player.getUUID());
    }

    private static void openSwallowingGate(ServerPlayer player) {
        if (!player.isAlive()) return;
        Vec3 facing = look(player), feet = player.position().add(facing.scale(1.4));
        RavenFx.door(player.serverLevel(), feet, player.getYRot(), SWALLOW_TICKS + 4).shape(RavenGateEntity.DOOR, 1.5F, player.getYRot(), SWALLOW_TICKS + 4);
        GATES.put(player.getUUID(), new Gate(player.level().getGameTime() + SWALLOW_TICKS, feet.add(0, 1.6, 0), facing, new ArrayList<>()));
    }

    private static void gateTick(ServerPlayer player, Gate gate) {
        ServerLevel level = player.serverLevel();
        if (level.getGameTime() >= gate.until || !player.isAlive() || player.level() != level) {
            GATES.remove(player.getUUID());
            sendBack(player, gate);
            return;
        }
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, new AABB(gate.centre, gate.centre).inflate(GATE_REACH_SWALLOW + 1),
                p -> p.getOwner() != player && p.isAlive())) {
            Vec3 to = p.position().subtract(gate.centre);
            // Only from the front: a projectile on the gate's far side, coming at it.
            if (to.length() > GATE_REACH_SWALLOW || to.dot(gate.facing) < -0.3) continue;
            Entity owner = p.getOwner();
            gate.shooters.add(owner != null ? owner.getUUID() : null);
            RavenFx.feathers(level, p.position(), 4, 0.2);
            p.discard();
        }
    }

    /** The swallowed projectiles come back out of the gate, at those who shot them (straight ahead if they are gone). */
    private static void sendBack(ServerPlayer player, Gate gate) {
        if (gate.shooters.isEmpty()) return;
        ServerLevel level = player.serverLevel();
        Vec3 from = gate.centre.add(gate.facing.scale(0.6));
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 0.8F, 1.4F);
        for (UUID id : gate.shooters) {
            Entity shooter = id == null ? null : level.getEntity(id);
            Arrow arrow = new Arrow(level, player, new ItemStack(net.minecraft.world.item.Items.ARROW), null);
            arrow.setPos(from.x, from.y, from.z);
            arrow.pickup = Arrow.Pickup.DISALLOWED;
            Vec3 aim = shooter != null && shooter.isAlive() ? shooter.position().add(0, shooter.getBbHeight() * 0.6, 0).subtract(from) : gate.facing;
            arrow.shoot(aim.x, aim.y + aim.horizontalDistance() * 0.05, aim.z, 2.6F, 1.0F);
            level.addFreshEntity(arrow);
        }
        MartialProgress.addMastery(player, art(), MartialArts.SWALLOWING_GATE.id(), 1);
    }

    // ---------------------------------------------------------------- the finish: Last Message

    /** The art's finish is the executor's when it is their main style, an iron fan in hand. */
    public static boolean finishes(ServerPlayer executor) {
        return stanceActive(executor) && holdsFan(executor);
    }

    /** Last Message: a door opens under the fallen, feathers rise round them, and the fan shuts on them. */
    public static void finishTick(ServerPlayer executor, ServerPlayer victim, int elapsed) {
        ServerLevel level = executor.serverLevel();
        if (elapsed == FINISH_OPEN_TICK) RavenFx.pit(level, victim.position(), 1.4F, FINISH_HIT_TICK - FINISH_OPEN_TICK + 8);
        if (elapsed > FINISH_OPEN_TICK && elapsed < FINISH_HIT_TICK && elapsed % 3 == 0)
            RavenFx.feathers(level, victim.position().add(0, 0.4, 0), 6, 1.2);
        if (elapsed == FINISH_HIT_TICK) {
            RavenFx.feathers(level, victim.position().add(0, 0.8, 0), 50, 1.6);
            level.playSound(null, victim.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.2F, 0.6F);
            level.playSound(null, victim.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.4F);
        }
    }

    // ---------------------------------------------------------------- Between Worlds

    private record Rift(UUID attacker, int count, long last) { }

    private static final Map<UUID, Rift> RIFTS_ON = new HashMap<>();

    @Nullable static ResourceLocation creditOf(Player player, AnimationAccessor<? extends StaticAnimation> a) {
        return isStrike(a) && stanceActive(player) && holdsFan(player) ? MartialArts.BETWEEN_WORLDS.id() : null;
    }

    static boolean isStrike(AnimationAccessor<? extends StaticAnimation> a) {
        return a != null && (a == SectWeaponAnimations.FAN_AUTO1 || a == SectWeaponAnimations.FAN_AUTO2 || a == SectWeaponAnimations.FAN_AUTO3
                || a == SectWeaponAnimations.FAN_AUTO4 || a == SectWeaponAnimations.FAN_DASH || a == SectWeaponAnimations.FAN_AIR);
    }

    /** A fan strike landed: one more rift on the body; on a body already with three, it tips through, behind the player. */
    public static void onHit(ServerPlayer player, LivingEntity target, boolean enemy) {
        if (!enemy || MurimGuardSkill.parriedThisTick(target) || MurimGuardSkill.guardedThisTick(target)) return;
        long now = player.level().getGameTime();
        Rift r = RIFTS_ON.get(target.getUUID());
        int count = r != null && r.attacker.equals(player.getUUID()) && now - r.last <= RIFT_TICKS ? r.count : 0;
        if (count >= RIFTS && canSwallow(player, target)) {
            RIFTS_ON.remove(target.getUUID());
            MartialCombat.later(player, 1, () -> {
                if (target.isAlive()) setBehind(player, target, StunType.LONG);
            });
            MartialProgress.addMastery(player, art(), MartialArts.BETWEEN_WORLDS.id(), 1);
            return;
        }
        RIFTS_ON.put(target.getUUID(), new Rift(player.getUUID(), Math.min(RIFTS, count + 1), now));
        RavenFx.feathers(player.serverLevel(), target.position().add(0, target.getBbHeight() / 2, 0), 3 + 3 * count, 0.4);
    }

    /** Through a door and out of another behind the player, facing away, stunned. */
    static void setBehind(ServerPlayer player, LivingEntity body, StunType stun) {
        Vec3 back = look(player).scale(-1.8);
        Vec3 to = player.position().add(back.x, 0, back.z);
        if (!player.level().noCollision(body, body.getBoundingBox().move(to.subtract(body.position())))) to = player.position();
        RavenFx.door(player.serverLevel(), body.position(), body.getYRot(), 12);
        body.teleportTo(to.x, to.y, to.z);
        body.setYRot(player.getYRot());
        body.setYHeadRot(player.getYRot());
        RavenFx.door(player.serverLevel(), to, player.getYRot() + 180, 12);
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(body, LivingEntityPatch.class);
        if (patch != null) patch.applyStun(stun, 1.5F);
    }

    /** The roll goes through the halls: unseen, and two blocks further. */
    static void rolled(ServerPlayer player) {
        if (!stanceActive(player) || !holdsFan(player)) return;
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 12, 0, false, false));
        Vec3 push = player.getDeltaMovement().multiply(1, 0, 1);
        if (push.lengthSqr() < 1e-4) push = look(player);
        Vec3 to = player.position().add(push.normalize().scale(2));
        if (player.level().noCollision(player, player.getBoundingBox().move(to.subtract(player.position())))) {
            RavenFx.feathers(player.serverLevel(), player.position().add(0, 1, 0), 14, 0.6);
            player.teleportTo(to.x, to.y, to.z);
        }
    }

    // ---------------------------------------------------------------- the secret: Return to Sender

    static void onPerfectParry(ServerPlayer player, Entity attacker) {
        if (!stanceActive(player) || !holdsFan(player) || !(attacker instanceof LivingEntity foe) || foe.distanceTo(player) > 6
                || !canSwallow(player, foe)) return;
        setBehind(player, foe, StunType.KNOCKDOWN);
        MartialData data = MartialService.data(player);
        if (!data.hasTechnique(MartialArts.RETURN_TO_SENDER.id())) {
            MartialService.set(player, data.unlock(MartialArts.RETURN_TO_SENDER));
            player.sendSystemMessage(Component.translatable("technique.murimblock.return_to_sender.discovered"));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.5F);
        }
    }

    // ---------------------------------------------------------------- ticks

    static void tick(ServerPlayer player) {
        Gate gate = GATES.get(player.getUUID());
        if (gate != null) gateTick(player, gate);
    }

    static void forget(@Nullable UUID id) {
        if (id == null) {
            GATES.clear();
            RIFTS_ON.clear();
        } else {
            GATES.remove(id);
            RIFTS_ON.remove(id);
        }
    }

    /** The rifts on a body now (for the tests). */
    public static int rifts(LivingEntity body) {
        Rift r = RIFTS_ON.get(body.getUUID());
        return r == null ? 0 : r.count;
    }
}
