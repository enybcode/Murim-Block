package com.murimblock.cultivation;

import com.murimblock.Murimblock;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class CultivationAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Murimblock.MOD_ID);

    static final Supplier<AttachmentType<CultivationData>> PLAYER_CULTIVATION = ATTACHMENT_TYPES.register(
            "player_cultivation",
            () -> AttachmentType.builder(CultivationData::initial)
                    .serialize(CultivationData.CODEC)
                    .copyOnDeath()
                    .sync((holder, recipient) -> holder == recipient, CultivationData.STREAM_CODEC)
                    .build()
    );

    private CultivationAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
