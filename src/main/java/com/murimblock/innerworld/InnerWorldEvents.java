package com.murimblock.innerworld;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Hooks the inner world into the game: the travel message, the checks on players inside, and the rule that nothing in
 * an inner world can be broken, placed or taken, whatever the game mode.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class InnerWorldEvents {
    private static final String PROTOCOL = "1";

    private InnerWorldEvents() {
    }

    /** Client to server: enter the inner world, or return to the body. */
    public record Travel() implements CustomPacketPayload {
        public static final Type<Travel> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "inner_world_travel"));
        public static final StreamCodec<ByteBuf, Travel> STREAM_CODEC = StreamCodec.unit(new Travel());

        @Override
        public Type<Travel> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL).playToServer(Travel.TYPE, Travel.STREAM_CODEC,
                (payload, context) -> InnerWorld.travel((ServerPlayer) context.player()));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && InnerWorld.isInnerWorld(player.level())) {
            InnerWorld.tick(player);
        }
    }

    /** Logging in inside (server stopped or player left while in): wake in the body. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && (InnerWorld.state(player).inside() || InnerWorld.isInnerWorld(player.level()))) {
            InnerWorld.leave(player, "message.murimblock.inner_world.woke");
        }
    }

    /** Taken out by other means (a command, a portal): the body has nobody to wait for. */
    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTo() != InnerWorld.LEVEL) {
            InnerWorld.state(player).clear();
        }
    }

    /** Nobody dies in their inner world: a lethal blow sends the spirit back to the body instead. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && InnerWorld.isInnerWorld(player.level())
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && event.getAmount() >= player.getHealth()) {
            event.setCanceled(true);
            InnerWorld.leave(player, "message.murimblock.inner_world.lethal");
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && InnerWorld.isInnerWorld(level)) {
            deny(event, event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level && InnerWorld.isInnerWorld(level)) {
            deny(event, event.getEntity() instanceof Player player ? player : null);
        }
    }

    /** Stops mining before it starts, so blocks do not even crack (and creative cannot break them). */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (InnerWorld.isInnerWorld(event.getLevel())) {
            event.setCanceled(true);
        }
    }

    /** No using items on blocks or the blocks themselves: no bucket, bone meal, flint, stripping or trampling. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (InnerWorld.isInnerWorld(event.getLevel())) {
            event.setCanceled(true);
        }
    }

    /** Buckets and items placed by use in the air (water, lily pads) would take or add blocks. */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (InnerWorld.isInnerWorld(event.getLevel())
                && (event.getItemStack().getItem() instanceof BucketItem || event.getItemStack().getItem() instanceof BlockItem)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof Level level && InnerWorld.isInnerWorld(level)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (InnerWorld.isInnerWorld(event.getLevel())) {
            event.getAffectedBlocks().clear();
        }
    }

    private static void deny(ICancellableEvent event, Player player) {
        event.setCanceled(true);
        if (player instanceof ServerPlayer serverPlayer) {
            InnerWorld.system(serverPlayer, "message.murimblock.inner_world.unchangeable", ChatFormatting.GOLD);
        }
    }
}
