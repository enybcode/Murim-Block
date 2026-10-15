package com.murimblock.cultivation;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.network.CultivationPayloads;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * What the server does to show a breakthrough (the particles are drawn by every client that sees the body, from
 * {@link CultivationPayloads.Breakthrough}): the message, the sound, the pose, and for a great one the blast that pushes
 * the mobs around back and the news told to the whole server.
 */
public final class CultivationEffects {
    /** How far a great breakthrough's blast pushes mobs back (blocks). */
    public static final double BLAST_RADIUS = 6;

    private CultivationEffects() {
    }

    public static void show(Entity body, int kind) {
        if (body instanceof ServerPlayer player) PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new CultivationPayloads.Breakthrough(body.getId(), kind));
        else PacketDistributor.sendToPlayersTrackingEntity(body, new CultivationPayloads.Breakthrough(body.getId(), kind));
    }

    private static void sound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void play(ServerPlayer player, yesman.epicfight.api.animation.AnimationManager.AnimationAccessor<? extends yesman.epicfight.api.animation.types.StaticAnimation> animation) {
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (patch != null) patch.playAnimationSynchronized(animation, 0.0F);
    }

    static Component stageName(CultivationData data) {
        return Component.translatable("gui.murimblock.stage." + data.stage().serializedName())
                .append(" ").append(Component.translatable("gui.murimblock.realm." + data.realm().serializedName()));
    }

    /** A stage crossed: the breath drawn in (if seated), the spiral, a bell, a line in the chat. */
    static void minorBreakthrough(ServerPlayer player, CultivationData reached) {
        show(player, CultivationPayloads.Breakthrough.MINOR);
        sound(player, SoundEvents.BELL_RESONATE, 0.6F, 1.6F);
        sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.8F);
        if (MeditationService.isMeditating(player)) play(player, MurimAnimations.MEDITATE_PULSE);
        player.sendSystemMessage(Component.translatable("cultivation.murimblock.minor", stageName(reached)));
    }

    /** Qi Max reached the gate of the next realm: the way through is the Heart Demon, in the inner world. */
    static void bottleneck(ServerPlayer player) {
        CultivationData data = CultivationService.getCultivation(player);
        CultivationProgression.getNext(data).ifPresent(next -> player.sendSystemMessage(Component.translatable("cultivation.murimblock.bottleneck",
                Component.translatable("gui.murimblock.realm." + next.realm().serializedName()), Component.keybind("key.murimblock.charge_qi"))));
    }

    /** A new realm: the body rises in the lotus, a pillar of light, the blast, a gong, and the whole server hears of it. */
    static void greatBreakthrough(ServerPlayer player, CultivationData reached) {
        show(player, CultivationPayloads.Breakthrough.MAJOR);
        sound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.9F);
        sound(player, SoundEvents.BELL_BLOCK, 2.0F, 0.5F);
        sound(player, SoundEvents.BEACON_POWER_SELECT, 1.5F, 0.7F);
        if (MeditationService.isMeditating(player)) play(player, MurimAnimations.MEDITATE_ASCEND);
        // The blast: the mobs around pushed back (not other players, not the body itself).
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(BLAST_RADIUS),
                e -> e != player && !(e instanceof Player) && e.isAlive())) {
            Vec3 away = e.position().subtract(player.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1e-4) continue;
            e.knockback(1.2, -away.x, -away.z);
            e.hurtMarked = true;
        }
        Component news = Component.translatable("cultivation.murimblock.major", player.getDisplayName(),
                Component.translatable("gui.murimblock.realm." + reached.realm().serializedName()));
        player.server.getPlayerList().broadcastSystemMessage(news, false);
    }

    /** The Qi gone wrong: doubled over, coughing, the reserve gone. */
    static void deviation(ServerPlayer player) {
        show(player, CultivationPayloads.Breakthrough.DEVIATION);
        MeditationService.stop(player, false);
        play(player, MurimAnimations.MEDITATE_DEVIATION);
        sound(player, SoundEvents.PLAYER_HURT, 1.0F, 0.6F);
        sound(player, SoundEvents.WITHER_AMBIENT, 0.4F, 1.6F);
        player.sendSystemMessage(Component.translatable("cultivation.murimblock.deviation"));
    }
}
