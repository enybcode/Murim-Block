package com.murimblock.martial;

import com.murimblock.Murimblock;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class BloodCrystalAttachments {
    private static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES,Murimblock.MOD_ID);
    public static final Supplier<AttachmentType<BloodCrystalState>> STATE = TYPES.register("blood_crystal_state",
            () -> AttachmentType.builder(() -> BloodCrystalState.EMPTY)
                    .sync((holder,recipient) -> true,BloodCrystalState.STREAM_CODEC).build());
    private BloodCrystalAttachments() { }
    public static void register(IEventBus bus) { TYPES.register(bus); }
}
