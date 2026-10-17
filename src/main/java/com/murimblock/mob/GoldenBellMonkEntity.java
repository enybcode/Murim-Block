package com.murimblock.mob;

import com.murimblock.integration.epicfight.EpicFightMobSupport;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.GoldenBellItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.registry.entries.EpicFightAttributes;

/**
 * The Golden Bell monk (docs/mobs/golden-bell-monk.md): a Shaolin-style warrior monk who keeps the Golden Bell's sutra in
 * the Misty Bamboo Temple's middle court. Shaved head, saffron robe, brown sash, prayer beads, bare forearms in cloth
 * wraps, all in cloth (scripts/animation/golden-bell-monk-3d.cjs).
 * <ul>
 *   <li>Right-clicked, he opens Minecraft's trading screen: the Golden Bell manual for {@link #MANUAL_PRICE} taels (the
 *   Long Fist scroll's price at the Alliance Merchant) and a few temple things.</li>
 *   <li>Neutral: he never strikes first. Struck, he fights back bare-handed with Epic Fight's fists
 *   (integration/epicfight/GoldenBellMonkPatch), and will not trade while angry. Left unstruck for {@link #CALM_TICKS},
 *   he lets it go.</li>
 *   <li>Below half his health, once per fight, he takes the Golden Bell form for {@link #GOLDEN_TICKS}: his skin turns
 *   gold (a synced flag, client/GoldenBellMonkRenderer), he takes {@link #GOLDEN_REDUCTION} less damage and nothing
 *   knocks him back.</li>
 * </ul>
 * He keeps to a few blocks around where he was placed and never despawns; killed, he leaves nothing special.
 */
public class GoldenBellMonkEntity extends PathfinderMob implements Merchant {
    /** The Golden Bell manual's price, in silver taels. */
    public static final int MANUAL_PRICE = 40;
    /** His health. */
    public static final float MAX_HEALTH = 40.0F;
    /** How long the golden form lasts (ticks: 10 s). */
    public static final int GOLDEN_TICKS = 200;
    /** The share of every blow the golden form takes away. */
    public static final float GOLDEN_REDUCTION = 0.4F;
    /** How long without a blow before he calms down (ticks: 30 s). */
    public static final int CALM_TICKS = 600;
    /** How far from his post he strolls (blocks). */
    public static final int HOME_RADIUS = 6;
    /** How often he restocks (ticks: half a Minecraft day). */
    public static final int RESTOCK_TICKS = 12000;
    /** How far a trading player may step away before he closes the trade (blocks). */
    static final double TRADE_REACH = 8.0;

