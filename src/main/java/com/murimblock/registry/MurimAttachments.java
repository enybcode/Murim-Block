package com.murimblock.registry;

import com.murimblock.Murimblock;
import com.murimblock.combat.Fighter;
import com.murimblock.cultivation.Cultivation;
import com.murimblock.cultivation.CultivationCodecs;
import com.murimblock.innerworld.InnerWorldState;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data attached to entities. Registry names are saved IDs: keep them. */
public final class MurimAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Murimblock.MOD_ID);

    public static final Supplier<AttachmentType<Cultivation>> CULTIVATION = ATTACHMENTS.register("cultivation",
            () -> AttachmentType.builder(Cultivation::new).serialize(CultivationCodecs.SAVED).copyOnDeath().build());

    public static final Supplier<AttachmentType<InnerWorldState>> INNER_WORLD = ATTACHMENTS.register("inner_world",
            () -> AttachmentType.builder(InnerWorldState::new).serialize(InnerWorldState.CODEC).build());

    /** Not saved: a fight does not survive a reconnection. */
    public static final Supplier<AttachmentType<Fighter>> FIGHTER = ATTACHMENTS.register("fighter",
            () -> AttachmentType.builder(Fighter::new).build());

    private MurimAttachments() {
    }
}
