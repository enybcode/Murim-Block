package com.murimblock.network;

import com.murimblock.Murimblock;
import com.murimblock.mob.GoldenDragonShop;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The Golden Dragon's scroll of wares (GoldenDragonShop): the scroll shown to a player, and a ware bought. */
public final class ScrollShopPayloads {
    private ScrollShopPayloads() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
    }

    /** A line of the scroll: what it shows, its name, its price in gold yuanbao, its difficulty (0 to 4), whether it is for sale. */
    public record Ware(ItemStack icon, Component name, int price, int difficulty, boolean forSale) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Ware> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Ware::icon,
                ComponentSerialization.TRUSTED_STREAM_CODEC, Ware::name,
                ByteBufCodecs.VAR_INT, Ware::price,
                ByteBufCodecs.VAR_INT, Ware::difficulty,
                ByteBufCodecs.BOOL, Ware::forSale,
                Ware::new);
    }

    /** Server to a player: the scroll of this seller, unrolled. */
    public record Open(int sellerId, List<Ware> wares) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(id("scroll_shop_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::sellerId,
                Ware.STREAM_CODEC.apply(ByteBufCodecs.list(32)), Open::wares,
                Open::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client: the ware at this line bought from this seller. */
    public record Buy(int sellerId, int index) implements CustomPacketPayload {
        public static final Type<Buy> TYPE = new Type<>(id("scroll_shop_buy"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Buy> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Buy::sellerId,
                ByteBufCodecs.VAR_INT, Buy::index,
                Buy::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(Buy payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) GoldenDragonShop.buy(player, payload.sellerId, payload.index);
        }
    }
}
