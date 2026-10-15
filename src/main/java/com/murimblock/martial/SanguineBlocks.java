package com.murimblock.martial;

import com.murimblock.Murimblock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Blood Iceberg's crystal: plain, and veined with glowing blood. No items; nothing drops (BloodIceberg). */
public final class SanguineBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Murimblock.MOD_ID);
    public static final DeferredBlock<BloodIcebergBlock> BLOOD_CRYSTAL =
            BLOCKS.register("blood_crystal_block", () -> new BloodIcebergBlock(properties(0)));
    public static final DeferredBlock<BloodIcebergBlock> BLOOD_CRYSTAL_VEIN =
            BLOCKS.register("blood_crystal_vein", () -> new BloodIcebergBlock(properties(9)));

    private SanguineBlocks() {
    }

    private static BlockBehaviour.Properties properties(int light) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.CRIMSON_NYLIUM).strength(2F, 6F).sound(SoundType.AMETHYST)
                .lightLevel(s -> light).noLootTable().pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, p, t) -> false);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
