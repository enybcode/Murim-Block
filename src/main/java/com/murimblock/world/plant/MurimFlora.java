package com.murimblock.world.plant;

import com.murimblock.Murimblock;
import com.murimblock.world.block.LotusBlock;
import com.murimblock.world.block.MurimPlantBlock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Murim's plants in game (docs/cultivation/plants.md), from PlantCatalog: the wild plants that were not blocks yet
 * (still 3D models, their item a pixel art icon), every herb item, what each plant grows from, and the planted crop with
 * its block entity.
 */
public final class MurimFlora {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Murimblock.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Murimblock.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Murimblock.MOD_ID);

    public static final DeferredBlock<PlantCropBlock> CROP = BLOCKS.register("murim_crop", () -> new PlantCropBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT).noCollission().instabreak().randomTicks().sound(SoundType.CROP).noOcclusion()
            .pushReaction(PushReaction.DESTROY).ignitedByLava()));
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlantCropBlockEntity>> CROP_ENTITY = BLOCK_ENTITIES.register("murim_crop",
            () -> BlockEntityType.Builder.of(PlantCropBlockEntity::new, CROP.get()).build(null));

    private static final Map<String, DeferredBlock<? extends Block>> WILD = new HashMap<>();
    private static final Map<String, DeferredItem<Item>> HERBS = new HashMap<>(), SEEDS = new HashMap<>();
    /** In the nature tab, in this order: each plant, its herb, what it grows from. */
    private static final List<Supplier<? extends Item>> CREATIVE = new ArrayList<>();

    static {
        for (PlantCatalog.Species s : PlantCatalog.ALL) {
            if (!s.existingWild()) {
                Supplier<BlockBehaviour.Properties> props = () -> BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak()
                        .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ).ignitedByLava().pushReaction(PushReaction.DESTROY);
                DeferredBlock<? extends Block> block;
                DeferredItem<? extends Item> item;
                if (s.soil() == PlantCatalog.Soil.WATER) {
                    var water = BLOCKS.register(s.wild(), () -> new LotusBlock(props.get().offsetType(BlockBehaviour.OffsetType.NONE)));
                    block = water;
                    item = ITEMS.register(s.wild(), () -> new PlaceOnWaterBlockItem(water.get(), new Item.Properties()));
                } else {
                    var plant = BLOCKS.register(s.wild(), () -> new MurimPlantBlock(PlantCropBlock.soil(s), 13, props.get()));
                    block = plant;
                    item = ITEMS.register(s.wild(), () -> new BlockItem(plant.get(), new Item.Properties().rarity(rarity(s))));
                }
                WILD.put(s.id(), block);
                CREATIVE.add(item);
            }
            // An existing wild plant is in the nature tab already (MurimBlocks).
            if (s.herb() != null) {
                DeferredItem<Item> herb = ITEMS.register(s.herb(), () -> new HerbItem(s, new Item.Properties().rarity(rarity(s))));
                HERBS.put(s.id(), herb);
                CREATIVE.add(herb);
            }
            DeferredItem<Item> seed = ITEMS.register(s.seed(), () -> new PlantSeedItem(s, new Item.Properties()));
            SEEDS.put(s.id(), seed);
            CREATIVE.add(seed);
        }
    }

    private MurimFlora() {
    }

    private static Rarity rarity(PlantCatalog.Species s) {
        return switch (s.rarity()) { case COMMON -> Rarity.COMMON; case UNCOMMON -> Rarity.UNCOMMON; case RARE -> Rarity.RARE; case VERY_RARE -> Rarity.EPIC; };
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    /** The herb item of a herb species. */
    public static Item herb(PlantCatalog.Species s) {
        DeferredItem<Item> herb = HERBS.get(s.id());
        return herb == null ? Items.AIR : herb.get();
    }

    /** What a species grows from. */
    public static Item seed(PlantCatalog.Species s) {
        return SEEDS.get(s.id()).get();
    }

    /** The wild plant's item (a flower's harvest), new or an existing block's. */
    public static Item wildItem(PlantCatalog.Species s) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, s.wild()));
    }

    /** The wild plant's block, new or existing. */
    public static Block wildBlock(PlantCatalog.Species s) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, s.wild()));
    }

    /** The translation key of the plant's name (its wild block's). */
    public static String nameKey(PlantCatalog.Species s) {
        return "block." + Murimblock.MOD_ID + "." + s.wild();
    }

    public static List<Supplier<? extends Item>> creativeItems() {
        return List.copyOf(CREATIVE);
    }
}
