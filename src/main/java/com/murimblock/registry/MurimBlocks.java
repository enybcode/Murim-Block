package com.murimblock.registry;

import com.murimblock.Murimblock;
import com.murimblock.shenfa.FracturedGroundBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Murimblock blocks. */
public final class MurimBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Murimblock.MOD_ID);

    /**
     * Ground that a blow has fractured, on the client only: it stands in for the real block while Shenfa draws that
     * block tilted and lifted (client.GroundFractures). Never placed in a saved world.
     */
    public static final DeferredBlock<Block> FRACTURED_GROUND = BLOCKS.register("fractured_ground",
            () -> new FracturedGroundBlock(BlockBehaviour.Properties.of().noOcclusion().noLootTable()
                    .strength(-1, 3_600_000).isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false).pushReaction(PushReaction.BLOCK)));

    private MurimBlocks() {
    }
}
