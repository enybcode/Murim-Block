package com.murimblock.shenfa.client;

import com.murimblock.Murimblock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

/**
 * Step sounds when a foot comes down, not on vanilla's distance count: Minecraft plays a step every so far the body
 * moves, out of time with the legs; here each planted foot plays its own step, from the block under that foot.
 * Vanilla's step sounds are muted for a body whose feet Shenfa plants ({@link FootPlanting#ownsSteps}); one that is
 * not drawn (out of view) keeps vanilla's.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class Footsteps {
    /** A vanilla step sound this close (blocks) to a body whose steps are ours is that body's. */
    private static final double NEAR = 1.5;
    /** For checks: steps played here, and vanilla step sounds heard near a skeletal body, of which muted. */
    public static int played, heard, muted;

    private Footsteps() {
    }

    /**
     * The step sound of the ground under {@code at} (a sole, in the world), as vanilla plays it for the body: the
     * block's step sound at 0.15 of its volume, a carpet or snow layer over it in its place.
     *
     * @param volume a share of vanilla's volume (a shuffle in place is softer)
     */
    public static void step(LivingEntity player, Vec3 at, float volume) {
        Level level = player.level();
        BlockPos pos = BlockPos.containing(at.x, at.y - 0.2, at.z);
        BlockState state = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());
        if (above.is(BlockTags.INSIDE_STEP_SOUND_BLOCKS) || above.is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
            pos = pos.above();
            state = above;
        }
        if (state.isAir()) {
            return;
        }
        SoundType sound = state.getSoundType(level, pos, player);
        played++;
        level.playLocalSound(at.x, at.y, at.z, sound.getStepSound(), player.getSoundSource(),
                sound.getVolume() * 0.15f * volume, sound.getPitch(), false);
    }

    @SubscribeEvent
    public static void mutePosition(PlayLevelSoundEvent.AtPosition event) {
        if (ours(event.getSound(), event.getPosition())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void muteEntity(PlayLevelSoundEvent.AtEntity event) {
        if (event.getEntity() instanceof AbstractClientPlayer && ours(event.getSound(), event.getEntity().position())) {
            event.setCanceled(true);
        }
    }

    /** Whether {@code sound} is a block's step sound played at a body whose steps Shenfa plays. */
    private static boolean ours(Holder<SoundEvent> sound, Vec3 at) {
        Minecraft mc = Minecraft.getInstance();
        if (sound == null || mc.level == null || !sound.value().getLocation().getPath().endsWith(".step")) {
            return false;
        }
        for (AbstractClientPlayer player : mc.level.players()) {
            if (player.position().closerThan(at, NEAR)) {
                heard++;
                if (FootPlanting.ownsSteps(player)) {
                    muted++;
                    return true;
                }
            }
        }
        return false;
    }
}
