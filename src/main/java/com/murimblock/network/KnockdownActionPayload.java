package com.murimblock.network;

import com.murimblock.Murimblock;
import com.murimblock.combat.knockdown.KnockdownService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client asks to carry (V), put down (V while carrying) or execute (B) a knocked-down player. Only a request: the
 * server checks reach, sight and every state in {@link KnockdownService} before doing anything.
 */
public record KnockdownActionPayload(Action action, int targetId) implements CustomPacketPayload {
    public enum Action { CARRY, RELEASE, EXECUTE }

    public static final CustomPacketPayload.Type<KnockdownActionPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "knockdown_action"));
    public static final StreamCodec<ByteBuf, KnockdownActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Action.values()[Math.floorMod(i, Action.values().length)], Action::ordinal), KnockdownActionPayload::action,
            ByteBufCodecs.VAR_INT, KnockdownActionPayload::targetId,
            KnockdownActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(KnockdownActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (payload.action == Action.RELEASE) {
            KnockdownService.release(player);
            return;
        }
        if (!(player.level().getEntity(payload.targetId) instanceof ServerPlayer target)) return;
        if (payload.action == Action.CARRY) KnockdownService.carry(player, target);
        else KnockdownService.execute(player, target);
    }
}
