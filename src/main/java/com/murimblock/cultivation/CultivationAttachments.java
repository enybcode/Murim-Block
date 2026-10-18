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

    static final Supplier<AttachmentType<CultivationJournal>> PLAYER_JOURNAL = ATTACHMENT_TYPES.register(
            "player_cultivation_journal",
            () -> AttachmentType.builder(() -> CultivationJournal.EMPTY)
                    .serialize(CultivationJournal.CODEC)
                    .copyOnDeath()
                    .sync((holder, recipient) -> holder == recipient, CultivationJournal.STREAM_CODEC)
                    .build()
    );

    /** The cultivation methods known and practised (MethodData): saved, kept through death, sent to the player alone. */
    static final Supplier<AttachmentType<MethodData>> PLAYER_METHOD = ATTACHMENT_TYPES.register(
            "player_cultivation_method",
            () -> AttachmentType.builder(() -> MethodData.INITIAL)
                    .serialize(MethodData.CODEC)
                    .copyOnDeath()
                    .sync((holder, recipient) -> holder == recipient, MethodData.STREAM_CODEC)
                    .build()
    );

    /** Seen by every client tracking the player (meditation, aura, main style); not saved, rebuilt at login. */
    static final Supplier<AttachmentType<AuraState>> PLAYER_AURA = ATTACHMENT_TYPES.register(
            "player_aura",
            () -> AttachmentType.builder(() -> AuraState.NONE)
                    .sync((holder, recipient) -> true, AuraState.STREAM_CODEC)
                    .build()
    );

    /** A journey into the inner world (where the body was left): saved, never sent. */
    public static final Supplier<AttachmentType<com.murimblock.cultivation.inner.InnerSession>> PLAYER_INNER_SESSION = ATTACHMENT_TYPES.register(
            "player_inner_session",
            () -> AttachmentType.builder(() -> com.murimblock.cultivation.inner.InnerSession.NONE)
                    .serialize(com.murimblock.cultivation.inner.InnerSession.CODEC)
                    .build()
    );

    private CultivationAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
