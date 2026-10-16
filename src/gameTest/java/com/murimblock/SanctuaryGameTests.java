package com.murimblock;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.murimblock.item.MurimItems;
import com.murimblock.item.SectTokenItem;
import com.murimblock.world.block.MurimBlocks;
import com.murimblock.world.structure.SanctuaryBuilder;
import com.murimblock.world.structure.SanctuaryPlacement;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/** The Cloud Cliff Sanctuary and its sect token. Absolute coordinates far out at x 3000: one batch per test. */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SanctuaryGameTests {
    private static final int X = 3000, Z = 0, GROUND = 70, R = 30;

    private SanctuaryGameTests() { }

    private static void forEach(ServerLevel level, int y0, int y1, Consumer<BlockPos> action) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = X - R; x <= X + R; x++) for (int z = Z - R; z <= Z + R; z++) for (int y = y0; y <= y1; y++) action.accept(p.set(x, y, z));
    }

    private static int count(ServerLevel level, Predicate<BlockState> what) {
        int[] n = {0};
        forEach(level, GROUND - 2, GROUND + SanctuaryBuilder.HEIGHT + 30, p -> { if (what.test(level.getBlockState(p))) n[0]++; });
        return n[0];
    }

    @GameTest(template = "foundation_arena", batch = "sanctuary_build", timeoutTicks = 400)
    public static void sanctuaryRisesWithItsRoadHallsAndSignalFire(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int cx = (X - R - 16) >> 4; cx <= (X + R + 16) >> 4; cx++) for (int cz = (Z - R - 16) >> 4; cz <= (Z + R + 16) >> 4; cz++) level.setChunkForced(cx, cz, true);
        forEach(level, GROUND - 1, GROUND - 1, p -> level.setBlock(p, Blocks.STONE.defaultBlockState(), 2));
        try {
            SanctuaryBuilder builder = new SanctuaryBuilder(level, X, GROUND, Z, RandomSource.create(7));
            // Step by step, as in a live world (one small step per call): the result must be the full sanctuary.
            while (builder.buildSome(0)) { }
            int top = builder.top();
            // Fingerprint of every block of the build: unchanged by a refactoring that must not change the sanctuary.
            long[] print = {17};
            forEach(level, GROUND - 6, GROUND + SanctuaryBuilder.HEIGHT + 30, q -> print[0] = print[0] * 31 + Block.getId(level.getBlockState(q)));
            LogUtils.getLogger().info("SANCTUARY_FINGERPRINT {}", Long.toHexString(print[0]));
            int granite = count(level, s -> s.is(MurimBlocks.PALE_GRANITE.get()));
            int greenRoof = count(level, s -> s.is(Blocks.PRISMARINE_BRICK_STAIRS) || s.is(Blocks.PRISMARINE_BRICK_SLAB));
            int columns = count(level, s -> s.is(Blocks.STRIPPED_MANGROVE_LOG));
            int road = 0;
            boolean roadReachesTop = false;
            for (int y = GROUND; y <= top; y++) {
                int at = y;
                int here = count2(level, at, s -> s.is(Blocks.SPRUCE_PLANKS) || s.is(Blocks.SPRUCE_STAIRS));
                road += here;
                if (y >= top - 2 && here > 0) roadReachesTop = true;
            }
            boolean signal = false, bell = false;
            for (int dx = -2; dx <= 2; dx++) for (int dz = -1; dz <= 5; dz++) {
                BlockState s = level.getBlockState(new BlockPos(X + dx, top + 2, Z + dz));
                if (s.is(Blocks.CAMPFIRE) && s.getValue(CampfireBlock.SIGNAL_FIRE) && level.getBlockState(new BlockPos(X + dx, top + 1, Z + dz)).is(Blocks.HAY_BLOCK)) signal = true;
            }
            bell = count(level, s -> s.is(Blocks.BELL)) > 0;
            // The entrance: the road ends at the south and a gangway crosses into the court, nothing in the way.
            boolean gateOpen = true;
            for (int x = -1; x <= 1; x++) for (int dz = 19; dz <= 25; dz++) {
                BlockPos floor = new BlockPos(X + x, top, Z + dz);
                if (!level.getBlockState(floor).isSolid() || !level.getBlockState(floor.above()).isAir() || !level.getBlockState(floor.above(2)).isAir()) gateOpen = false;
            }
            // No gap under the roofs: the layer under the stair courses is closed (main hall, a side hall, a tower).
            boolean roofsClosed = level.getBlockState(new BlockPos(X, top + 10, Z - 10)).isSolid()
                    && level.getBlockState(new BlockPos(X - 12, top + 6, Z + 1)).isSolid()
                    && level.getBlockState(new BlockPos(X - 12, top + 8, Z + 10)).isSolid();
            // Every red column and tower stands on the court, none over the void.
            boolean grounded = true;
            for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
                BlockPos at = new BlockPos(X + x, top + 1, Z + z);
                var s = level.getBlockState(at);
                if ((s.is(Blocks.STRIPPED_MANGROVE_LOG) || s.is(Blocks.RED_TERRACOTTA) || s.is(Blocks.POLISHED_ANDESITE)) && level.getBlockState(at.below()).isAir()) grounded = false;
            }
            BlockPos foot = builder.foot();
            boolean footSafe = level.getBlockState(foot.below()).isSolid() && level.getBlockState(foot).isAir() && level.getBlockState(foot.above()).isAir();
            helper.assertTrue(top == GROUND + SanctuaryBuilder.HEIGHT, "Summit at " + top);
            helper.assertTrue(granite > 25000, "Spire too thin: " + granite + " granite");
            helper.assertTrue(greenRoof > 150, "Green glazed roofs missing: " + greenRoof);
            helper.assertTrue(columns > 60, "Vermilion columns missing: " + columns);
            helper.assertTrue(road > 300 && roadReachesTop, "Plank road too short or not reaching the summit (" + road + ")");
            helper.assertTrue(signal, "No signal fire (campfire on hay) in the court");
            helper.assertTrue(bell, "No bell in the bell tower");
            helper.assertTrue(gateOpen, "No open gangway from the road into the court at the south gate");
            helper.assertTrue(roofsClosed, "Gap left under a roof");
            helper.assertTrue(grounded, "A column or tower stands over the void at the summit");
            helper.assertTrue(footSafe, "Players would not arrive safely at the foot " + foot);
            helper.succeed();
        } finally {
            forEach(level, GROUND - 6, GROUND + SanctuaryBuilder.HEIGHT + 30, p -> { if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16); });
        }
    }

    private static int count2(ServerLevel level, int y, Predicate<BlockState> what) {
        int n = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = X - R; x <= X + R; x++) for (int z = Z - R; z <= Z + R; z++) if (what.test(level.getBlockState(p.set(x, y, z)))) n++;
        return n;
    }

    @GameTest(template = "foundation_arena", batch = "sanctuary_locate")
    public static void vanillaLocateFindsTheSanctuary(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        SanctuaryPlacement data = SanctuaryPlacement.get(server.overworld());
        var source = server.createCommandSourceStack().withLevel(server.overworld()).withPosition(Vec3.ZERO).withSuppressedOutput();
        var dispatcher = server.getCommands().getDispatcher();
        boolean notFoundWithoutSanctuary = false;
        try {
            dispatcher.execute("locate structure murimblock:cloud_cliff_sanctuary", source);
        } catch (CommandSyntaxException e) {
            notFoundWithoutSanctuary = true; // "Could not find a structure..." in a flat world
        }
        data.record(new BlockPos(1200, 90, -800), new BlockPos(1200, 91, -784), 160);
        try {
            int found = dispatcher.execute("locate structure murimblock:cloud_cliff_sanctuary", source);
            var direct = server.overworld().getChunkSource().getGenerator().findNearestMapStructure(server.overworld(),
                    HolderSet.direct(server.registryAccess().registryOrThrow(Registries.STRUCTURE)
                            .getHolderOrThrow(SanctuaryPlacement.STRUCTURE)), BlockPos.ZERO, 100, false);
            helper.assertTrue(notFoundWithoutSanctuary, "/locate found a sanctuary in a world without one");
            // /locate returns the distance in blocks.
            helper.assertTrue(found > 0 && direct != null && direct.getFirst().getX() == 1200 && direct.getFirst().getZ() == -800,
                    "/locate did not find the sanctuary: " + found + " " + direct);
            helper.succeed();
        } catch (CommandSyntaxException e) {
            helper.fail("/locate structure failed: " + e.getMessage());
        } finally {
            data.clear();
        }
    }

    /**
     * The sect's token is no longer given at the first login: the Alliance merchant sells it, once per restock, for a
     * stack of taels and 32 blocks of gold; its needle points to the sanctuary.
     */
    @GameTest(template = "foundation_arena", batch = "sanctuary_token")
    public static void theTokenIsBoughtDearAndPointsToTheSanctuary(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        SanctuaryPlacement data = SanctuaryPlacement.get(overworld);
        // Flat test world: no sanctuary was built on start.
        helper.assertTrue(data.centre() == null, "A sanctuary was built in a flat world");
        data.record(new BlockPos(1200, 90, -800), new BlockPos(1200, 91, -784), 160);
        var player = player(helper);
        helper.assertTrue(player.getInventory().countItem(MurimItems.SECT_TOKEN.get()) == 0, "A new player was given the sect's token");
        var offer = com.murimblock.mob.AllianceMerchantEntity.wares().stream().filter(o -> o.getResult().is(MurimItems.SECT_TOKEN.get())).findFirst();
        helper.assertTrue(offer.isPresent(), "The Alliance merchant does not sell the sect's token");
        var sold = offer.get();
        helper.assertTrue(sold.getBaseCostA().is(MurimItems.SILVER_TAEL.get()) && sold.getBaseCostA().getCount() == com.murimblock.mob.AllianceMerchantEntity.SECT_TOKEN_TAELS
                && sold.getCostB().is(net.minecraft.world.item.Items.GOLD_BLOCK) && sold.getCostB().getCount() == com.murimblock.mob.AllianceMerchantEntity.SECT_TOKEN_GOLD_BLOCKS
                && sold.getMaxUses() == 1, "The token's price is not a stack of taels and 32 gold blocks, once: " + sold.getBaseCostA() + " + " + sold.getCostB());
        player.getInventory().clearContent();
        // Inventory.add moves the stack into a slot (the one passed is emptied): tick the one in the inventory.
        player.getInventory().add(sold.getResult().copy());
        ItemStack found = ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().items) if (stack.is(MurimItems.SECT_TOKEN.get())) found = stack;
        ItemStack held = found;
        helper.assertTrue(!held.isEmpty(), "The bought token is not in the inventory");
        // The needle refreshes once a second: tick the token for a little over a second of game time.
        for (int t = 1; t <= 25; t++) helper.runAfterDelay(t, () -> held.inventoryTick(overworld, player, 0, true));
        helper.runAfterDelay(26, () -> {
            try {
                var tracker = held.get(DataComponents.LODESTONE_TRACKER);
                helper.assertTrue(tracker != null && tracker.target().isPresent() && tracker.target().get().pos().equals(new BlockPos(1200, 160, -800)),
                        "Token needle not on the sanctuary: " + tracker);
                helper.assertTrue(SectTokenItem.compass(0, -100).equals("north") && SectTokenItem.compass(100, 0).equals("east")
                        && SectTokenItem.compass(70, 70).equals("south_east") && SectTokenItem.compass(-70, -70).equals("north_west"), "Wrong compass winds");
                helper.succeed();
            } finally {
                data.clear();
                cleanup(player);
            }
        });
    }
}
