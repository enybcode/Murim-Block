package com.murimblock.world.raven;

import com.murimblock.Murimblock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Raven Art's blocks: the doors of shadow of the Raven Halls (no item) and the raven's perch. */
public final class RavenBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Murimblock.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Murimblock.MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Murimblock.MOD_ID);
    /** The endless castle that fills the Raven Halls (placed in every chunk by its biome). */
    public static final DeferredHolder<Feature<?>, RavenCastleFeature> CASTLE = FEATURES.register("raven_castle", () -> new RavenCastleFeature());

    /** A door of shadow: walked through, unbreakable, a dim amber light. */
    public static final DeferredBlock<RavenDoorBlock> RAVEN_DOOR = BLOCKS.register("raven_door", () -> new RavenDoorBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK).strength(-1F, 3_600_000F).noCollission().noOcclusion().lightLevel(s -> 6).noLootTable()
            .pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, p, t) -> false).sound(SoundType.AMETHYST)));
    /** The raven's perch: set down by a student of the art, a way through the halls. */
    public static final DeferredBlock<RavenPerchBlock> RAVEN_PERCH = BLOCKS.register("raven_perch", () -> new RavenPerchBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK).strength(3F, 6F).noOcclusion().requiresCorrectToolForDrops().sound(SoundType.POLISHED_DEEPSLATE)));
    public static final DeferredItem<BlockItem> RAVEN_PERCH_ITEM = ITEMS.register("raven_perch", () -> new BlockItem(RAVEN_PERCH.get(), new Item.Properties()));

    private RavenBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        FEATURES.register(bus);
    }
}
