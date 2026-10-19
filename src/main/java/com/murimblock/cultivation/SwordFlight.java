package com.murimblock.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.network.CultivationPayloads;
import com.murimblock.qi.QiService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Flying on the sword (御剑飞行), from Core Formation (docs/cultivation/refonte-cultivation.md, 19/10/2026): a sword in
 * hand, the jump key pressed a second time in the air and held {@link #CHARGE_TICKS} (Enzo), the Qi gathers under the
 * feet, the sword goes under them and carries them where they look (client.cultivation.SwordFlightClient steers). Each
 * second costs {@link #QI_PER_SECOND} Qi; without (or the sword let go), it sets them down gently (slow falling). Jump
 * pressed twice in flight, or landing, ends it (in the air, set down gently). Sprinting, very fast, at a higher cost. While flying, the server lets them be off the ground (no kick).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class SwordFlight {
    /** The realm it comes with. */
    public static final CultivationRealm REALM = CultivationRealm.CORE_FORMATION;
    /** What a second of flight costs. */
    public static final double QI_PER_SECOND = 15;
    /**
     * Very fast (Enzo, 19/10/2026: "la possibilité d'aller très vite"): sprinting on the sword, faster than this (blocks a
     * tick) counts as very fast, and each second of it costs {@link #FAST_QI_PER_SECOND} instead.
     */
    public static final double FAST_FROM = 1.5, FAST_QI_PER_SECOND = 60;
    /** How long the jump key is held, the second press, before the sword comes (3 s). */
    public static final int CHARGE_TICKS = 60;
    /** Landed this long, the flight ends. */
    static final int LANDED_TICKS = 10;

    private static final Map<UUID, Long> CHARGING = new HashMap<>();
    private static final Set<UUID> FLYING = new HashSet<>();
    private static final Map<UUID, Integer> LANDED = new HashMap<>();
    /** Where each flyer was last tick, and how many ticks of the current second they went very fast. */
    private static final Map<UUID, net.minecraft.world.phys.Vec3> LAST = new HashMap<>();
    private static final Map<UUID, Integer> FAST = new HashMap<>();

    private SwordFlight() {
    }

    /** The swords one flies on (the tag murimblock:flying_swords: Minecraft's swords and the mod's jian and dao; not its spears, staff or fans). */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> FLYING_SWORDS =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "flying_swords"));

    /** A sword: what one flies on. */
    public static boolean isSword(ItemStack stack) {
        return stack.is(FLYING_SWORDS);
    }

    /** Whether this player may fly on their sword: the realm reached, a sword in hand, Qi for a second at least. */
    public static boolean able(Player player) {
        return CultivationService.getRealm(player).ordinal() >= REALM.ordinal() && isSword(player.getMainHandItem())
                && QiService.getQi(player) >= QI_PER_SECOND && !player.isSpectator();
    }

    public static boolean flying(Player player) {
        return FLYING.contains(player.getUUID());
    }

    /** The second press of the jump key held: the Qi gathers. */
    public static void charge(ServerPlayer player) {
        if (flying(player) || !able(player)) return;
        CHARGING.put(player.getUUID(), player.level().getGameTime());
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.6F);
    }

    public static void cancel(ServerPlayer player) {
        CHARGING.remove(player.getUUID());
    }

    /** Held long enough: onto the sword. */
    public static void mount(ServerPlayer player) {
        Long since = CHARGING.remove(player.getUUID());
        // The client holds the full three seconds; a server running behind counts fewer ticks: half is enough here.
        if (since == null || player.level().getGameTime() - since < CHARGE_TICKS / 2 || !able(player)) return;
        FLYING.add(player.getUUID());
        LANDED.remove(player.getUUID());
        // Off the ground is allowed (the server would kick a floating player otherwise); not sent, so that the client's
        // own double jump does not turn into creative flight.
        player.getAbilities().mayfly = true;
        player.setNoGravity(true);
        player.resetFallDistance();
        player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2.value(), SoundSource.PLAYERS, 0.8F, 1.3F);
        broadcast(player, true);
    }

    /** Off the sword: set down gently if in the air (slow falling a moment); told so when the Qi or the sword failed. */
    public static void land(ServerPlayer player, boolean gently) {
        CHARGING.remove(player.getUUID());
        LANDED.remove(player.getUUID());
        LAST.remove(player.getUUID());
        FAST.remove(player.getUUID());
        if (!FLYING.remove(player.getUUID())) return;
        player.getAbilities().mayfly = player.isCreative() || player.isSpectator();
        player.setNoGravity(false);
        player.resetFallDistance();
        if (gently || !player.onGround()) player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false));
        if (gently) player.displayClientMessage(Component.translatable("cultivation.murimblock.sword_flight.spent"), true);
        broadcast(player, false);
    }

    public static void tick(ServerPlayer player) {
        if (!flying(player)) return;
        player.resetFallDistance();
        if (player.onGround()) {
            int landed = LANDED.merge(player.getUUID(), 1, Integer::sum);
            if (landed >= LANDED_TICKS) {
                land(player, false);
                return;
            }
        } else LANDED.remove(player.getUUID());
        // How fast, from where they were (the client moves them): very fast ticks counted.
        var last = LAST.put(player.getUUID(), player.position());
        if (last != null && last.distanceTo(player.position()) > FAST_FROM) FAST.merge(player.getUUID(), 1, Integer::sum);
        if (player.tickCount % 20 == 0) {
            if (!able(player)) {
                land(player, true);
                return;
            }
            int fast = FAST.getOrDefault(player.getUUID(), 0);
            FAST.remove(player.getUUID());
            QiService.removeQi(player, QI_PER_SECOND + (FAST_QI_PER_SECOND - QI_PER_SECOND) * fast / 20.0);
        }
    }

    static void broadcast(ServerPlayer player, boolean flying) {
        var payload = new CultivationPayloads.SwordFlightState(player.getId(), flying);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload);
    }

    /** One who comes to see a flyer is told they fly. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer flyer && flying(flyer) && event.getEntity() instanceof ServerPlayer viewer)
            PacketDistributor.sendToPlayer(viewer, new CultivationPayloads.SwordFlightState(flyer.getId(), true));
    }

    public static void forget(ServerPlayer player) {
        land(player, false);
    }

    public static void clearForTests() {
        CHARGING.clear();
        FLYING.clear();
        LANDED.clear();
        LAST.clear();
        FAST.clear();
    }
}
