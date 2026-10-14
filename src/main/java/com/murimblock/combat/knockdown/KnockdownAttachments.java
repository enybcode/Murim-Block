package com.murimblock.combat.knockdown;

import com.murimblock.Murimblock;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class KnockdownAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Murimblock.MOD_ID);

    /** Not copied on death: a respawned player stands. Synced to everyone tracking the player. */
    static final Supplier<AttachmentType<KnockdownData>> PLAYER_KNOCKDOWN = ATTACHMENT_TYPES.register(
            "player_knockdown",
            () -> AttachmentType.builder(() -> KnockdownData.STANDING)
                    .serialize(KnockdownData.CODEC)
                    .sync((holder, recipient) -> true, KnockdownData.STREAM_CODEC)
                    .build()
    );

    private KnockdownAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
