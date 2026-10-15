package com.murimblock;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightContentPolicy;
import net.dawn_day.gameasset.animation.DawnDayAnimations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * Dawn Day is loaded for its animations only (docs/DAWN_DAY.md): its animations play through Epic Fight, and none of
 * its items reaches a player, whether dropped, worn by a mob, rolled into a chest by its loot modifiers or put there,
 * and no player carries one of its skills.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DawnDayGameTests {
    private DawnDayGameTests() { }

    private static Item item(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(EpicFightContentPolicy.DAWN_DAY, path);
        if (!BuiltInRegistries.ITEM.containsKey(id)) throw new IllegalStateException("Dawn Day has no item " + id);
        return BuiltInRegistries.ITEM.get(id);
    }

    @GameTest(template = "foundation_arena", batch = "dawn_day")
    public static void itsAnimationsAreBuiltAndPlay(GameTestHelper helper) {
        helper.assertTrue(DawnDayAnimations.HALBERD_AUTO1 != null && DawnDayAnimations.HALBERD_AUTO1.get() != null, "Halberd animation not built");
        helper.assertTrue(DawnDayAnimations.BATTLESTAFF_AUTO1 != null && DawnDayAnimations.BATTLESTAFF_AUTO1.get() != null, "Battle staff animation not built");
        Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        zombie.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        zombie.setNoAi(true);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        helper.getLevel().addFreshEntity(zombie);
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(zombie, LivingEntityPatch.class);
        helper.assertTrue(patch != null, "Zombie has no Epic Fight patch");
        patch.playAnimationSynchronized(DawnDayAnimations.HALBERD_AUTO1, 0.0F);
        helper.runAfterDelay(3, () -> {
            var playing = patch.getAnimator().getPlayerFor(null).getRealAnimation();
            helper.assertTrue(playing.registryName().equals(DawnDayAnimations.HALBERD_AUTO1.registryName()),
                    "The zombie plays " + playing.registryName() + " instead of Dawn Day's halberd strike");
            zombie.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "dawn_day")
    public static void itsDroppedItemsNeverAppear(GameTestHelper helper) {
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        var drops = new java.util.ArrayList<ItemEntity>();
        for (String path : new String[]{"iron_halberd", "iron_battlestaff", "milady", "iron_knife"}) {
            helper.assertTrue(EpicFightContentPolicy.blocked(new ItemStack(item(path))), "Not retired: " + path);
            ItemEntity drop = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(item(path)));
            helper.getLevel().addFreshEntity(drop);
            drops.add(drop);
        }
        ItemEntity sword = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(Items.IRON_SWORD));
        helper.getLevel().addFreshEntity(sword);
        helper.runAfterDelay(2, () -> {
            // What counts is what is in the world: none of its drops, the vanilla one still there.
            for (ItemEntity drop : drops) {
                helper.assertTrue(helper.getLevel().getEntity(drop.getId()) == null, "Dawn Day's " + drop.getItem() + " lies in the world");
            }
            helper.assertTrue(helper.getLevel().getEntity(sword.getId()) == sword, "A vanilla drop was refused");
            sword.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "dawn_day")
    public static void mobsLoseItsWeapons(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        Zombie born = EntityType.ZOMBIE.create(helper.getLevel());
        born.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        born.setNoAi(true);
        born.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item("iron_halberd")));
        helper.getLevel().addFreshEntity(born);
        helper.assertTrue(born.getMainHandItem().isEmpty(), "A mob joined holding Dawn Day's halberd");
        // Given one later: taken away when its equipment changes.
        born.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(item("iron_knife")));
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(born.getOffhandItem().isEmpty(), "A mob kept Dawn Day's knife");
            born.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "dawn_day", timeoutTicks = 200)
    public static void itsChestLootIsEmptiedWhenOpened(GameTestHelper helper) {
        // Its loot modifiers roll its items into vanilla chests: roll an ancient city chest until one comes.
        var level = helper.getLevel();
        var table = level.getServer().reloadableRegistries().getLootTable(
                ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/ancient_city")));
        BlockPos chestPos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
        int rolled = 0;
        for (int i = 0; i < 400 && rolled == 0; i++) {
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(chestPos))
                    .create(LootContextParamSets.CHEST);
            for (ItemStack stack : table.getRandomItems(params)) if (EpicFightContentPolicy.blocked(stack)) rolled++;
        }
        // Whether or not a roll brought one, put two in the chest as its modifiers would.
        chest.setItem(0, new ItemStack(item("iron_halberd")));
        chest.setItem(5, new ItemStack(item("iron_battlestaff")));
        chest.setItem(9, new ItemStack(Items.DIAMOND));
        ServerPlayer player = player(helper);
        player.openMenu(chest);
        helper.runAfterDelay(2, () -> {
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                helper.assertFalse(EpicFightContentPolicy.blocked(chest.getItem(slot)), "Dawn Day's item stayed in an opened chest");
            }
            helper.assertTrue(chest.getItem(9).is(Items.DIAMOND), "The chest's own loot was taken too");
            player.closeContainer();
            cleanup(player);
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "dawn_day", timeoutTicks = 120)
    public static void playersCarryNoneOfItsItemsOrSkills(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.getInventory().setItem(10, new ItemStack(item("milady")));
        player.getEnderChestInventory().setItem(3, new ItemStack(item("iron_knife")));
        // A mock player gets no network ticks: run one full player tick on a sweep tick (once a second).
        player.tickCount = 40;
        player.doTick();
        helper.runAfterDelay(1, () -> {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                helper.assertFalse(EpicFightContentPolicy.blocked(player.getInventory().getItem(i)), "Dawn Day's item stayed in the inventory");
            }
            helper.assertFalse(EpicFightContentPolicy.blocked(player.getEnderChestInventory().getItem(3)), "Dawn Day's item stayed in the ender chest");
            var patch = EpicFightBridge.patch(player);
            for (var container : patch.getPlayerSkills().listSkillContainers().toList()) {
                var skill = container.getSkill();
                helper.assertTrue(skill == null || !skill.getRegistryName().getNamespace().equals(EpicFightContentPolicy.DAWN_DAY),
                        "A player carries Dawn Day's skill " + (skill == null ? "" : skill.getRegistryName()));
            }
            cleanup(player);
            helper.succeed();
        });
    }
}
