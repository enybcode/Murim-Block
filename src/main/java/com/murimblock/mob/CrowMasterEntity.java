package com.murimblock.mob;

import com.murimblock.integration.epicfight.EpicFightMobSupport;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.RavenFamiliarEntity;
import com.murimblock.martial.RavenFx;
import com.murimblock.world.raven.RavenBlocks;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Crow Master (docs/mobs/crow-master.md): the old hermit of the Raven Tower who keeps the Raven Messenger Art, his
 * Shadow Raven always by him (scripts/animation/crow-master-3d.cjs). Right-clicked, he speaks a line (the first time,
 * and from then on now and then), then opens Minecraft's trading screen: the art's scroll for {@link #SCROLL_PRICE}
 * silver taels, an iron fan, raven perches. He cannot be struck: a blow meets only feathers, he steps out of a door behind
 * whoever struck and will not trade with anyone for {@link #SULK_TICKS}. He keeps to his tower and never despawns.
 */
public class CrowMasterEntity extends PathfinderMob implements Merchant {
    /** The Raven Messenger Art's scroll, in silver taels (an intermediate art: dearer than the starting arts). */
    public static final int SCROLL_PRICE = 64;
    /** How long he will not trade after being struck (ticks: 30 s). */
    public static final int SULK_TICKS = 600;
    public static final int HOME_RADIUS = 4;
    public static final int RESTOCK_TICKS = 12000;
    static final double TRADE_REACH = 8.0;
    /** His lines, by number (lang: entity.murimblock.crow_master.line.N). */
    static final int LINES = 5;

    @Nullable private Player tradingPlayer;
    @Nullable private MerchantOffers offers;
    @Nullable private RavenFamiliarEntity raven;
    private long lastRestock;
    private int sulkUntil;
    private final Set<UUID> met = new HashSet<>();

    public CrowMasterEntity(EntityType<? extends CrowMasterEntity> type, Level level) {
        super(type, level);
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EpicFightMobSupport.humanoidAttributes(PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 16).add(Attributes.STEP_HEIGHT, 1.0));
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.7));
        WaterAvoidingRandomStrollGoal stroll = new WaterAvoidingRandomStrollGoal(this, 0.4);
        stroll.setInterval(240);
        goalSelector.addGoal(6, stroll);
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /** Whether he sulks (struck a moment ago): he will not trade. */
    public boolean sulking() {
        return tickCount < sulkUntil;
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
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
        // His raven, always by him.
        if ((raven == null || raven.isRemoved()) && tickCount % 20 == 0 && level() instanceof ServerLevel level) {
            raven = MurimEntities.RAVEN_FAMILIAR.get().create(level);
            if (raven != null) {
                raven.owner(getUUID()).setPos(getX(), getY() + getBbHeight() + 0.4, getZ());
                level.addFreshEntity(raven);
            }
        }
    }

    // ---- struck: only feathers

    @Override public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        Entity by = source.getEntity();
        if (by == null) return false;
        ServerLevel level = (ServerLevel) level();
        stopTrading();
        sulkUntil = tickCount + SULK_TICKS;
        // Out of a door behind whoever struck him.
        Vec3 back = Vec3.directionFromRotation(0, by.getYRot()).scale(-2.0), to = by.position().add(back.x, 0, back.z);
        RavenFx.door(level, position(), getYRot(), 12);
        if (level.noCollision(this, getBoundingBox().move(to.subtract(position())))) {
            teleportTo(to.x, to.y, to.z);
            RavenFx.door(level, to, by.getYRot(), 12);
            getLookControl().setLookAt(by);
        }
        if (by instanceof ServerPlayer player) player.displayClientMessage(Component.translatable("entity.murimblock.crow_master.struck"), true);
        return false;
    }

    // ---- trading

    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !isAlive() || tradingPlayer != null && tradingPlayer != player) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.NAME_TAG) || held.getItem() instanceof SpawnEggItem) return InteractionResult.PASS;
        if (!level().isClientSide) {
            if (sulking()) {
                player.displayClientMessage(Component.translatable("entity.murimblock.crow_master.sulk"), true);
                return InteractionResult.CONSUME;
            }
            // His first words to someone, then a line now and then.
            int line = met.add(player.getUUID()) ? 0 : 1 + random.nextInt(LINES - 1);
            player.sendSystemMessage(Component.translatable("entity.murimblock.crow_master.says", getDisplayName(),
                    Component.translatable("entity.murimblock.crow_master.line." + line)));
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

    /** His wares: the art's scroll, an iron fan, raven perches, a pill of his own. */
    public static MerchantOffers wares() {
        MerchantOffers list = new MerchantOffers();
        list.add(new MerchantOffer(new ItemCost(MurimItems.SILVER_TAEL.get(), SCROLL_PRICE), new ItemStack(MurimItems.RAVEN_ART_SCROLL.get()), 1, 0, 0.0F));
        list.add(new MerchantOffer(new ItemCost(MurimItems.SILVER_TAEL.get(), 16), new ItemStack(MurimItems.IRON_FAN.get()), 2, 0, 0.0F));
        list.add(new MerchantOffer(new ItemCost(MurimItems.SILVER_TAEL.get(), 6), new ItemStack(RavenBlocks.RAVEN_PERCH_ITEM.get()), 6, 0, 0.0F));
        list.add(new MerchantOffer(new ItemCost(MurimItems.SILVER_TAEL.get(), 6), new ItemStack(MurimItems.QI_GATHERING_PILL.get()), 4, 0, 0.0F));
        return list;
    }

    @Override public void setTradingPlayer(@Nullable Player player) { tradingPlayer = player; }
    @Override @Nullable public Player getTradingPlayer() { return tradingPlayer; }
    @Override public MerchantOffers getOffers() { return offers != null ? offers : (offers = wares()); }
    @Override public void overrideOffers(MerchantOffers offers) { }
    @Override public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        ambientSoundTime = -getAmbientSoundInterval();
        playSound(SoundEvents.PARROT_AMBIENT, 0.8F, 0.5F);
    }
    @Override public void notifyTradeUpdated(ItemStack stack) { }
    @Override public int getVillagerXp() { return 0; }
    @Override public void overrideXp(int xp) { }
    @Override public boolean showProgressBar() { return false; }
    @Override public boolean canRestock() { return true; }
    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }
    @Override public boolean isClientSide() { return level().isClientSide; }

    // ---- his post

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

    @Override public boolean removeWhenFarAway(double distance) { return false; }

    @Override public void remove(RemovalReason reason) {
        if (raven != null) raven.discard();
        super.remove(reason);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hasRestriction()) tag.put("Home", NbtUtils.writeBlockPos(getRestrictCenter()));
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Home").ifPresent(this::setHome);
    }

    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PARROT_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }
}
