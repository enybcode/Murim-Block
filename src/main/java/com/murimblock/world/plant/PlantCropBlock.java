package com.murimblock.world.plant;

import com.mojang.serialization.MapCodec;
import com.murimblock.world.block.MurimPlantBlock;
import com.murimblock.world.plant.PlantCatalog.Species;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A planted herb or flower (docs/cultivation/plants.md): one block for every species (its index in PlantCatalog, saved)
 * and its stage, drawn in 3D and moving by its block entity (GeckoLib). It grows on random ticks as PlantGrowth says;
 * used with an empty hand when mature it is harvested (kept, or pulled up for roots and shoots); sneaking, it tells what
 * it lacks. Bone meal hurries it to maturity, never to spirit maturity.
 */
public class PlantCropBlock extends BaseEntityBlock implements BonemealableBlock {
    public static final MapCodec<PlantCropBlock> CODEC = simpleCodec(PlantCropBlock::new);
    public static final IntegerProperty SPECIES = IntegerProperty.create("species", 0, 63);
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);
    private static final VoxelShape[] SHAPES = { Block.box(5, 0, 5, 11, 4, 11), Block.box(3, 0, 3, 13, 8, 13), Block.box(2, 0, 2, 14, 13, 14), Block.box(2, 0, 2, 14, 13, 14) };

    public PlantCropBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SPECIES, 0).setValue(AGE, 0));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPECIES, AGE);
    }

    public static Species species(BlockState state) {
        return PlantCatalog.byIndex(state.getValue(SPECIES));
    }

    public BlockState planted(Species species) {
        return defaultBlockState().setValue(SPECIES, species.index()).setValue(AGE, 0);
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlantCropBlockEntity(pos, state);
    }

    // ---------------------------------------------------------------- where it roots

    /** Whether this species can root here: its soil under it, or still water for the floating ones. */
    public static boolean mayRoot(Species species, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (species.soil() == PlantCatalog.Soil.WATER) {
            return level.getFluidState(pos.below()).getType() == Fluids.WATER && level.getFluidState(pos).isEmpty();
        }
        return soil(species).accepts(below);
    }

    static MurimPlantBlock.Soil soil(Species species) {
        return switch (species.soil()) {
            case SAND -> MurimPlantBlock.Soil.SAND;
            case WET -> MurimPlantBlock.Soil.WET;
            case ALPINE -> MurimPlantBlock.Soil.ALPINE;
            default -> MurimPlantBlock.Soil.GRASS;
        };
    }

    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return mayRoot(species(state), level, pos);
    }

    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return !state.canSurvive(level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    // ---------------------------------------------------------------- growing

    /** The conditions here now: light, warmth, water. */
    public static PlantGrowth.Condition[] conditions(Species species, Level level, BlockPos pos) {
        int light = Math.max(level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken(), level.getBrightness(LightLayer.BLOCK, pos));
        if (level.isDay() || level.getBrightness(LightLayer.SKY, pos) >= 13) light = Math.max(light, level.getBrightness(LightLayer.SKY, pos));
        boolean needsWater = species.soil() == PlantCatalog.Soil.WET || species.soil() == PlantCatalog.Soil.WATER;
        return new PlantGrowth.Condition[] {
            PlantGrowth.light(light),
            PlantGrowth.warmth(species.climate(), level.getBiome(pos).value().getBaseTemperature()),
            PlantGrowth.water(needsWater, species.soil() == PlantCatalog.Soil.WATER || waterNear(level, pos)),
        };
    }

    static boolean waterNear(LevelReader level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 0, 4)))
            if (level.getFluidState(p).is(FluidTags.WATER)) return true;
        return false;
    }

    /** Planted herbs near it (Qi shared). */
    static int neighbours(Level level, BlockPos pos) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, -2, -4), pos.offset(4, 2, 4)))
            if (!p.equals(pos) && level.getBlockState(p).getBlock() instanceof PlantCropBlock) n++;
        return n;
    }

    @Override protected boolean isRandomlyTicking(BlockState state) { return state.getValue(AGE) < PlantGrowth.SPIRIT; }

    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        grow(state, level, pos, random.nextDouble());
    }

    /** One random tick of growth (`roll` in [0, 1)): returns whether it grew a stage. */
    public static boolean grow(BlockState state, ServerLevel level, BlockPos pos, double roll) {
        Species species = species(state);
        int age = state.getValue(AGE);
        if (age >= PlantGrowth.SPIRIT) return false;
        PlantGrowth.Condition condition = PlantGrowth.overall(conditions(species, level, pos));
        double chance;
        if (age < PlantGrowth.MATURE) chance = PlantGrowth.growChance(species.rarity(), condition);
        else {
            var place = com.murimblock.cultivation.CultivationService.placeAt(level, pos, pos.getY());
            boolean qiRich = place != com.murimblock.cultivation.CultivationRules.Place.ORDINARY;
            chance = PlantGrowth.spiritChance(species.rarity(), condition, qiRich, place == com.murimblock.cultivation.CultivationRules.Place.POWER, neighbours(level, pos));
        }
        if (roll >= chance) return false;
        level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof PlantCropBlockEntity be) be.grew(condition, age + 1);
        if (age + 1 == PlantGrowth.SPIRIT) level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.4F);
        return true;
    }

    @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < PlantGrowth.MATURE;
    }

    @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }

    @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(AGE, Math.min(PlantGrowth.MATURE, state.getValue(AGE) + 1)), Block.UPDATE_ALL);
        // Hurried: it gains nothing in purity.
    }

    // ---------------------------------------------------------------- harvesting

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Species species = species(state);
        int age = state.getValue(AGE);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) player.displayClientMessage(status(species, level, pos, age), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (age < PlantGrowth.MATURE) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ServerLevel server = (ServerLevel) level;
        int purity = level.getBlockEntity(pos) instanceof PlantCropBlockEntity be ? be.purity() : PlantGrowth.START_PURITY;
        for (ItemStack stack : harvest(species, age, purity, server.random)) popResource(level, pos, stack);
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.9F + server.random.nextFloat() * 0.2F);
        if (species.harvest() == PlantCatalog.Harvest.UPROOT) level.removeBlock(pos, false);
        else {
            level.setBlock(pos, state.setValue(AGE, PlantGrowth.YOUNG), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof PlantCropBlockEntity be) be.harvested();
        }
        return InteractionResult.CONSUME;
    }

    /** What a harvest gives: the herb (with its maturity and purity) or the flower, and perhaps what it grows from. */
    public static List<ItemStack> harvest(Species species, int age, int purity, RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        int count = PlantGrowth.harvestCount(species, age);
        if (species.isHerb()) out.add(HerbItem.of(MurimFlora.herb(species), count, age >= PlantGrowth.SPIRIT ? "spirit" : "mature", purity));
        else out.add(new ItemStack(MurimFlora.wildItem(species), count));
        if (random.nextDouble() < PlantGrowth.seedChance(species, age)) out.add(new ItemStack(MurimFlora.seed(species)));
        return out;
    }

    /** Broken: a mature plant gives its harvest; a younger one gives back what it grew from. */
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        Species species = species(state);
        int age = state.getValue(AGE);
        if (age < PlantGrowth.MATURE) return List.of(new ItemStack(MurimFlora.seed(species)));
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        int purity = be instanceof PlantCropBlockEntity crop ? crop.purity() : PlantGrowth.START_PURITY;
        return harvest(species, age, purity, params.getLevel().random);
    }

    static Component status(Species species, Level level, BlockPos pos, int age) {
        PlantGrowth.Condition[] c = conditions(species, level, pos);
        int purity = level.getBlockEntity(pos) instanceof PlantCropBlockEntity be ? be.purity() : PlantGrowth.START_PURITY;
        return Component.translatable("message.murimblock.crop.status", Component.translatable(MurimFlora.nameKey(species)),
                Component.translatable("message.murimblock.crop.stage." + age), word(c[0]), word(c[1]), word(c[2]), purity);
    }

    private static Component word(PlantGrowth.Condition c) {
        return Component.translatable("message.murimblock.crop." + (c == PlantGrowth.Condition.GOOD ? "good" : c == PlantGrowth.Condition.POOR ? "poor" : "none"));
    }

    // ---------------------------------------------------------------- spirit maturity shows

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean spirit = state.getValue(AGE) == PlantGrowth.SPIRIT, legendary = species(state).rarity() == PlantCatalog.Rarity.VERY_RARE && state.getValue(AGE) >= PlantGrowth.MATURE;
        if ((spirit || legendary) && random.nextInt(spirit ? 3 : 5) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.4 + random.nextDouble() * 0.5,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.012, 0);
        }
    }
}
