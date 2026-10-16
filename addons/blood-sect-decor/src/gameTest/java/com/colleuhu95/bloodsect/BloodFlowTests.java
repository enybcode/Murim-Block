package com.colleuhu95.bloodsect;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BloodSectDecor.ID)
@PrefixGameTestTemplate(false)
public final class BloodFlowTests {
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void bloodFlowsAndRemainsInsideStoneChannel(GameTestHelper test) {
        for (int x=1; x<=9; x++) {
            test.setBlock(x,1,2,Blocks.SANDSTONE);
            test.setBlock(x,2,1,Blocks.SANDSTONE);
            test.setBlock(x,2,3,Blocks.SANDSTONE);
        }
        test.setBlock(1,2,2,Blocks.SANDSTONE);
        test.setBlock(9,2,2,Blocks.SANDSTONE);
        test.setBlock(2,2,2,BloodSectDecor.BLOOD_BLOCK.get());
        test.runAfterDelay(90, () -> {
            var source=test.getLevel().getFluidState(test.absolutePos(new BlockPos(2,2,2)));
            var flowing=test.getLevel().getFluidState(test.absolutePos(new BlockPos(5,2,2)));
            test.assertTrue(source.isSource(), "Blood source disappeared");
            test.assertTrue(flowing.getType()==BloodSectDecor.FLOWING_BLOOD.get(), "Blood did not flow down the channel");
            test.assertTrue(test.getLevel().getFluidState(test.absolutePos(new BlockPos(5,2,4))).isEmpty(), "Blood escaped the channel");
            test.assertTrue(BloodSectDecor.BLOOD.get().getFluidType().getLightLevel()==0, "Blood must not glow");
            test.assertTrue(BloodSectDecor.BLOOD_BLOCK.get().getStateDefinition().getPossibleStates().size()==16, "Unexpected blood block state palette");
            test.succeed();
        });
    }

    @GameTest(template = "secte", timeoutTicks = 260)
    public static void terraceBloodDoesNotFloodTheStairs(GameTestHelper test) {
        test.runAfterDelay(200, () -> {
            for (int x=-4; x<=4; x++) for (int z=-20; z<=16; z++) for(int y=0; y<=11; y++) {
                if(Math.abs(x)<3) continue;
                var fluid=test.getLevel().getFluidState(test.absolutePos(new BlockPos(x+48,y+4,z+57)));
                test.assertTrue(fluid.isEmpty(), "Blood escaped onto the walkway at "+x+","+y+","+z);
            }
            test.succeed();
        });
    }
}
