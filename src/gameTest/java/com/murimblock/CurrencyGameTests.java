package com.murimblock;

import com.murimblock.item.MurimItems;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Murim's money (docs/items/currency.md): copper cash, strings of cash, silver taels, gold yuanbao, each worth nine
 * of the one below; nine change into one at a crafting table and one back into nine. Bandits carry small change.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CurrencyGameTests {
    private CurrencyGameTests() { }

    @GameTest(template = "foundation_arena", batch = "currency")
    public static void nineChangeIntoOneAndOneBackIntoNine(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipes = level.getRecipeManager();
        List<Item> ladder = List.of(MurimItems.COPPER_CASH.get(), MurimItems.CASH_STRING.get(), MurimItems.SILVER_TAEL.get(), MurimItems.GOLD_YUANBAO.get());
        for (int i = 0; i + 1 < ladder.size(); i++) {
            Item small = ladder.get(i), big = ladder.get(i + 1);
            NonNullList<ItemStack> nine = NonNullList.withSize(9, new ItemStack(small));
            var up = recipes.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, nine), level);
            helper.assertTrue(up.isPresent(), "Nine " + small + " make nothing");
            ItemStack made = up.get().value().assemble(CraftingInput.of(3, 3, nine), level.registryAccess());
            helper.assertTrue(made.is(big) && made.getCount() == 1, "Nine " + small + " make " + made);
            var down = recipes.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(1, 1, List.of(new ItemStack(big))), level);
            helper.assertTrue(down.isPresent(), "One " + big + " changes into nothing");
            ItemStack change = down.get().value().assemble(CraftingInput.of(1, 1, List.of(new ItemStack(big))), level.registryAccess());
            helper.assertTrue(change.is(small) && change.getCount() == 9, "One " + big + " changes into " + change);
        }
        helper.succeed();
    }

    /** Killed by a player, a mountain bandit leaves a little copper cash: 2 to 8 coins (none for anything else). */
    @GameTest(template = "foundation_arena", batch = "currency_bandit")
    public static void banditsCarrySmallChange(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = com.murimblock.testing.GameTestPlayers.player(helper);
        var bandit = helper.spawn(com.murimblock.mob.MurimEntities.MOUNTAIN_BANDIT.get(), new net.minecraft.core.BlockPos(2, 1, 2));
        var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "entities/mountain_bandit")));
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, bandit)
                .withParameter(LootContextParams.ORIGIN, bandit.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(player))
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player).withParameter(LootContextParams.ATTACKING_ENTITY, player).create(LootContextParamSets.ENTITY);
        int least = Integer.MAX_VALUE, most = 0;
        for (int i = 0; i < 200; i++) {
            int coins = table.getRandomItems(params).stream().filter(s -> s.is(MurimItems.COPPER_CASH.get())).mapToInt(ItemStack::getCount).sum();
            least = Math.min(least, coins);
            most = Math.max(most, coins);
        }
        bandit.discard();
        com.murimblock.testing.GameTestPlayers.cleanup(player);
        helper.assertTrue(least >= 2 && most <= 8 && most > least, "A bandit carries " + least + " to " + most + " copper cash, not 2 to 8");
        helper.succeed();
    }
}
