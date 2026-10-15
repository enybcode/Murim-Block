package com.murimblock.martial;

import com.murimblock.Murimblock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Blood Iceberg's blood temple (BloodIceberg), in the style Enzo chose: red lacquered stone and gold. Rough stone,
 * bricks, polished stone, the gold seal, gold crystal and lanterns; and blood, a liquid (SanguineFluids). No items;
 * nothing drops; each removes itself when the arena's time is up.
 */
public final class SanguineBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Murimblock.MOD_ID);
    public static final DeferredBlock<BloodIcebergBlock> TEMPLE_ROCK = temple("blood_temple_rock", MapColor.TERRACOTTA_RED, SoundType.STONE, 0);
    public static final DeferredBlock<BloodIcebergBlock> TEMPLE_BRICKS = temple("blood_temple_bricks", MapColor.TERRACOTTA_RED, SoundType.STONE, 0);
    public static final DeferredBlock<BloodIcebergBlock> TEMPLE_POLISHED = temple("blood_temple_polished", MapColor.TERRACOTTA_RED, SoundType.STONE, 0);
    public static final DeferredBlock<BloodIcebergBlock> TEMPLE_SEAL = temple("blood_temple_seal", MapColor.GOLD, SoundType.STONE, 4);
    public static final DeferredBlock<BloodIcebergBlock> TEMPLE_GOLD = temple("blood_temple_gold", MapColor.GOLD, SoundType.AMETHYST, 6);
    public static final DeferredBlock<BloodIcebergBlock> TEMPLE_LANTERN = temple("blood_temple_lantern", MapColor.GOLD, SoundType.LANTERN, 15);
    /** Blood, the liquid of the arena's channel and falls. */
    public static final DeferredBlock<BloodLiquidBlock> BLOOD = BLOCKS.register("blood", () -> new BloodLiquidBlock(SanguineFluids.BLOOD.get(),
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(MapColor.CRIMSON_NYLIUM).noLootTable()));

    private SanguineBlocks() {
    }

    private static DeferredBlock<BloodIcebergBlock> temple(String name, MapColor color, SoundType sound, int light) {
        // Unbreakable, by hand or by explosion; it goes when its time is up (BloodIceberg).
        return BLOCKS.register(name, () -> new BloodIcebergBlock(BlockBehaviour.Properties.of().mapColor(color).strength(-1F, 3_600_000F).sound(sound)
                .lightLevel(s -> light).noLootTable().pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, p, t) -> false)));
    }

    public static void register(IEventBus bus) {
        SanguineFluids.register(bus);
        BLOCKS.register(bus);
    }
}
