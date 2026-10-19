package com.murimblock.martial;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.integration.epicfight.MurimWeaponCategories;
import com.murimblock.integration.epicfight.SectWeaponAnimations;
import com.murimblock.item.MurimItems;
import com.murimblock.world.raven.RavenHalls;
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

    static boolean perform(ServerPlayer player, PlayerPatch<?> patch, MartialArts.Technique technique) {
        if (RavenHalls.dueling(player) && technique != MartialArts.SWALLOWING_GATE) return false;
        if (technique == MartialArts.RAVEN_GATE) {
            LivingEntity target = AzureCloudSword.targetAhead(player, GATE_REACH);
            if (target == null || !canSwallow(player, target)) return false;
            return RavenHalls.swallow(target, GATE_TICKS, GATE_DROP);
        }
        if (technique == MartialArts.HALLS_STEP) return step(player);
        if (technique == MartialArts.EXCHANGE) return exchange(player);
        if (technique == MartialArts.SWALLOWING_GATE) {
            GATES.put(player.getUUID(), new Gate(player.level().getGameTime() + SWALLOW_TICKS, new ArrayList<>()));
            RavenHalls.gateFx(player.serverLevel(), player.position().add(look(player).scale(1.5)));
            return true;
        }
        if (technique == MartialArts.CLOSED_ROOM) {
            if (player.isShiftKeyDown()) {
                if (RavenHalls.isHalls(player.level())) {
                    RavenHalls.leave(player);
                    return true;
                }
                return RavenHalls.home(player);
            }
            if (RavenHalls.isHalls(player.level())) return false;
            LivingEntity target = AzureCloudSword.targetAhead(player, ROOM_REACH);
            if (target == null || !canSwallow(player, target)) return false;
            return RavenHalls.duel(player, target, DUEL_TICKS);
        }
        if (technique == MartialArts.HALLS_UNFOLD) {
            if (RavenHalls.isHalls(player.level())) return false;
            List<LivingEntity> foes = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(UNFOLD_RADIUS),
                    e -> e != player && MartialCombat.isEnemy(e, player) && e.distanceTo(player) <= UNFOLD_RADIUS && canSwallow(player, e));
            if (foes.isEmpty()) return false;
            for (LivingEntity e : foes) RavenHalls.swallow(e, UNFOLD_TICKS, 0);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5F, 1.6F);
            return true;
        }
        return false;
    }

    private static Vec3 look(ServerPlayer player) {
        return Vec3.directionFromRotation(0, player.getYRot());
    }

    /** Halls Step: out where one looks, up to 16 blocks, on the near side of a wall. */
    private static boolean step(ServerPlayer player) {
        if (RavenHalls.isHalls(player.level())) return false;
        Vec3 eye = player.getEyePosition(), dir = player.getLookAngle();
        BlockHitResult hit = player.level().clip(new ClipContext(eye, eye.add(dir.scale(STEP_REACH)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 at = hit.getType() == HitResult.Type.MISS ? hit.getLocation() : hit.getLocation().subtract(dir.scale(0.7));
        // Feet on the ground below (at most 6 blocks down), or where it is if nothing is below.
        BlockHitResult ground = player.level().clip(new ClipContext(at, at.add(0, -6, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 feet = ground.getType() == HitResult.Type.MISS ? at.subtract(0, player.getEyeHeight(), 0) : ground.getLocation();
        if (!player.level().noCollision(player, player.getBoundingBox().move(feet.subtract(player.position())))) return false;
        RavenHalls.gateFx(player.serverLevel(), player.position());
        player.teleportTo(feet.x, feet.y, feet.z);
        player.fallDistance = 0;
        RavenHalls.gateFx(player.serverLevel(), feet);
        return true;
    }

    /** Exchange: two doors at once. */
    private static boolean exchange(ServerPlayer player) {
        LivingEntity target = AzureCloudSword.targetAhead(player, EXCHANGE_REACH);
        if (target == null || !canSwallow(player, target)) return false;
        LivingEntity other = player;
        if (player.isShiftKeyDown()) {
            other = null;
            double best = EXCHANGE_REACH;
            for (Player p : player.level().players()) {
                if (p == player || !p.isAlive() || p.isSpectator() || MartialCombat.isEnemy(p, player)) continue;
                double d = p.distanceTo(player);
                if (d <= best) {
                    best = d;
                    other = p;
                }
            }
            if (other == null) return false;
        }
        Vec3 a = target.position(), b = other.position();
        ServerLevel level = player.serverLevel();
        RavenHalls.gateFx(level, a);
        RavenHalls.gateFx(level, b);
        place(target, b);
        place(other, a);
        return true;
    }

    private static void place(LivingEntity e, Vec3 at) {
        if (e instanceof ServerPlayer p) p.teleportTo(p.serverLevel(), at.x, at.y, at.z, p.getYRot(), p.getXRot());
        else e.teleportTo(at.x, at.y, at.z);
        e.fallDistance = 0;
    }

    // ---------------------------------------------------------------- the Swallowing Gate

    private record Gate(long until, List<UUID> shooters) { }

    private static final Map<UUID, Gate> GATES = new HashMap<>();

    /** Whether a Swallowing Gate stands before this player now. */
    public static boolean gateOpen(ServerPlayer player) {
        return GATES.containsKey(player.getUUID());
    }

    private static void gateTick(ServerPlayer player, Gate gate) {
        ServerLevel level = player.serverLevel();
        if (level.getGameTime() >= gate.until || !player.isAlive()) {
            GATES.remove(player.getUUID());
            sendBack(player, gate.shooters);
            return;
        }
        Vec3 look = look(player), front = player.position().add(0, 1, 0).add(look.scale(1.5));
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, new AABB(front, front).inflate(2.5),
                p -> p.getOwner() != player && p.isAlive())) {
            Vec3 to = p.position().subtract(player.position());
            if (new Vec3(to.x, 0, to.z).normalize().dot(look) < 0.2) continue;
            Entity owner = p.getOwner();
            gate.shooters.add(owner != null ? owner.getUUID() : null);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, p.getX(), p.getY(), p.getZ(), 6, 0.1, 0.1, 0.1, 0.02);
            p.discard();
        }
    }

    /** The swallowed projectiles come back out, at those who shot them (straight ahead if they are gone). */
    private static void sendBack(ServerPlayer player, List<UUID> shooters) {
        if (shooters.isEmpty()) return;
        ServerLevel level = player.serverLevel();
        Vec3 from = player.getEyePosition().add(look(player).scale(1.2));
        RavenHalls.gateFx(level, from.subtract(0, 1, 0));
        for (UUID id : shooters) {
            Entity shooter = id == null ? null : level.getEntity(id);
            Arrow arrow = new Arrow(level, player, new ItemStack(net.minecraft.world.item.Items.ARROW), null);
            arrow.setPos(from.x, from.y, from.z);
            arrow.pickup = Arrow.Pickup.DISALLOWED;
            Vec3 aim = shooter != null && shooter.isAlive() ? shooter.position().add(0, shooter.getBbHeight() * 0.6, 0).subtract(from) : player.getLookAngle();
            arrow.shoot(aim.x, aim.y + aim.horizontalDistance() * 0.05, aim.z, 2.6F, 1.0F);
            level.addFreshEntity(arrow);
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
            return;
        }
        RIFTS_ON.put(target.getUUID(), new Rift(player.getUUID(), Math.min(RIFTS, count + 1), now));
        player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, target.getX(), target.getY() + target.getBbHeight() / 2,
                target.getZ(), 4 + 3 * count, 0.2, 0.3, 0.2, 0.01);
    }

    /** Through a door and out of another behind the player, facing away, stunned. */
    static void setBehind(ServerPlayer player, LivingEntity body, StunType stun) {
        Vec3 back = look(player).scale(-1.8);
        Vec3 to = player.position().add(back.x, 0, back.z);
        if (!player.level().noCollision(body, body.getBoundingBox().move(to.subtract(body.position())))) to = player.position();
        RavenHalls.gateFx(player.serverLevel(), body.position());
        body.teleportTo(to.x, to.y, to.z);
        body.setYRot(player.getYRot());
        body.setYHeadRot(player.getYRot());
        RavenHalls.gateFx(player.serverLevel(), to);
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
            RavenHalls.gateFx(player.serverLevel(), player.position());
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
