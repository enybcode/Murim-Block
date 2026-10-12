package com.murimblock.world.block;

import com.murimblock.Murimblock;
import com.murimblock.item.MurimItems;
import com.murimblock.world.MurimTrees;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Blocks of the seven Murim biomes (docs/biomes): the rock of each landscape, eight trees (log, planks, leaves,
 * sapling) and the plants that make each biome's flora. Models, textures and loot come from
 * scripts/worldgen (generate-nature.cjs, NatureTextures.java).
 */
public final class MurimBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Murimblock.MOD_ID);
    private static final List<DeferredBlock<? extends Block>> CREATIVE_ORDER = new ArrayList<>();

    // ---------- rock and soil of the landscapes ----------
    /** Guilin karst: pale grey limestone of the tower peaks. */
    public static final DeferredBlock<Block> KARST_LIMESTONE = stone("karst_limestone", MapColor.TERRACOTTA_WHITE);
    public static final DeferredBlock<Block> MOSSY_KARST_LIMESTONE = stone("mossy_karst_limestone", MapColor.TERRACOTTA_WHITE);
    /** Huangshan: coarse pale granite (vanilla granite is far too red). */
    public static final DeferredBlock<Block> PALE_GRANITE = stone("pale_granite", MapColor.QUARTZ);
    /** Wulingyuan quartz sandstone of the peach valley's pillars, with horizontal bedding. */
    public static final DeferredBlock<Block> PILLAR_SANDSTONE = stone("pillar_sandstone", MapColor.SAND);
    /** Loess Plateau: buff wind-laid silt, and the reddish fossil soil bands seen in gully walls. */
    public static final DeferredBlock<Block> LOESS = soil("loess", MapColor.SAND, SoundType.GRAVEL);
    public static final DeferredBlock<Block> RED_LOESS = soil("red_loess", MapColor.TERRACOTTA_ORANGE, SoundType.GRAVEL);
    /** Gobi: dark, wind-varnished desert gravel; falls like gravel. */
    public static final DeferredBlock<ColoredFallingBlock> GOBI_GRAVEL = register("gobi_gravel",
            p -> new ColoredFallingBlock(new ColorRGBA(0x5A4A3AFF), p),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).instrument(NoteBlockInstrument.SNARE)
                    .strength(0.6F).sound(SoundType.GRAVEL));

    // ---------- trees ----------
    public static final Wood OSMANTHUS = wood("osmanthus", MapColor.TERRACOTTA_LIGHT_GRAY, MapColor.COLOR_GREEN, MurimTrees.OSMANTHUS, MurimPlantBlock.Soil.GRASS);
    public static final Wood BANYAN = wood("banyan", MapColor.TERRACOTTA_GRAY, MapColor.COLOR_GREEN, MurimTrees.BANYAN, MurimPlantBlock.Soil.GRASS);
    public static final Wood HUANGSHAN_PINE = wood("huangshan_pine", MapColor.TERRACOTTA_BROWN, MapColor.COLOR_GREEN, MurimTrees.HUANGSHAN_PINE, MurimPlantBlock.Soil.GRASS);
    public static final Wood JUJUBE = wood("jujube", MapColor.COLOR_BROWN, MapColor.COLOR_GREEN, MurimTrees.JUJUBE, MurimPlantBlock.Soil.SAND);
    public static final Wood DOVE_TREE = wood("dove_tree", MapColor.TERRACOTTA_GRAY, MapColor.COLOR_LIGHT_GREEN, MurimTrees.DOVE_TREE, MurimPlantBlock.Soil.GRASS);
    public static final Wood WILLOW = wood("willow", MapColor.TERRACOTTA_GRAY, MapColor.COLOR_LIGHT_GREEN, MurimTrees.WILLOW, MurimPlantBlock.Soil.WET);
    public static final Wood PEACH = wood("peach", MapColor.TERRACOTTA_BROWN, MapColor.COLOR_PINK, MurimTrees.PEACH, MurimPlantBlock.Soil.GRASS);
    public static final Wood DESERT_POPLAR = wood("desert_poplar", MapColor.TERRACOTTA_LIGHT_GRAY, MapColor.GOLD, MurimTrees.DESERT_POPLAR, MurimPlantBlock.Soil.SAND);

    /** Hanging growth: banyan aerial roots and weeping willow fronds. */
    public static final DeferredBlock<HangingStrandBlock> BANYAN_ROOTS = register("banyan_roots", HangingStrandBlock::new, () -> strand(MapColor.TERRACOTTA_BROWN));
    public static final DeferredBlock<HangingStrandBlock> WILLOW_STRANDS = register("willow_strands", HangingStrandBlock::new, () -> strand(MapColor.COLOR_LIGHT_GREEN));

    // ---------- giant bamboo of Shu ----------
    public static final DeferredBlock<MosoBambooBlock> MOSO_BAMBOO = register("moso_bamboo", MosoBambooBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(1.5F).sound(SoundType.BAMBOO_WOOD).noOcclusion());
    public static final DeferredBlock<LeavesBlock> MOSO_BAMBOO_LEAVES = register("moso_bamboo_leaves", LeavesBlock::new, () -> leaves(MapColor.COLOR_GREEN));

    // ---------- plants ----------
    public static final DeferredBlock<MurimPlantBlock> KARST_FERN = plant("karst_fern", MurimPlantBlock.Soil.GRASS, 12, MapColor.PLANT);
    public static final DeferredBlock<MurimTallPlantBlock> RHODODENDRON = tallPlant("rhododendron", MurimPlantBlock.Soil.GRASS, MapColor.COLOR_PINK);
    public static final DeferredBlock<MurimPlantBlock> SEA_BUCKTHORN = plant("sea_buckthorn", MurimPlantBlock.Soil.SAND, 14, MapColor.COLOR_ORANGE);
    public static final DeferredBlock<MurimPlantBlock> FEATHER_GRASS = plant("feather_grass", MurimPlantBlock.Soil.SAND, 13, MapColor.SAND);
    public static final DeferredBlock<MurimPlantBlock> BAMBOO_SHOOT = plant("bamboo_shoot", MurimPlantBlock.Soil.GRASS, 9, MapColor.COLOR_BROWN);
    public static final DeferredBlock<MurimPlantBlock> ARROW_BAMBOO = plant("arrow_bamboo", MurimPlantBlock.Soil.GRASS, 15, MapColor.PLANT);
    public static final DeferredBlock<MurimTallPlantBlock> TREE_FERN = tallPlant("tree_fern", MurimPlantBlock.Soil.GRASS, MapColor.PLANT);
    public static final DeferredBlock<LotusBlock> LOTUS_PAD = lotus("lotus_pad");
    public static final DeferredBlock<LotusBlock> LOTUS_BLOSSOM = lotus("lotus_blossom");
    public static final DeferredBlock<MurimTallPlantBlock> WILD_RICE = tallPlant("wild_rice", MurimPlantBlock.Soil.WET, MapColor.PLANT);
    public static final DeferredBlock<FlowerBlock> CHRYSANTHEMUM = flower("chrysanthemum", MapColor.GOLD);
    public static final DeferredBlock<FlowerBlock> RAPESEED = flower("rapeseed", MapColor.COLOR_YELLOW);
    public static final DeferredBlock<MurimTallPlantBlock> TAMARISK = tallPlant("tamarisk", MurimPlantBlock.Soil.SAND, MapColor.COLOR_PINK);
    public static final DeferredBlock<MurimPlantBlock> SAXAUL = plant("saxaul", MurimPlantBlock.Soil.SAND, 14, MapColor.TERRACOTTA_GREEN);
    public static final DeferredBlock<MurimPlantBlock> CISTANCHE = plant("cistanche", MurimPlantBlock.Soil.SAND, 10, MapColor.TERRACOTTA_YELLOW);

    private MurimBlocks() { }

    /** Log, planks, leaves and sapling of one tree. */
    public record Wood(String name, DeferredBlock<RotatedPillarBlock> log, DeferredBlock<Block> planks,
                       DeferredBlock<LeavesBlock> leaves, DeferredBlock<MurimSaplingBlock> sapling) { }

    public static final List<Wood> WOODS = List.of(OSMANTHUS, BANYAN, HUANGSHAN_PINE, JUJUBE, DOVE_TREE, WILLOW, PEACH, DESERT_POPLAR);

    public static List<DeferredBlock<? extends Block>> creativeOrder() {
        return Collections.unmodifiableList(CREATIVE_ORDER);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    /** Fire spreads and burns like the vanilla equivalents (call from common setup). */
    public static void registerFlammability() {
        FireBlock fire = (FireBlock) Blocks.FIRE;
        for (Wood wood : WOODS) {
            fire.setFlammable(wood.log().get(), 5, 5);
            fire.setFlammable(wood.planks().get(), 5, 20);
            fire.setFlammable(wood.leaves().get(), 30, 60);
        }
        fire.setFlammable(MOSO_BAMBOO.get(), 5, 5);
        fire.setFlammable(MOSO_BAMBOO_LEAVES.get(), 30, 60);
        for (var plant : List.of(KARST_FERN, SEA_BUCKTHORN, FEATHER_GRASS, BAMBOO_SHOOT, ARROW_BAMBOO, CHRYSANTHEMUM, RAPESEED, SAXAUL,
                CISTANCHE, RHODODENDRON, TREE_FERN, WILD_RICE, TAMARISK, BANYAN_ROOTS, WILLOW_STRANDS)) {
            fire.setFlammable(plant.get(), 60, 100);
        }
    }

    // ---------- helpers ----------

    private static <B extends Block> DeferredBlock<B> register(String name, Function<BlockBehaviour.Properties, B> factory,
                                                               Supplier<BlockBehaviour.Properties> properties) {
        return register(name, factory, properties, block -> new BlockItem(block, new Item.Properties()));
    }

    private static <B extends Block> DeferredBlock<B> register(String name, Function<BlockBehaviour.Properties, B> factory,
                                                               Supplier<BlockBehaviour.Properties> properties,
                                                               Function<B, Item> item) {
        DeferredBlock<B> block = BLOCKS.register(name, () -> factory.apply(properties.get()));
        MurimItems.ITEMS.register(name, () -> item.apply(block.get()));
        CREATIVE_ORDER.add(block);
        return block;
    }

    private static DeferredBlock<Block> stone(String name, MapColor color) {
        return register(name, Block::new, () -> BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(1.5F, 6.0F));
    }

    private static DeferredBlock<Block> soil(String name, MapColor color, SoundType sound) {
        return register(name, Block::new, () -> BlockBehaviour.Properties.of().mapColor(color).strength(0.6F).sound(sound));
    }

    private static Wood wood(String name, MapColor bark, MapColor foliage, net.minecraft.world.level.block.grower.TreeGrower grower,
                             MurimPlantBlock.Soil soil) {
        var log = register(name + "_log", RotatedPillarBlock::new, () -> BlockBehaviour.Properties.of().mapColor(bark)
                .instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava());
        var planks = register(name + "_planks", Block::new, () -> BlockBehaviour.Properties.of().mapColor(bark)
                .instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());
        var leaves = register(name + "_leaves", LeavesBlock::new, () -> leaves(foliage));
        var sapling = register(name + "_sapling", p -> new MurimSaplingBlock(grower, soil, p), () -> BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY));
        return new Wood(name, log, planks, leaves, sapling);
    }

    private static BlockBehaviour.Properties leaves(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(0.2F).randomTicks().sound(SoundType.GRASS).noOcclusion()
                .isValidSpawn(Blocks::ocelotOrParrot).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)
                .ignitedByLava().pushReaction(PushReaction.DESTROY).isRedstoneConductor((s, l, p) -> false);
    }

    private static BlockBehaviour.Properties strand(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).noCollission().instabreak().sound(SoundType.VINE)
                .ignitedByLava().pushReaction(PushReaction.DESTROY).replaceable();
    }

    private static BlockBehaviour.Properties plantProperties(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).noCollission().instabreak().sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ).ignitedByLava().pushReaction(PushReaction.DESTROY);
    }

    private static DeferredBlock<MurimPlantBlock> plant(String name, MurimPlantBlock.Soil soil, int height, MapColor color) {
        return register(name, p -> new MurimPlantBlock(soil, height, p), () -> plantProperties(color));
    }

    private static DeferredBlock<MurimTallPlantBlock> tallPlant(String name, MurimPlantBlock.Soil soil, MapColor color) {
        return register(name, p -> new MurimTallPlantBlock(soil, p), () -> plantProperties(color));
    }

    private static DeferredBlock<FlowerBlock> flower(String name, MapColor color) {
        return register(name, p -> new FlowerBlock(MobEffects.REGENERATION, 5, p), () -> plantProperties(color));
    }

    private static DeferredBlock<LotusBlock> lotus(String name) {
        return register(name, LotusBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).instabreak()
                .sound(SoundType.LILY_PAD).noOcclusion().pushReaction(PushReaction.DESTROY),
                block -> new PlaceOnWaterBlockItem(block, new Item.Properties()));
    }
}
