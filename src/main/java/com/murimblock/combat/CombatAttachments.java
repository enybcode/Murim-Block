package com.murimblock.combat;

import com.murimblock.Murimblock;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class CombatAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Murimblock.MOD_ID);

    static final Supplier<AttachmentType<CombatData>> PLAYER_COMBAT = ATTACHMENT_TYPES.register(
            "player_combat",
            () -> AttachmentType.builder(CombatData::initial)
                    .sync((holder, recipient) -> holder == recipient, CombatData.STREAM_CODEC)
                    .build()
    );

    private CombatAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