    private static final EntityDataAccessor<Boolean> GOLDEN = SynchedEntityData.defineId(GoldenBellMonkEntity.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation GOLDEN_FOOTING = ResourceLocation.fromNamespaceAndPath("murimblock", "golden_bell_monk_footing");

    @Nullable private Player tradingPlayer;
    @Nullable private MerchantOffers offers;
    private long lastRestock;
    /** When he was last struck (his age), and when his golden form ends. */
    private int lastStruck, goldenUntil;
    /** Whether he has used his golden form in this fight; whether someone struck him and he has not calmed down yet. */
    private boolean goldenUsed, riled;

    public GoldenBellMonkEntity(EntityType<? extends GoldenBellMonkEntity> type, Level level) {
        super(type, level);
        xpReward = 10;
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createMonkAttributes() {
        return EpicFightMobSupport.humanoidAttributes(PathfinderMob.createMobAttributes()
                        .add(Attributes.MAX_HEALTH, MAX_HEALTH).add(Attributes.ARMOR, 2).add(Attributes.ATTACK_DAMAGE, 4)
                        .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 24).add(Attributes.STEP_HEIGHT, 1.0))
                .add(EpicFightAttributes.STUN_ARMOR, 6.0).add(EpicFightAttributes.IMPACT, 1.2);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GOLDEN, false);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
        WaterAvoidingRandomStrollGoal stroll = new WaterAvoidingRandomStrollGoal(this, 0.5);
        stroll.setInterval(200);
        goalSelector.addGoal(6, stroll);
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Neutral: only whoever strikes him. Epic Fight installs his attack and chase goals for his bare hands.
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity target = getTarget();
        // Left unstruck for a while, or his opponent gone, he lets it go: calm, and his golden form ready again.
        if (target != null && (!target.isAlive() || tickCount - lastStruck > CALM_TICKS)) {
            setTarget(null);
            setLastHurtByMob(null);
            target = null;
        }
        if (target == null && tickCount - lastStruck > CALM_TICKS) {
            goldenUsed = false;
            riled = false;
        }
        if (golden() && tickCount >= goldenUntil) leaveGolden();
        setAggressive(target != null);

        Player trader = tradingPlayer;
        if (trader != null) {
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

    // ---- struck

    @Override public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && golden()) amount *= 1.0F - GOLDEN_REDUCTION;
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide) {
            lastStruck = tickCount;
            if (source.getEntity() instanceof LivingEntity) riled = true;
            stopTrading();
            if (!goldenUsed && isAlive() && getHealth() <= getMaxHealth() / 2) enterGolden();
        }
        return hurt;
    }

    /** Whether he is in the Golden Bell form (synced: his skin is gold). */
    public boolean golden() {
        return entityData.get(GOLDEN);
    }

    /** Whether he is angry (fighting someone): he will not trade. */
    public boolean angry() {
        return riled || getTarget() != null;
    }

    private void enterGolden() {
        goldenUsed = true;
        goldenUntil = tickCount + GOLDEN_TICKS;
        entityData.set(GOLDEN, true);
        var footing = getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (footing != null) footing.addOrUpdateTransientModifier(new AttributeModifier(GOLDEN_FOOTING, 1.0, AttributeModifier.Operation.ADD_VALUE));
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.2F, 0.7F);
    }

    private void leaveGolden() {
        entityData.set(GOLDEN, false);
        var footing = getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (footing != null) footing.removeModifier(GOLDEN_FOOTING);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 0.8F, 1.2F);
    }

    // ---- trading

    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !isAlive() || tradingPlayer != null && tradingPlayer != player) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.NAME_TAG) || held.getItem() instanceof SpawnEggItem) return InteractionResult.PASS;
        if (angry()) return InteractionResult.PASS;
        if (!level().isClientSide) {
            setTradingPlayer(player);
            openTradingScreen(player, getDisplayName(), 1);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private void stopTrading() {
        Player trader = tradingPlayer;
        setTradingPlayer(null);
        if (trader instanceof ServerPlayer serverPlayer && serverPlayer.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu)
            serverPlayer.closeContainer();
    }

    /** His wares, always the same: the Golden Bell's sutra, and a few things of the temple. */
    public static MerchantOffers wares() {
        MerchantOffers list = new MerchantOffers();
        list.add(new MerchantOffer(new ItemCost(MurimItems.SILVER_TAEL.get(), MANUAL_PRICE), new ItemStack(GoldenBellItems.MANUAL.get()), 1, 0, 0.0F));
        // The temple's own: Qi Gathering Pills at the Alliance Merchant's price, candles and plain bread for strings of cash.
        list.add(new MerchantOffer(new ItemCost(MurimItems.SILVER_TAEL.get(), 6), new ItemStack(MurimItems.QI_GATHERING_PILL.get()), 4, 0, 0.0F));
        list.add(new MerchantOffer(new ItemCost(MurimItems.CASH_STRING.get(), 1), new ItemStack(Items.CANDLE, 2), 8, 0, 0.0F));
        list.add(new MerchantOffer(new ItemCost(MurimItems.CASH_STRING.get(), 1), new ItemStack(Items.BREAD, 4), 16, 0, 0.0F));
        return list;
    }

    @Override public void setTradingPlayer(@Nullable Player player) { tradingPlayer = player; }
    @Override @Nullable public Player getTradingPlayer() { return tradingPlayer; }
    @Override public MerchantOffers getOffers() { return offers != null ? offers : (offers = wares()); }
    @Override public void overrideOffers(MerchantOffers offers) { }
    @Override public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        ambientSoundTime = -getAmbientSoundInterval();
        playSound(SoundEvents.VILLAGER_YES, 0.6F, 0.8F);
    }
    @Override public void notifyTradeUpdated(ItemStack stack) { }
    @Override public int getVillagerXp() { return 0; }
    @Override public void overrideXp(int xp) { }
    @Override public boolean showProgressBar() { return false; }
    @Override public boolean canRestock() { return true; }
    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }
    @Override public boolean isClientSide() { return level().isClientSide; }

    // ---- his post

    /** Keeps him about his post, around this block. */
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
