package com.murimblock.alchemy;

import com.murimblock.qi.QiService;
import com.murimblock.world.plant.HerbItem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The pill furnace at work (docs/cultivation/alchemy.md). Spring water first, then the herbs in the recipe's order; then,
 * its fire fed with charcoal, the alchemist holds the heat: the bellows raise it, left alone it sinks to the embers'.
 * Four steps: PREPARE (the herbs in), EXTRACT, STABILIZE (hold each band its time), REFINE (hold a narrow band while
 * infusing one's own Qi). Out come the pills, their purity the herbs' and the alchemist's skill; too much heat costs
 * purity, a blaze past {@link #BLAST} ruins it all in a residue and a burst of flame.
 */
public class PillFurnaceBlockEntity extends BlockEntity implements GeoBlockEntity, MenuProvider {
    public static final int PREPARE = 0, EXTRACT = 1, STABILIZE = 2, REFINE = 3, DONE = 4;
    /** The heat's scale, the embers' (with fuel), the bellows' rise a tick, the drift back (share of the gap a tick). */
    public static final int MAX_HEAT = 1000, EMBERS = 320, BLAST = 950;
    public static final double BELLOWS = 6, DRIFT = 0.01;
    /** Fuel ticks a charcoal or coal gives, the most it holds. */
    public static final int FUEL_PER_COAL = 600, MAX_FUEL = 6000;
    /** Above a band by this much: purity lost each tick. */
    public static final int SCORCH_MARGIN = 120;
    public static final double SCORCH_LOSS = 0.08;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle"), REFINING = RawAnimation.begin().thenLoop("refine"),
            FINISH = RawAnimation.begin().thenPlay("finish");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean water;
    private final List<ItemStack> herbs = new ArrayList<>();
    private int phase = PREPARE, held, spent, fuel;
    private double heat, scorch;
    private boolean bellows, infusing;
    @Nullable private UUID brewer, infuser;
    /** Ticks until the finished pills fly out (the lid opened). */
    private int outIn = -1;
    private List<ItemStack> out = List.of();

    public PillFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(AlchemyItems.FURNACE_ENTITY.get(), pos, state);
    }

    // ---------------------------------------------------------------- what is in it

    public boolean hasWater() { return water; }
    public List<ItemStack> herbs() { return herbs; }
    public int phase() { return phase; }
    public int heat() { return (int) Math.round(heat); }
    public int fuel() { return fuel; }
    public boolean lit() { return fuel > 0; }

    @Nullable public AlchemyRecipes.Recipe recipe() {
        return AlchemyRecipes.made(herbs.stream().map(ItemStack::getItem).toList());
    }

    /** Spring water poured in (first). */
    public boolean pourWater(Player player) {
        if (water || phase != PREPARE) return false;
        water = true;
        brewer = player == null ? null : player.getUUID();
        changed();
        return true;
    }

    /** A herb thrown in, if it is the next one of a recipe. Returns a message key, or null if it went in. */
    @Nullable public String addHerb(Player player, ItemStack stack) {
        if (phase != PREPARE) return "alchemy.murimblock.busy";
        if (!water) return "alchemy.murimblock.water_first";
        List<Item> next = new ArrayList<>(herbs.stream().map(ItemStack::getItem).toList());
        next.add(stack.getItem());
        if (AlchemyRecipes.startingWith(next).isEmpty()) return "alchemy.murimblock.not_now";
        herbs.add(stack.copyWithCount(1));
        if (player != null) brewer = player.getUUID();
        changed();
        AlchemyRecipes.Recipe made = recipe();
        if (made != null && player != null && !AlchemyKnowledge.knows(player, made)) return "alchemy.murimblock.unknown";
        return null;
    }

    /** Fuel added (charcoal, coal): true if it took it. */
    public boolean addFuel(ItemStack stack) {
        if (!(stack.is(Items.CHARCOAL) || stack.is(Items.COAL)) || fuel + FUEL_PER_COAL > MAX_FUEL) return false;
        fuel += FUEL_PER_COAL;
        changed();
        return true;
    }

    /** Before the work starts, everything back (the water is lost). */
    public List<ItemStack> takeBack() {
        if (phase != PREPARE) return List.of();
        List<ItemStack> back = new ArrayList<>(herbs);
        herbs.clear();
        water = false;
        changed();
        return back;
    }

    public void setBellows(boolean on) { bellows = on; }

    public void setInfusing(@Nullable ServerPlayer player, boolean on) {
        infusing = on;
        infuser = on && player != null ? player.getUUID() : null;
    }

    // ---------------------------------------------------------------- the work, each tick

    public static void serverTick(Level level, BlockPos pos, BlockState state, PillFurnaceBlockEntity be) {
        be.tick((ServerLevel) level);
    }

    void tick(ServerLevel level) {
        if (outIn >= 0 && --outIn < 0) release(level);
        // The fire: fed, it holds the embers' heat; the bellows raise it; without fuel it dies down.
        if (fuel > 0) fuel--;
        double target = fuel > 0 ? EMBERS : 0;
        heat += (target - heat) * DRIFT;
        if (bellows && fuel > 0) heat = Math.min(MAX_HEAT, heat + BELLOWS);
        boolean lit = fuel > 0;
        if (getBlockState().getValue(PillFurnaceBlock.LIT) != lit) level.setBlock(worldPosition, getBlockState().setValue(PillFurnaceBlock.LIT, lit), Block.UPDATE_ALL);
        AlchemyRecipes.Recipe recipe = recipe();
        if (phase == PREPARE && recipe != null && lit && knownByBrewer(level, recipe)) phase = EXTRACT;
        if (phase >= EXTRACT && phase <= REFINE && recipe != null) work(level, recipe);
        if (level.getGameTime() % 10 == 0) changed();
    }

    private boolean knownByBrewer(ServerLevel level, AlchemyRecipes.Recipe recipe) {
        Player p = brewer == null ? null : level.getPlayerByUUID(brewer);
        return p != null && AlchemyKnowledge.knows(p, recipe);
    }

    private void work(ServerLevel level, AlchemyRecipes.Recipe recipe) {
        if (heat >= BLAST) {
            fail(level);
            return;
        }
        AlchemyRecipes.Band band = recipe.band(phase);
        spent++;
        if (heat > band.high() + SCORCH_MARGIN) {
            scorch += SCORCH_LOSS;
            if (level.getGameTime() % 4 == 0) level.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.4, worldPosition.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.01);
        }
        boolean inBand = band.holds(heat());
        if (phase == REFINE) {
            ServerPlayer p = infuser == null ? null : (ServerPlayer) level.getPlayerByUUID(infuser);
            boolean infused = infusing && p != null && p.distanceToSqr(worldPosition.getCenter()) < 64 && QiService.getQi(p) >= recipe.qiPerTick();
            if (infused) QiService.removeQi(p, recipe.qiPerTick());
            inBand &= infused;
        }
        if (inBand) held++;
        if (held >= band.hold()) {
            held = 0;
            phase++;
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 0.8F + phase * 0.1F);
            if (phase == DONE) finish(level, recipe);
        }
    }

    /** Skill: the ideal time over the time it took, a little lenient; between 0.3 and 1. */
    double skill(AlchemyRecipes.Recipe recipe) {
        int ideal = recipe.extract().hold() + recipe.stabilize().hold() + recipe.refine().hold();
        return Math.clamp(ideal * 1.6 / Math.max(ideal, spent), 0.3, 1.0);
    }

    /** The herbs' own purity (a mature herb found wild is 70). */
    int herbsPurity() {
        return (int) Math.round(herbs.stream().mapToInt(HerbItem::purity).average().orElse(60));
    }

    /** The pills' purity: their herbs' purity and the skill, less what too much heat cost. */
    public int forecastPurity() {
        AlchemyRecipes.Recipe recipe = recipe();
        if (recipe == null) return 0;
        return (int) Math.clamp(Math.round(herbsPurity() * 0.6 + skill(recipe) * 40 - scorch), 5, 100);
    }

    private void finish(ServerLevel level, AlchemyRecipes.Recipe recipe) {
        int purity = forecastPurity();
        int count = recipe.count() + (purity >= 80 ? 1 : 0) + (herbs.stream().anyMatch(h -> HerbItem.maturity(h).equals("spirit")) ? 1 : 0);
        out = List.of(AlchemyPillItem.withPurity(new ItemStack(recipe.resultItem(), count), purity));
        Player brewerPlayer = brewer == null ? null : level.getPlayerByUUID(brewer);
        if (brewerPlayer != null) brewerPlayer.displayClientMessage(Component.translatable("alchemy.murimblock.done", count,
                Component.translatable(recipe.resultItem().getDescriptionId()), purity), true);
        triggerAnim("main", "finish");
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5F, 0.7F);
        outIn = 20;
        reset();
    }

    /** The pills fly out of the open furnace. */
    private void release(ServerLevel level) {
        for (ItemStack stack : out) {
            ItemEntity item = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.6, worldPosition.getZ() + 0.5, stack);
            item.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.15, 0.35, (level.random.nextDouble() - 0.5) * 0.15);
            level.addFreshEntity(item);
        }
        level.sendParticles(ParticleTypes.END_ROD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.6, worldPosition.getZ() + 0.5, 30, 0.3, 0.6, 0.3, 0.05);
        out = List.of();
    }

    /** A blaze: everything in it burns to a residue, flames burst out, a blast that hurts but breaks nothing. */
    private void fail(ServerLevel level) {
        out = List.of(new ItemStack(AlchemyItems.PILL_RESIDUE.get()));
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 1.2, z = worldPosition.getZ() + 0.5;
        level.explode(null, x, y, z, 1.2F, false, Level.ExplosionInteraction.NONE);
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 60, 0.5, 0.5, 0.5, 0.15);
        Player brewerPlayer = brewer == null ? null : level.getPlayerByUUID(brewer);
        if (brewerPlayer != null) brewerPlayer.displayClientMessage(Component.translatable("alchemy.murimblock.blast"), true);
        heat = EMBERS;
        outIn = 2;
        reset();
    }

    private void reset() {
        herbs.clear();
        water = false;
        phase = PREPARE;
        held = 0;
        spent = 0;
        scorch = 0;
        bellows = false;
        infusing = false;
        infuser = null;
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // ---------------------------------------------------------------- for tests

    public void setHeatForTests(double heat) { this.heat = heat; }
    public void tickForTests(ServerLevel level) { tick(level); }
    public int heldForTests() { return held; }

    // ---------------------------------------------------------------- the screen

    /** What the screen shows (synchronised by the menu): heat, phase, held, the band, fuel, recipe, known, herbs in, purity, flags, Qi a tick. */
    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            AlchemyRecipes.Recipe recipe = recipe();
            AlchemyRecipes.Band band = recipe == null || phase < EXTRACT || phase > REFINE ? null : recipe.band(phase);
            return switch (i) {
                case 0 -> heat();
                case 1 -> phase;
                case 2 -> held;
                case 3 -> band == null ? 0 : band.hold();
                case 4 -> band == null ? 0 : band.low();
                case 5 -> band == null ? 0 : band.high();
                case 6 -> fuel;
                case 7 -> recipe == null ? candidate() : recipe.index();
                case 8 -> recipe != null && level instanceof ServerLevel s && knownByBrewer(s, recipe) ? 1 : 0;
                case 9 -> herbs.size();
                case 10 -> forecastPurity();
                case 11 -> (bellows ? 1 : 0) | (infusing ? 2 : 0) | (water ? 4 : 0);
                case 12 -> recipe == null ? 0 : (int) Math.round(recipe.qiPerTick() * 100);
                default -> 0;
            };
        }
        @Override public void set(int i, int value) { }
        @Override public int getCount() { return 13; }
    };

    /** While the herbs are going in: the recipe they are on their way to, if only one. */
    private int candidate() {
        if (herbs.isEmpty()) return -1;
        var c = AlchemyRecipes.startingWith(herbs.stream().map(ItemStack::getItem).toList());
        return c.size() == 1 ? c.get(0).index() : -1;
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.murimblock.alchemy_furnace");
    }

    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PillFurnaceMenu(id, inventory, this, data);
    }

    // ---------------------------------------------------------------- saving

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("water", water);
        ListTag list = new ListTag();
        for (ItemStack h : herbs) list.add(h.save(registries));
        tag.put("herbs", list);
        tag.putInt("phase", phase);
        tag.putInt("held", held);
        tag.putInt("spent", spent);
        tag.putInt("fuel", fuel);
        tag.putDouble("heat", heat);
        tag.putDouble("scorch", scorch);
        if (brewer != null) tag.putUUID("brewer", brewer);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water = tag.getBoolean("water");
        herbs.clear();
        for (Tag t : tag.getList("herbs", Tag.TAG_COMPOUND)) ItemStack.parse(registries, t).ifPresent(herbs::add);
        phase = tag.getInt("phase");
        held = tag.getInt("held");
        spent = tag.getInt("spent");
        fuel = tag.getInt("fuel");
        heat = tag.getDouble("heat");
        scorch = tag.getDouble("scorch");
        brewer = tag.hasUUID("brewer") ? tag.getUUID("brewer") : null;
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    // ---------------------------------------------------------------- GeckoLib

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> state.setAndContinue(getBlockState().getValue(PillFurnaceBlock.LIT) ? REFINING : IDLE))
                .triggerableAnim("finish", FINISH));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    /** Drawn bigger than its block (the furnace stands a block and a half high, its array wider). */
    public AABB renderBox() {
        return new AABB(worldPosition).inflate(1, 2, 1);
    }

    static ResourceLocation id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
}
