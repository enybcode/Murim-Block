package com.murimblock.alchemy;

import com.murimblock.Murimblock;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The alchemy in game (docs/cultivation/alchemy.md): the pill furnace, its screen, its pills, the manual and the scrolls. */
public final class AlchemyItems {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Murimblock.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Murimblock.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Murimblock.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Murimblock.MOD_ID);

    public static final DeferredBlock<PillFurnaceBlock> FURNACE = BLOCKS.register("alchemy_furnace", () -> new PillFurnaceBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.GOLD).strength(3.5F, 6.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(PillFurnaceBlock.LIT) ? 13 : 0)));
    public static final DeferredItem<BlockItem> FURNACE_ITEM = ITEMS.register("alchemy_furnace", () -> new BlockItem(FURNACE.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PillFurnaceBlockEntity>> FURNACE_ENTITY = BLOCK_ENTITIES.register("alchemy_furnace",
            () -> BlockEntityType.Builder.of(PillFurnaceBlockEntity::new, FURNACE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<PillFurnaceMenu>> FURNACE_MENU = MENUS.register("alchemy_furnace",
            () -> IMenuTypeExtension.create(PillFurnaceMenu::new));

    public static final DeferredItem<Item> MANUAL = ITEMS.register("alchemy_manual", () -> new AlchemyBookItem(
            List.of("qi_gathering_pill", "realm_breaking_pill", "healing_decoction", "qi_decoction", "purifying_brew"), false,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> MARTIAL_ELIXIR_SCROLL = ITEMS.register("martial_elixir_recipe", () -> new AlchemyBookItem(
            List.of("martial_elixir"), true, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> SOUL_SOOTHING_SCROLL = ITEMS.register("soul_soothing_pill_recipe", () -> new AlchemyBookItem(
            List.of("soul_soothing_pill"), true, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> HEALING_DECOCTION = pill("healing_decoction", AlchemyPillItem.Kind.HEALING_DECOCTION, Rarity.COMMON);
    public static final DeferredItem<Item> QI_DECOCTION = pill("qi_decoction", AlchemyPillItem.Kind.QI_DECOCTION, Rarity.COMMON);
    public static final DeferredItem<Item> PURIFYING_BREW = pill("purifying_brew", AlchemyPillItem.Kind.PURIFYING_BREW, Rarity.UNCOMMON);
    public static final DeferredItem<Item> MARTIAL_ELIXIR = pill("martial_elixir", AlchemyPillItem.Kind.MARTIAL_ELIXIR, Rarity.RARE);
    public static final DeferredItem<Item> SOUL_SOOTHING_PILL = pill("soul_soothing_pill", AlchemyPillItem.Kind.SOUL_SOOTHING_PILL, Rarity.EPIC);
    public static final DeferredItem<Item> PILL_RESIDUE = ITEMS.register("pill_residue", () -> new Item(new Item.Properties()));

    private AlchemyItems() {
    }

    private static DeferredItem<Item> pill(String name, AlchemyPillItem.Kind kind, Rarity rarity) {
        return ITEMS.register(name, () -> new AlchemyPillItem(kind, new Item.Properties().stacksTo(16).rarity(rarity)));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
    }

    /** In the main tab, after the pills. */
    public static List<Supplier<? extends Item>> creativeItems() {
        return List.of(FURNACE_ITEM, MANUAL, MARTIAL_ELIXIR_SCROLL, SOUL_SOOTHING_SCROLL, HEALING_DECOCTION, QI_DECOCTION, PURIFYING_BREW,
                MARTIAL_ELIXIR, SOUL_SOOTHING_PILL, PILL_RESIDUE);
    }
}
