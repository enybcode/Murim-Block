package com.murimblock;

import com.murimblock.command.MurimblockCommands;
import com.murimblock.cultivation.CultivationAttachments;
import com.murimblock.cultivation.CultivationEvents;
import com.murimblock.qi.QiAttachments;
import com.murimblock.qi.QiEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Entry point for Murimblock.
 */
@Mod(Murimblock.MOD_ID)
public final class Murimblock {
    public static final String MOD_ID = "murimblock";

    public Murimblock(IEventBus modEventBus) {
        CultivationAttachments.register(modEventBus);
        QiAttachments.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(CultivationEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(QiEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(QiEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(MurimblockCommands::onRegisterCommands);
    }
}
