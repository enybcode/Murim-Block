package com.murimblock;

import com.murimblock.world.plant.HerbItem;
import com.murimblock.world.plant.MurimFlora;
import com.murimblock.world.plant.PlantCatalog;
import com.murimblock.world.plant.PlantCropBlock;
import com.murimblock.world.plant.PlantCropBlockEntity;
import com.murimblock.world.plant.PlantGrowth;
import com.murimblock.world.plant.PlantSeedItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Murim's plants in game (docs/cultivation/plants.md): what a plant grows from is planted on its soil only (on still
 * water for the floating ones); a seedling grows in light; a mature plant harvested gives its herb with its purity and is
 * kept (flowers, berries) or pulled up (roots); a wild herb broken gives its herb, not its block.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlantGameTests {
    private PlantGameTests() { }

    private static final BlockPos SOIL = new BlockPos(4, 1, 4);

    private static PlantCatalog.Species species(String id) {
        return PlantCatalog.byId(id);
    }

    /** Ground of `soil` with light (glowstone beside it): returns where a plant goes, above it. */
    private static BlockPos bed(GameTestHelper helper, net.minecraft.world.level.block.Block soil) {
        helper.setBlock(SOIL, soil);
        helper.setBlock(SOIL.above(), Blocks.AIR);
        helper.setBlock(SOIL.above().east(), Blocks.GLOWSTONE);
        return helper.absolutePos(SOIL.above());
    }

    private static List<ItemStack> dropsNear(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream().map(ItemEntity::getItem).toList();
    }

    private static void clearDrops(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).forEach(ItemEntity::discard);
    }

    @GameTest(template = "foundation_arena", batch = "plant_sow")
    public static void seedsTakeTheirSoilOnly(GameTestHelper helper) {
        PlantCatalog.Species chrys = species("chrysanthemum"), poppy = species("blue_poppy");
        // On stone: the mountain's poppy roots, the valley's chrysanthemum does not.
        BlockPos top = bed(helper, Blocks.STONE);
        helper.assertTrue(!PlantSeedItem.plant(chrys, helper.getLevel(), top), "A chrysanthemum rooted in bare stone");
        helper.assertTrue(PlantSeedItem.plant(poppy, helper.getLevel(), top), "A blue poppy did not root in mountain stone");
        top = bed(helper, Blocks.GRASS_BLOCK);
        helper.assertTrue(PlantSeedItem.plant(chrys, helper.getLevel(), top), "Chrysanthemum seeds did not root in grass");
        BlockState state = helper.getLevel().getBlockState(top);
        helper.assertTrue(state.is(MurimFlora.CROP.get()) && PlantCropBlock.species(state) == chrys && state.getValue(PlantCropBlock.AGE) == 0, "Not a chrysanthemum seedling: " + state);
        helper.assertTrue(helper.getLevel().getBlockEntity(top) instanceof PlantCropBlockEntity, "No block entity on the seedling");
        // A player uses the seeds on the top of the soil.
        helper.setBlock(SOIL.above(), Blocks.AIR);
        ServerPlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimFlora.seed(chrys), 3));
        var hit = new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(SOIL)), Direction.UP, helper.absolutePos(SOIL), false);
        player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(helper.getLevel().getBlockState(top).is(MurimFlora.CROP.get()), "Using the seeds planted nothing");
        helper.assertTrue(player.getMainHandItem().getCount() == 2, "Planting did not use a seed: " + player.getMainHandItem().getCount());
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "plant_grow")
    public static void aSeedlingGrowsToMaturityAndItsPurityRises(GameTestHelper helper) {
        PlantCatalog.Species chrys = species("chrysanthemum");
        BlockPos top = bed(helper, Blocks.GRASS_BLOCK);
        PlantSeedItem.plant(chrys, helper.getLevel(), top);
        // A roll of 0 always succeeds when the conditions allow growth at all.
        for (int age = 0; age < PlantGrowth.MATURE; age++) {
            helper.assertTrue(PlantCropBlock.grow(helper.getLevel().getBlockState(top), helper.getLevel(), top, 0.0), "It did not grow from stage " + age);
        }
        helper.assertTrue(helper.getLevel().getBlockState(top).getValue(PlantCropBlock.AGE) == PlantGrowth.MATURE, "Not mature after two stages");
        int purity = ((PlantCropBlockEntity) helper.getLevel().getBlockEntity(top)).purity();
        helper.assertTrue(purity > PlantGrowth.START_PURITY, "Purity did not rise: " + purity);
        // Off Murim land: never spirit maturity.
        helper.assertTrue(!PlantCropBlock.grow(helper.getLevel().getBlockState(top), helper.getLevel(), top, 0.0), "Spirit maturity off Murim land");
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "plant_dark")
    public static void inTheDarkNothingGrows(GameTestHelper helper) {
        PlantCatalog.Species chrys = species("chrysanthemum");
        helper.setBlock(SOIL, Blocks.GRASS_BLOCK);
        BlockPos top = helper.absolutePos(SOIL.above());
        helper.setBlock(SOIL.above(), Blocks.AIR);
        // Shut in stone all round: no light.
        for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 2; dy++) for (int dz = -1; dz <= 1; dz++)
            if (dx != 0 || dy != 1 || dz != 0) helper.setBlock(SOIL.offset(dx, dy, dz), dy == 0 && dx == 0 && dz == 0 ? Blocks.GRASS_BLOCK : Blocks.STONE);
        PlantSeedItem.plant(chrys, helper.getLevel(), top);
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(!PlantCropBlock.grow(helper.getLevel().getBlockState(top), helper.getLevel(), top, 0.0), "It grew in the dark");
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "plant_harvest")
    public static void aMatureHerbIsHarvestedAndKept(GameTestHelper helper) {
        PlantCatalog.Species chrys = species("chrysanthemum");
        BlockPos top = bed(helper, Blocks.GRASS_BLOCK);
        PlantSeedItem.plant(chrys, helper.getLevel(), top);
        helper.getLevel().setBlock(top, helper.getLevel().getBlockState(top).setValue(PlantCropBlock.AGE, PlantGrowth.MATURE), 3);
        ((PlantCropBlockEntity) helper.getLevel().getBlockEntity(top)).setPurityForTests(77);
        clearDrops(helper, top);
        ServerPlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.getLevel().getBlockState(top).useWithoutItem(helper.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(top), Direction.UP, top, false));
        List<ItemStack> drops = dropsNear(helper, top);
        ItemStack herb = drops.stream().filter(s -> s.is(MurimFlora.herb(chrys))).findFirst().orElse(ItemStack.EMPTY);
        helper.assertTrue(!herb.isEmpty(), "No chrysanthemum flower harvested: " + drops);
        helper.assertTrue(HerbItem.purity(herb) == 77 && HerbItem.maturity(herb).equals("mature"), "The herb's purity or maturity is wrong: " + HerbItem.purity(herb) + " " + HerbItem.maturity(herb));
        BlockState after = helper.getLevel().getBlockState(top);
        helper.assertTrue(after.is(MurimFlora.CROP.get()) && after.getValue(PlantCropBlock.AGE) == PlantGrowth.YOUNG, "The chrysanthemum was not kept, young again: " + after);
        cleanup(player);
        clearDrops(helper, top);
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "plant_uproot")
    public static void aRootIsPulledUpWhole(GameTestHelper helper) {
        PlantCatalog.Species ginseng = species("ginseng_root");
        BlockPos top = bed(helper, Blocks.GRASS_BLOCK);
        PlantSeedItem.plant(ginseng, helper.getLevel(), top);
        helper.getLevel().setBlock(top, helper.getLevel().getBlockState(top).setValue(PlantCropBlock.AGE, PlantGrowth.MATURE), 3);
        clearDrops(helper, top);
        ServerPlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.getLevel().getBlockState(top).useWithoutItem(helper.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(top), Direction.UP, top, false));
        helper.assertTrue(dropsNear(helper, top).stream().anyMatch(s -> s.is(MurimFlora.herb(ginseng))), "No ginseng root harvested");
        helper.assertTrue(helper.getLevel().getBlockState(top).isAir(), "The ginseng was not pulled up");
        cleanup(player);
        clearDrops(helper, top);
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "plant_wild")
    public static void aWildHerbGivesItsHerb(GameTestHelper helper) {
        PlantCatalog.Species chrys = species("chrysanthemum");
        helper.setBlock(SOIL, Blocks.GRASS_BLOCK);
        helper.setBlock(SOIL.above(), MurimFlora.wildBlock(chrys));
        BlockPos top = helper.absolutePos(SOIL.above());
        clearDrops(helper, top);
        helper.getLevel().destroyBlock(top, true);
        List<ItemStack> drops = dropsNear(helper, top);
        helper.assertTrue(drops.stream().anyMatch(s -> s.is(MurimFlora.herb(chrys))), "A wild chrysanthemum gave no flower: " + drops);
        helper.assertTrue(drops.stream().noneMatch(s -> s.is(MurimFlora.wildItem(chrys))), "A wild chrysanthemum gave its block without shears");
        clearDrops(helper, top);
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "plant_water")
    public static void aWaterLilyIsPlantedOnStillWater(GameTestHelper helper) {
        PlantCatalog.Species lily = species("water_lily");
        helper.setBlock(SOIL, Blocks.WATER);
        helper.setBlock(SOIL.above(), Blocks.AIR);
        BlockPos top = helper.absolutePos(SOIL.above());
        helper.assertTrue(PlantSeedItem.plant(lily, helper.getLevel(), top), "Water lily rhizome did not take on water");
        helper.setBlock(SOIL, Blocks.GRASS_BLOCK);
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(!helper.getLevel().getBlockState(top).is(MurimFlora.CROP.get()), "The water lily stayed when its water was gone");
            helper.succeed();
        });
    }
}
