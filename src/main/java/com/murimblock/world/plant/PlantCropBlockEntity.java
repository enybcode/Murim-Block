package com.murimblock.world.plant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A planted herb's or flower's block entity: the purity its harvest will have (it grows with the stages grown in good
 * conditions), and its animations (GeckoLib): the sway of its rarity always, and the spirit's motes at spirit maturity.
 */
public class PlantCropBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation SWAY = RawAnimation.begin().thenLoop("sway");
    private static final RawAnimation SPIRIT = RawAnimation.begin().thenLoop("spirit");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int purity = -1;

    public PlantCropBlockEntity(BlockPos pos, BlockState state) {
        super(MurimFlora.CROP_ENTITY.get(), pos, state);
    }

    /** The purity: a plant found at spirit maturity in the wild (no purity saved yet) is a pure one. */
    public int purity() {
        if (purity < 0) return getBlockState().getValue(PlantCropBlock.AGE) == PlantGrowth.SPIRIT ? 95 : PlantGrowth.START_PURITY;
        return purity;
    }

    void grew(PlantGrowth.Condition condition, int toStage) {
        purity = PlantGrowth.purityAfter(purity(), condition, toStage);
        setChanged();
    }

    /** Harvested and kept: it grows back from young, its purity a little lower. */
    void harvested() {
        purity = Math.max(PlantGrowth.MIN_PURITY, purity() - 5);
        setChanged();
    }

    public void setPurityForTests(int purity) {
        this.purity = purity;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("purity", purity());
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("purity")) purity = tag.getInt("purity");
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "sway", 0, state -> state.setAndContinue(SWAY)));
        controllers.add(new AnimationController<>(this, "spirit", 0, state -> {
            PlantCatalog.Species species = PlantCropBlock.species(getBlockState());
            if (species.stagesModelled() < 4 || getBlockState().getValue(PlantCropBlock.AGE) != PlantGrowth.SPIRIT) return PlayState.STOP;
            return state.setAndContinue(SPIRIT);
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
