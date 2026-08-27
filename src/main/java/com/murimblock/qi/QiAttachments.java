package com.murimblock.qi;

import com.murimblock.Murimblock;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class QiAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Murimblock.MOD_ID);

    static final Supplier<AttachmentType<QiData>> PLAYER_QI = ATTACHMENT_TYPES.register(
            "player_qi",
            () -> AttachmentType.builder(QiData::initial)
                    .serialize(QiData.CODEC)
                    .copyOnDeath()
                    .sync((holder, recipient) -> holder == recipient, QiData.STREAM_CODEC)
                    .build()
    );

    private QiAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
