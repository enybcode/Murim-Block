package com.murimblock;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Entry point of Murimblock V2. */
@Mod(Murimblock.MOD_ID)
public final class Murimblock {
    public static final String MOD_ID = "murimblock";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Murimblock(IEventBus modBus) {
        LOGGER.info("Murimblock V2 loaded");
    }
}
