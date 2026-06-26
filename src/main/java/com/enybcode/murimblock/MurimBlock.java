package com.enybcode.murimblock;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(MurimBlock.MOD_ID)
public final class MurimBlock {
    public static final String MOD_ID = "murimblock";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MurimBlock(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Murim Block loaded.");
    }
}
