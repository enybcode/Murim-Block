package com.murimblock.martial;

import com.murimblock.Murimblock;
import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.network.GoldenBellPayload;
import com.murimblock.qi.QiService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The Golden Bell in play (docs/combat/styles/golden-bell.md, Enzo 17/10/2026): a starter art of bare hands, all defence,
 * no passive (Epic Fight's own bare-hand strikes). The body shows it: gold in the form, iron in Iron Rebound, stone in
 * Rooted Mountain (the clients recolour the player's skin: GoldenBellPayload, client/GoldenBellSkins).
 * <ul>
 *   <li><b>Golden Bell</b>, a form one keeps: {@link #FORM_COST} Qi to go in, then {@link #FORM_QI_PER_SECOND} a second; the
 *   body all gold, {@link #FORM_REDUCTION} less damage taken, nothing pushes it; its key again, the Qi run out or a fall
 *   ends it, the bell ringing once more: a wave pushing back all near, harder for each blow taken in it. The others
 *   change in it;</li>
 *   <li><b>Iron Rebound</b>: {@link #REBOUND_TICKS} ticks, the next blow in melee is sent back whole to its author, pushed
 *   and thrown off balance; in the form (Golden Rebound) for twice as long, {@link #GOLDEN_REBOUND} as hard, and arrows
 *   are sent back too;</li>
 *   <li><b>Rooted Mountain</b>: {@link #ROOT_TICKS} ticks planted, nothing moves it, it heals; in the form (Golden
 *   Mountain) twice as fast, and every blow taken rings the bell at its author;</li>
 *   <li><b>Bell Toll</b>, the secret: a perfect parry in the form rings the bell at once and knocks the attacker down;
 *   found after {@link #TOLL_HITS} blows taken in the form;</li>
 *   <li><b>Last Bell</b>, the finish: the palm on the chest, three rings of the bell.</li>
 * </ul>
 */
public final class GoldenBell {
    public static final double FORM_COST = 15, FORM_QI_PER_SECOND = 4, FORM_QI_EVOLVED = 3;
    public static final float FORM_REDUCTION = 0.4F;
    /** The last ring: its reach, its push, its share per blow taken in the form, the most it reaches. */
    public static final double RING_REACH = 3.5, RING_PUSH = 1.0;
    public static final float RING_SHARE_PER_BLOW = 0.2F, RING_SHARE_MAX = 2.0F;
    public static final int REBOUND_TICKS = 20, REBOUND_MISSED_COOLDOWN = 80;
    public static final float GOLDEN_REBOUND = 1.5F;
    public static final int ROOT_TICKS = 80;
    public static final float ROOT_HEAL = 1, GOLDEN_ROOT_HEAL = 2, MOUNTAIN_RING_SHARE = 0.3F;
    public static final int TOLL_HITS = 20;
    /** The finish's rings (ticks in); the last kills. */
    public static final int[] FINISH_RINGS = {12, 22, 32};
    public static final int FINISH_HIT_TICK = 32;
    /** What the body is made of, for the clients. */
    public static final int SKIN_NONE = 0, SKIN_GOLD = 1, SKIN_IRON = 2, SKIN_STONE = 3;
    private static final ResourceLocation ROOTED = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "rooted_mountain");
    private static final ResourceLocation STEADY = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "golden_bell_steady");

    private GoldenBell() {
    }

    static MartialArts.Art art() {
        return MartialArts.art(MartialArts.GOLDEN_BELL);
    }

    public static boolean stanceActive(Player player) {
        if (player.level().isClientSide)
            return com.murimblock.cultivation.MeditationService.aura(player).mainArt().filter(MartialArts.GOLDEN_BELL::equals).isPresent();
        return MartialService.data(player).mainArt().filter(MartialArts.GOLDEN_BELL::equals).isPresent();
    }

    public static boolean bareHanded(Player player) {
        return player.getMainHandItem().isEmpty();
    }

    /** The main style, bare-handed: the forearms' guard, and the art's finish. */
    public static boolean guardsBareHanded(Player player) {
        return stanceActive(player) && bareHanded(player);
    }

    public static boolean finishes(ServerPlayer executor) {
        return guardsBareHanded(executor);
    }

    /** Epic Fight's bare-hand strikes, in the stance (they count for the passive's mastery, which has nothing else). */
    @Nullable static ResourceLocation creditOf(Player player, AnimationAccessor<? extends StaticAnimation> a) {
        if (LongFist.isStrike(a)) return guardsBareHanded(player) ? MartialArts.BELL_PALMS.id() : null;
        return null;
    }

    // ---------------------------------------------------------------- state

    private static final class Form {
        int blows;
        Form() { }
    }

    private static final Map<UUID, Form> FORMS = new HashMap<>();
    /** Until when (game time) each player's Iron Rebound waits for a blow, and whether it is the golden one. */
    private record Rebound(long until, boolean golden) { }
    private static final Map<UUID, Rebound> REBOUNDS = new HashMap<>();
    private record Root(long until, boolean golden) { }
    private static final Map<UUID, Root> ROOTS = new HashMap<>();
    /** The skin each player's clients were last told. */
    private static final Map<UUID, Integer> SKINS = new HashMap<>();

    public static boolean inForm(Player player) {
        return FORMS.containsKey(player.getUUID());
    }

    public static boolean rooted(Player player) {
        Root r = ROOTS.get(player.getUUID());
        return r != null && player.level().getGameTime() <= r.until;
    }

    static boolean rebounding(Player player) {
        Rebound r = REBOUNDS.get(player.getUUID());
        return r != null && player.level().getGameTime() <= r.until;
    }

    /** The body's matter now: iron while waiting to rebound, stone while rooted, gold in the form, else its own. */
    static int skin(Player player) {
        if (rebounding(player)) return SKIN_IRON;
        if (rooted(player)) return SKIN_STONE;
        return inForm(player) ? SKIN_GOLD : SKIN_NONE;
    }

    /** Tells the player and everyone round them what their body is made of, when it changes. */
    private static void syncSkin(ServerPlayer player) {
        int now = skin(player);
        Integer was = SKINS.get(player.getUUID());
        if (was != null && was == now || was == null && now == SKIN_NONE) return;
        if (now == SKIN_NONE) SKINS.remove(player.getUUID()); else SKINS.put(player.getUUID(), now);
        if (player.connection != null) PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new GoldenBellPayload(player.getId(), now, inForm(player)));
    }

    /** A player coming into sight of another is told their body's matter. */
    public static void startTracking(Entity target, ServerPlayer watcher) {
        if (target instanceof ServerPlayer p && SKINS.containsKey(p.getUUID()))
            PacketDistributor.sendToPlayer(watcher, new GoldenBellPayload(p.getId(), SKINS.get(p.getUUID()), inForm(p)));
    }

    // ---------------------------------------------------------------- techniques

    /** The form's key again while in it: it ends (free, whatever the cooldown). */
    static boolean leaveIfIn(ServerPlayer player) {
        if (!inForm(player)) return false;
        endForm(player, true);
        return true;
    }

    static boolean perform(ServerPlayer player, PlayerPatch<?> patch, MartialArts.Technique technique) {
        ServerLevel level = player.serverLevel();
        if (technique == MartialArts.GOLDEN_BELL_FORM) {
            patch.playAnimationSynchronized(MurimAnimations.GB_FORM, 0.0F);
            FORMS.put(player.getUUID(), new Form());
            steady(player, true);
            level.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.4F, 0.8F);
            level.playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 0.8F, 1.0F);
            syncSkin(player);
            // Not again while in it: its cooldown starts when it ends.
            MartialService.holdCooldown(player, technique);
            return true;
        }
        if (technique == MartialArts.IRON_REBOUND) {
            boolean golden = inForm(player);
            patch.playAnimationSynchronized(MurimAnimations.GB_REBOUND, 0.0F);
            REBOUNDS.put(player.getUUID(), new Rebound(level.getGameTime() + (golden ? REBOUND_TICKS * 2 : REBOUND_TICKS), golden));
            level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.4F, 1.8F);
            syncSkin(player);
            return true;
        }
        if (technique == MartialArts.ROOTED_MOUNTAIN) {
            boolean golden = inForm(player);
            patch.playAnimationSynchronized(MurimAnimations.GB_ROOTED, 0.0F);
            ROOTS.put(player.getUUID(), new Root(level.getGameTime() + ROOT_TICKS, golden));
            var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && !speed.hasModifier(ROOTED))
                speed.addTransientModifier(new AttributeModifier(ROOTED, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            steady(player, true);
            level.playSound(null, player.blockPosition(), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.2F, 0.6F);
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
                    level.getBlockState(player.blockPosition().below())), player.getX(), player.getY() + 0.1, player.getZ(), 30, 0.8, 0.05, 0.8, 0.1);
            syncSkin(player);
            return true;
        }
        return false;
    }

    /** Nothing pushes the body (the form, the mountain). */
    private static void steady(ServerPlayer player, boolean on) {
        var kb = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb == null) return;
        if (on && !kb.hasModifier(STEADY)) kb.addTransientModifier(new AttributeModifier(STEADY, 1.0, AttributeModifier.Operation.ADD_VALUE));
        if (!on) kb.removeModifier(STEADY);
    }

    /** The form ends: the last ring of the bell, the cooldown from now. */
    static void endForm(ServerPlayer player, boolean ring) {
        Form f = FORMS.remove(player.getUUID());
        if (f == null) return;
        if (!rooted(player)) steady(player, false);
        if (ring && player.isAlive()) ring(player, Math.min(RING_SHARE_MAX, RING_SHARE_PER_BLOW * f.blows), false);
        MartialService.startCooldown(player, MartialArts.GOLDEN_BELL_FORM);
        syncSkin(player);
    }

    /** The bell rings: all near are pushed back (knocked down if `down`), and struck by this share of the attack. */
    static void ring(ServerPlayer player, float share, boolean down) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0F, 0.6F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RING_REACH), e -> MartialCombat.isEnemy(e, player))) {
            if (e.distanceTo(player) > RING_REACH) continue;
            if (share > 0) e.hurt(player.damageSources().playerAttack(player), TechniquePower.damage(player, MartialArts.GOLDEN_BELL, share));
            Vec3 away = e.position().subtract(player.position()).multiply(1, 0, 1);
            Vec3 push = away.lengthSqr() < 1e-4 ? Vec3.directionFromRotation(0, player.getYRot()) : away.normalize();
            e.push(push.x * RING_PUSH, 0.3, push.z * RING_PUSH);
            e.hurtMarked = true;
            if (down) stun(e, StunType.KNOCKDOWN);
        }
        if (player.connection != null) PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new GoldenBellPayload(player.getId(), -1, inForm(player)));
    }

    private static void stun(LivingEntity e, StunType type) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(e, LivingEntityPatch.class);
        if (patch != null) patch.applyStun(type, type == StunType.KNOCKDOWN ? 2.0F : 0.8F);
    }

    // ---------------------------------------------------------------- blows taken

    /** A blow on a player of the art: sent back (Iron Rebound), rung back (Golden Mountain), lessened (the form). */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DamageSource source = event.getSource();
        Entity author = source.getEntity(), direct = source.getDirectEntity();
        Rebound r = REBOUNDS.get(player.getUUID());
        boolean melee = direct instanceof LivingEntity && direct == author && author != player;
        if (r != null && player.level().getGameTime() <= r.until && melee) {
            event.setCanceled(true);
            REBOUNDS.remove(player.getUUID());
            LivingEntity attacker = (LivingEntity) author;
            attacker.invulnerableTime = 0;
            attacker.hurt(player.damageSources().playerAttack(player), event.getAmount() * (r.golden ? GOLDEN_REBOUND : 1));
            Vec3 away = attacker.position().subtract(player.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) attacker.push(away.normalize().x * 0.9, 0.25, away.normalize().z * 0.9);
            attacker.hurtMarked = true;
            stun(attacker, StunType.NEUTRALIZE);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7F, 1.6F);
            if (player.connection != null) PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new GoldenBellPayload(player.getId(), -2, inForm(player)));
            MartialProgress.count(player, MartialArts.RETURN_IT_BLOWS, 1);
            MartialProgress.addMastery(player, art(), MartialArts.IRON_REBOUND.id(), 1);
            syncSkin(player);
            return;
        }
        Form f = FORMS.get(player.getUUID());
        Root root = ROOTS.get(player.getUUID());
        if (f == null && (root == null || player.level().getGameTime() > root.until)) return;
        if (author == null && direct == null) return;
        if (f != null) {
            event.setAmount(event.getAmount() * (1 - FORM_REDUCTION));
            f.blows++;
            if (MartialCombat.isEnemy(author, player)) {
                MartialProgress.count(player, MartialArts.TAKE_THE_BLOW_BLOWS, 1);
                MartialProgress.addMastery(player, art(), MartialArts.GOLDEN_BELL_FORM.id(), 1);
                tolled(player);
            }
        }
        if (root != null && root.golden && author instanceof LivingEntity attacker && attacker != player && player.level().getGameTime() <= root.until) {
            attacker.invulnerableTime = 0;
            attacker.hurt(player.damageSources().playerAttack(player), TechniquePower.damage(player, MartialArts.GOLDEN_BELL, MOUNTAIN_RING_SHARE));
            Vec3 away = attacker.position().subtract(player.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) attacker.push(away.normalize().x * 0.8, 0.2, away.normalize().z * 0.8);
            attacker.hurtMarked = true;
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
            if (player.connection != null) PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new GoldenBellPayload(player.getId(), -1, true));
        }
    }

    /** Blows taken in the form, until Bell Toll is found. */
    private static void tolled(ServerPlayer player) {
        MartialData data = MartialService.data(player);
        if (data.hasTechnique(MartialArts.BELL_TOLL.id())) return;
        int n = data.progress(MartialArts.BELL_TOLL_BLOWS) + 1;
        if (n < TOLL_HITS) { MartialService.set(player, data.withProgress(MartialArts.BELL_TOLL_BLOWS, n)); return; }
        MartialService.set(player, data.withProgress(MartialArts.BELL_TOLL_BLOWS, n).unlock(MartialArts.BELL_TOLL));
        player.sendSystemMessage(Component.translatable("technique.murimblock.bell_toll.discovered"));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.4F);
    }

    /** Nothing pushes the body in the form, nor in the mountain. */
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && (inForm(player) || rooted(player))) event.setCanceled(true);
    }

    /** Golden Rebound (or Iron Rebound evolved): an arrow reaching the player is sent back the way it came. */
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ServerPlayer player)) return;
        Rebound r = REBOUNDS.get(player.getUUID());
        if (r == null || player.level().getGameTime() > r.until) return;
        boolean evolved = MartialService.data(player).evolved(art(), MartialArts.IRON_REBOUND.id());
        if (!r.golden && !evolved) return;
        Projectile p = event.getProjectile();
        if (p.getOwner() == player) return;
        event.setCanceled(true);
        REBOUNDS.remove(player.getUUID());
        Entity shooter = p.getOwner();
        Vec3 back = shooter != null ? shooter.getEyePosition().subtract(p.position()).normalize() : p.getDeltaMovement().scale(-1).normalize();
        p.setDeltaMovement(back.scale(Math.max(1.2, p.getDeltaMovement().length())));
        p.setOwner(player);
        p.hurtMarked = true;
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6F, 1.8F);
        if (player.connection != null) PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new GoldenBellPayload(player.getId(), -2, inForm(player)));
        MartialProgress.count(player, MartialArts.RETURN_IT_BLOWS, 1);
        syncSkin(player);
    }

    /** No blow struck from the mountain. */
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && rooted(player)) event.setCanceled(true);
    }

    /** A perfect parry in the form, with Bell Toll found: the bell rings at once, the attacker knocked down. */
    static void onPerfectParry(ServerPlayer player, Entity attacker) {
        if (!inForm(player) || !MartialService.data(player).hasTechnique(MartialArts.BELL_TOLL.id())) return;
        ring(player, 0.6F, false);
        if (attacker instanceof LivingEntity l) stun(l, StunType.KNOCKDOWN);
    }

    // ---------------------------------------------------------------- ticks

    static void tick(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (FORMS.containsKey(player.getUUID())) {
            boolean evolved = MartialService.data(player).evolved(art(), MartialArts.GOLDEN_BELL_FORM.id());
            double perTick = (evolved ? FORM_QI_EVOLVED : FORM_QI_PER_SECOND) / 20.0;
            if (QiService.getQi(player) < perTick || !player.isAlive() || KnockdownService.isHindered(player) || !bareHanded(player)) endForm(player, player.isAlive());
            else QiService.removeQi(player, perTick);
        }
        Root root = ROOTS.get(player.getUUID());
        if (root != null) {
            if (now > root.until || !player.isAlive()) {
                ROOTS.remove(player.getUUID());
                var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) speed.removeModifier(ROOTED);
                if (!inForm(player)) steady(player, false);
                MartialService.startCooldown(player, MartialArts.ROOTED_MOUNTAIN);
            } else if ((now - root.until) % 20 == 0) {
                player.heal(root.golden || MartialService.data(player).evolved(art(), MartialArts.ROOTED_MOUNTAIN.id()) ? GOLDEN_ROOT_HEAL : ROOT_HEAL);
            }
        }
        Rebound r = REBOUNDS.get(player.getUUID());
        if (r != null && now > r.until) {
            // Missed: nothing struck. The cooldown is shorter.
            REBOUNDS.remove(player.getUUID());
            MartialService.reduceCooldownTo(player, MartialArts.IRON_REBOUND, REBOUND_MISSED_COOLDOWN);
        }
        syncSkin(player);
    }

    // ---------------------------------------------------------------- the finish: Last Bell

    public static void finishTick(ServerPlayer executor, ServerPlayer victim, int elapsed) {
        if (elapsed == 2) {
            SKINS.put(executor.getUUID(), SKIN_GOLD);
            if (executor.connection != null) PacketDistributor.sendToPlayersTrackingEntityAndSelf(executor, new GoldenBellPayload(executor.getId(), SKIN_GOLD, true));
        }
        for (int t : FINISH_RINGS) if (elapsed == t) {
            executor.serverLevel().playSound(null, victim.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.6F, t == FINISH_HIT_TICK ? 0.5F : 0.8F);
            if (executor.connection != null) PacketDistributor.sendToPlayersTrackingEntityAndSelf(executor, new GoldenBellPayload(executor.getId(), -1, true));
        }
        if (elapsed == FINISH_HIT_TICK + 10) syncSkin(executor);
    }

    static void forget(@Nullable UUID player) {
        if (player == null) { FORMS.clear(); REBOUNDS.clear(); ROOTS.clear(); SKINS.clear(); return; }
        FORMS.remove(player);
        REBOUNDS.remove(player);
        ROOTS.remove(player);
        SKINS.remove(player);
    }

    /** For tests: ends everything at once (attributes back), without cooldowns. */
    public static void stopForTests(ServerPlayer player) {
        FORMS.remove(player.getUUID());
        REBOUNDS.remove(player.getUUID());
        ROOTS.remove(player.getUUID());
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(ROOTED);
        steady(player, false);
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (patch != null) patch.playAnimationSynchronized(yesman.epicfight.gameasset.Animations.BIPED_IDLE, 0.0F);
    }

    /** For tests: the blows taken in the current form. */
    public static int blowsInForm(Player player) {
        Form f = FORMS.get(player.getUUID());
        return f == null ? -1 : f.blows;
    }
}
