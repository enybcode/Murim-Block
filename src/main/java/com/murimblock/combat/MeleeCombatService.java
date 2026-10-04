package com.murimblock.combat;

import com.murimblock.Murimblock;
import com.murimblock.combat.preview.CombatPreviewService;
import com.murimblock.qi.QiService;
import com.murimblock.qi.charge.QiChargeService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import static com.murimblock.combat.MeleeData.Action.*;

/** First live slice: sword guard only. The trajectory attack core is not wired in yet. */
public final class MeleeCombatService {
    public static final int GUARD_REFRESH_TICKS = 10;
    public static final int GUARD_TIMEOUT_TICKS = 30;
    private static final ResourceLocation GUARD_SLOW_ID = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "guard_slow");
    private static final Map<UUID, GuardRequest> GUARD_REQUESTS = new HashMap<>();

    private MeleeCombatService() { }

    public static MeleeData getData(Player player) {
        return player.getData(CombatAttachments.PLAYER_MELEE);
    }

    public static boolean canUseSwordCombat(Player player) {
        return player.isAlive() && !player.isSpectator() && CombatService.isInCombatMode(player)
                && CombatProfiles.basicFor(player.getMainHandItem()).isPresent()
                && player.getOffhandItem().isEmpty() && !player.isUsingItem();
    }

    public static boolean isBusy(Player player) {
        return canUseSwordCombat(player) && getData(player).isActive(player.level().getGameTime());
    }

    public static void requestGuard(ServerPlayer player, boolean holding) {
        if (!holding) {
            release(player);
            return;
        }
        if (!canUseSwordCombat(player)) {
            clear(player);
            return;
        }
        CombatPreviewService.cancel(player);
        long tick = player.server.getTickCount();
        GuardRequest request = GUARD_REQUESTS.get(player.getUUID());
        if (request != null && !request.matches(player.getInventory().selected, player.getMainHandItem())) {
            release(player);
            request = null;
        }
        GUARD_REQUESTS.put(player.getUUID(), request == null
                ? GuardRequest.start(player.getInventory().selected, player.getMainHandItem(), tick)
                : request.refresh(tick));
        updateGuard(player);
        updateMovement(player);
    }

    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !canUseSwordCombat(player)) return;
        long tick = player.level().getGameTime();
        if (getData(player).locksAttack(tick)) {
            event.setCanceled(true);
            return;
        }
        if (QiChargeService.isCharging(player)) {
            event.setCanceled(true);
            return;
        }
        // The hit stays vanilla for now, but cannot also benefit from a held sword guard.
        var rules = profile(player).guard();
        player.setData(CombatAttachments.PLAYER_MELEE, MeleeData.begin(SWING, tick, rules.attackRecoveryTicks()));
        updateMovement(player);
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!canUseSwordCombat(player)) {
                clear(player);
                continue;
            }
            GuardRequest request = GUARD_REQUESTS.get(player.getUUID());
            if (request != null && (!request.isLive(server.getTickCount())
                    || !request.matches(player.getInventory().selected, player.getMainHandItem()))) {
                release(player);
            }
            MeleeData data = getData(player);
            if (data.action() != IDLE && player.level().getGameTime() >= data.endsAt()) {
                player.setData(CombatAttachments.PLAYER_MELEE,
                        data.action() == GUARD_IMPACT && GUARD_REQUESTS.containsKey(player.getUUID())
                                ? MeleeData.begin(GUARD, data.startedAt(), 0) : MeleeData.initial());
            }
            updateGuard(player);
            updateMovement(player);
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !canUseSwordCombat(player)
                || !getData(player).isGuarding() || !isBusy(player)) return;
        GuardRequest request = GUARD_REQUESTS.get(player.getUUID());
        if (request == null || !request.isLive(player.server.getTickCount())
                || !request.matches(player.getInventory().selected, player.getMainHandItem())) return;
        var source = event.getSource();
        var rules = profile(player).guard();
        if (source.is(DamageTypeTags.BYPASSES_SHIELD)
                || !(source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
                || source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)
                || source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK_NO_AGGRO))
                || !(source.getDirectEntity() instanceof LivingEntity attacker)
                || attacker == player || attacker.level() != player.level() || !player.hasLineOfSight(attacker)
                || !rules.covers(player.getLookAngle(), attacker.position().subtract(player.position()))
                || rules.cost(event.getAmount()) <= 0) return;
        long tick = player.level().getGameTime();
        if (!rules.canPay(QiService.getQi(player), event.getAmount())) {
            QiService.setQi(player, 0);
            player.setData(CombatAttachments.PLAYER_MELEE, MeleeData.begin(GUARD_BREAK, tick, rules.breakTicks()));
            updateMovement(player);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.7F, 0.9F);
            player.displayClientMessage(Component.translatable("message.murimblock.guard.broken"), true);
            return;
        }
        QiService.removeQi(player, rules.cost(event.getAmount()));
        event.setCanceled(true);
        player.setData(CombatAttachments.PLAYER_MELEE, MeleeData.begin(GUARD_IMPACT, tick, rules.impactTicks()));
        var point = player.getEyePosition().add(player.getLookAngle().scale(0.7));
        player.serverLevel().playSound(null, point.x, point.y, point.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.65F, 1.15F);
        player.serverLevel().sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 5, 0.08, 0.08, 0.08, 0.12);
    }

    private static CombatProfile profile(Player player) {
        return CombatProfiles.basicFor(player.getMainHandItem()).orElseThrow();
    }

    private static void updateGuard(ServerPlayer player) {
        if (!GUARD_REQUESTS.containsKey(player.getUUID()) || getData(player).action() != IDLE) return;
        QiChargeService.stopCharging(player);
        player.setSprinting(false);
        player.setData(CombatAttachments.PLAYER_MELEE, MeleeData.begin(GUARD, player.level().getGameTime(), 0));
    }

    private static void updateMovement(ServerPlayer player) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        if (canUseSwordCombat(player) && getData(player).isGuarding()
                && getData(player).isActive(player.level().getGameTime()) && GUARD_REQUESTS.containsKey(player.getUUID())) {
            player.setSprinting(false);
            speed.addOrUpdateTransientModifier(new AttributeModifier(GUARD_SLOW_ID,
                    profile(player).guard().movementScale() - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            speed.removeModifier(GUARD_SLOW_ID);
        }
    }

    private static void release(ServerPlayer player) {
        GUARD_REQUESTS.remove(player.getUUID());
        if (getData(player).action() == GUARD) player.setData(CombatAttachments.PLAYER_MELEE, MeleeData.initial());
        updateMovement(player);
    }

    public static void clear(ServerPlayer player) {
        GUARD_REQUESTS.remove(player.getUUID());
        if (!getData(player).equals(MeleeData.initial())) player.setData(CombatAttachments.PLAYER_MELEE, MeleeData.initial());
        updateMovement(player);
    }

    public static void clearAll() {
        GUARD_REQUESTS.clear();
    }
}
