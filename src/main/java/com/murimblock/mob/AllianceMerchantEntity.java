package com.murimblock.mob;

import com.murimblock.integration.epicfight.EpicFightMobSupport;
import com.murimblock.item.MurimItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * The Alliance Merchant (盟商), who trades for the Orthodox Alliance in silver taels (docs/mobs/alliance-merchant.md). A
 * well-fed, smiling trader in azure brocade, all in cloth (scripts/animation/merchant-3d.cjs). Neutral and no fighter:
 * right-clicked, he opens Minecraft's trading screen; struck, he stops trading and runs. He keeps to a few blocks around
 * where he was placed and never despawns. Not spawned in the world yet: a spawn egg and /summon, until the Alliance's
 * capital has its market.
 * <p>His wares are fixed (see {@link #wares()}): the Alliance's weapons, pills and healing, the Long Fist scroll, and at
 * {@link #PENDANT_PRICE} taels apiece the locator pendants that point to each kind of structure, and the sect's token
 * that points to the sanctuary for a fortune ({@link #SECT_TOKEN_TAELS} taels and {@link #SECT_TOKEN_YUANBAO} gold
 * yuanbao); and he
 * buys the spoils of bandits and crabs for taels. Each trade can be made a few times, then he restocks every half day.
 */
public class AllianceMerchantEntity extends PathfinderMob implements Merchant {
    /** A locator pendant's price, in taels (two full stacks). */
    public static final int PENDANT_PRICE = 128;
    /**
     * The Cloud Cliff Sect's jade token, which points to its sanctuary: no longer given to new players (Enzo,
     * 16/10/2026), sold here only, at a ruinous price: a full stack of taels and 32 gold yuanbao (288 taels more).
     */
    public static final int SECT_TOKEN_TAELS = 64, SECT_TOKEN_YUANBAO = 32;
    /** How far from his stall he strolls (blocks). */
    public static final int HOME_RADIUS = 6;
    /** How often he restocks (ticks: half a Minecraft day). */
    public static final int RESTOCK_TICKS = 12000;
    /** How far a trading player may step away before he closes the trade (blocks). */
    static final double TRADE_REACH = 8.0;

    @Nullable private Player tradingPlayer;
    @Nullable private MerchantOffers offers;
    private long lastRestock;

    public AllianceMerchantEntity(EntityType<? extends AllianceMerchantEntity> type, Level level) {
        super(type, level);
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createMerchantAttributes() {
        return EpicFightMobSupport.humanoidAttributes(PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30).add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 16));
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // Struck, he runs (and the trade is off, see hurt()).
        goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.7));
        WaterAvoidingRandomStrollGoal stroll = new WaterAvoidingRandomStrollGoal(this, 0.5);
        stroll.setInterval(200);
        goalSelector.addGoal(6, stroll);
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        Player trader = tradingPlayer;
        if (trader != null) {
            // Standing at his stall while someone trades, facing them; a trader who walks off or dies ends it.
            getNavigation().stop();
            getLookControl().setLookAt(trader);
            if (!trader.isAlive() || trader.distanceToSqr(this) > TRADE_REACH * TRADE_REACH) stopTrading();
        }
        long now = level().getGameTime();
        if (now - lastRestock >= RESTOCK_TICKS) {
            lastRestock = now;
            if (offers != null) offers.forEach(MerchantOffer::resetUses);
        }
    }

    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !isAlive() || tradingPlayer != null && tradingPlayer != player) return InteractionResult.PASS;
        // A spawn egg or a name tag does its own thing.
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.NAME_TAG) || held.getItem() instanceof net.minecraft.world.item.SpawnEggItem) return InteractionResult.PASS;
        if (!level().isClientSide && getLastHurtByMob() == null) {
            setTradingPlayer(player);
            openTradingScreen(player, getDisplayName(), 1);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide) stopTrading();
        return hurt;
    }

    private void stopTrading() {
        Player trader = tradingPlayer;
        setTradingPlayer(null);
        if (trader instanceof ServerPlayer serverPlayer && serverPlayer.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu)
            serverPlayer.closeContainer();
    }

    // ---- his wares

    /** One silver tael. */
    private static ItemCost taels(int count) {
        return new ItemCost(MurimItems.SILVER_TAEL.get(), count);
    }

    private static MerchantOffer sells(ItemStack ware, int taels, int maxUses) {
        return new MerchantOffer(taels(taels), ware, maxUses, 0, 0.0F);
    }

    private static MerchantOffer buys(ItemLike spoil, int count, int taels, int maxUses) {
        return new MerchantOffer(new ItemCost(spoil, count), new ItemStack(MurimItems.SILVER_TAEL.get(), taels), maxUses, 0, 0.0F);
    }

    /** His trades, always the same: what he sells, then what he buys. */
    public static MerchantOffers wares() {
        MerchantOffers list = new MerchantOffers();
        // The Alliance's weapons.
        list.add(sells(new ItemStack(MurimItems.ALLIANCE_SPEAR.get()), 24, 2));
        list.add(sells(new ItemStack(MurimItems.SNOW_PLUM_JIAN.get()), 24, 2));
        list.add(sells(new ItemStack(MurimItems.RATTAN_SHIELD.get()), 12, 2));
        // The two knives (Enzo, 15/10/2026: bought here, or found by great luck in a site's barrel).
        list.add(sells(new ItemStack(MurimItems.BISHOU.get()), 16, 2));
        list.add(sells(new ItemStack(MurimItems.FLYING_KNIFE.get()), 12, 2));
        // Pills and healing, food for the road.
        list.add(sells(new ItemStack(MurimItems.QI_GATHERING_PILL.get()), 6, 8));
        list.add(sells(new ItemStack(MurimItems.REALM_BREAKING_PILL.get()), 32, 2));
        list.add(sells(PotionContents.createItemStack(Items.POTION, Potions.HEALING), 4, 6));
        list.add(sells(new ItemStack(Items.GOLDEN_APPLE), 8, 4));
        // Bread, the small change's affair: four loaves for a string of cash.
        list.add(new MerchantOffer(new ItemCost(MurimItems.CASH_STRING.get(), 1), new ItemStack(Items.BREAD, 4), 16, 0, 0.0F));
        // The arts sold openly: the Long Fist, a free art; the Traveller's Staff Art, at its price for now (Enzo, 16/10/2026).
        list.add(sells(new ItemStack(MurimItems.LONG_FIST_SCROLL.get()), 40, 1));
        list.add(sells(new ItemStack(com.murimblock.martial.TravellerStaffItems.SCROLL.get()), 40, 1));
        // The sect's jade token, dearest of all: a stack of taels and 32 gold yuanbao, one per restock.
        list.add(new MerchantOffer(taels(SECT_TOKEN_TAELS), java.util.Optional.of(new ItemCost(MurimItems.GOLD_YUANBAO.get(), SECT_TOKEN_YUANBAO)),
                new ItemStack(MurimItems.SECT_TOKEN.get()), 1, 0, 0.0F));
        // Locator pendants, his dearest wares after it: two full stacks of taels each, one of each per restock.
        MurimItems.pendants().forEach(pendant -> list.add(new MerchantOffer(taels(64), java.util.Optional.of(taels(64)),
                new ItemStack(pendant.get()), 1, 0, 0.0F)));
        // Spoils he takes for taels.
        list.add(buys(MurimItems.BANDIT_DAO.get(), 1, 3, 8));
        list.add(buys(MurimItems.CHIEF_GREAT_AXE.get(), 1, 12, 2));
        for (Item piece : new Item[] { MurimItems.CHIEF_BROW_BAND.get(), MurimItems.CHIEF_LAMELLAR_VEST.get(),
                MurimItems.CHIEF_TASSETS.get(), MurimItems.CHIEF_FUR_BOOTS.get() })
            list.add(buys(piece, 1, 8, 2));
        list.add(buys(Items.TRIDENT, 1, 10, 2));
        list.add(buys(Items.LEATHER, 6, 1, 12));
        list.add(buys(Items.COD, 8, 1, 12));
        return list;
    }

    // ---- Merchant

    @Override public void setTradingPlayer(@Nullable Player player) { tradingPlayer = player; }
    @Override @Nullable public Player getTradingPlayer() { return tradingPlayer; }
    @Override public MerchantOffers getOffers() { return offers != null ? offers : (offers = wares()); }
    @Override public void overrideOffers(MerchantOffers offers) { }
    @Override public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        ambientSoundTime = -getAmbientSoundInterval();
        playSound(SoundEvents.VILLAGER_YES, 0.6F, 0.9F);
    }
    @Override public void notifyTradeUpdated(ItemStack stack) { }
    @Override public int getVillagerXp() { return 0; }
    @Override public void overrideXp(int xp) { }
    @Override public boolean showProgressBar() { return false; }
    @Override public boolean canRestock() { return true; }
    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }
    @Override public boolean isClientSide() { return level().isClientSide; }

    // ---- his stall

    /** Keeps him about his stall, around this block. */
    public void setHome(BlockPos home) {
        restrictTo(home, HOME_RADIUS);
    }

    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
        if (!hasRestriction()) setHome(blockPosition());
        setPersistenceRequired();
        return result;
    }

    @Override public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hasRestriction()) tag.put("Home", NbtUtils.writeBlockPos(getRestrictCenter()));
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Home").ifPresent(this::setHome);
    }

    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PLAYER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }
}
