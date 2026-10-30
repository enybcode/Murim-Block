package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.client.MurimClientConfig;
import com.murimblock.registry.MurimAttachments;
import com.murimblock.registry.MurimBlocks;
import com.murimblock.registry.MurimEntities;
import com.murimblock.registry.MurimItems;
import com.murimblock.registry.MurimParticles;
import com.murimblock.registry.MurimWorldgen;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

/** Entry point of Murimblock V2. */
@Mod(Murimblock.MOD_ID)
public final class Murimblock {
    public static final String MOD_ID = "murimblock";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Murimblock(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, MurimClientConfig.SPEC);
        MurimAttachments.ATTACHMENTS.register(modBus);
        MurimBlocks.BLOCKS.register(modBus);
        MurimItems.ITEMS.register(modBus);
        MurimEntities.ENTITIES.register(modBus);
        MurimParticles.PARTICLES.register(modBus);
        MurimWorldgen.BIOME_SOURCES.register(modBus);
        MurimWorldgen.DENSITY_FUNCTIONS.register(modBus);
        MurimWorldgen.FEATURES.register(modBus);
        modBus.addListener(Murimblock::fillCreativeTabs);
        LOGGER.info("Murimblock V2 loaded");
    }

    private static void fillCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(MurimItems.MERIDIAN_PILL);
            event.accept(MurimItems.DESTINY_TALISMAN);
        }
    }
}
