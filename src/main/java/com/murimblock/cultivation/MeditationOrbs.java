package com.murimblock.cultivation;

import com.murimblock.cultivation.inner.InnerWorldService;
import com.murimblock.cultivation.inner.QiMoteEntity;
import com.murimblock.mob.MurimEntities;
import com.murimblock.qi.QiService;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Qi of the place gathering round a cultivator who meditates, as motes they draw in with their gaze (Enzo,
 * 17/10/2026: "comme les orbes d'XP"). Sitting still is enough to cultivate; drawing the motes in cultivates faster:
 * <ul>
 *   <li>after three seconds seated, a mote forms round the body every {@link #SPAWN_EVERY} ticks, {@link #MAX_ALIVE} at
 *   most, one in {@link #GOLDEN_ONE_IN} golden; they drift slowly round it and fade after sixteen seconds;</li>
 *   <li>the player turns the view (the mouse) until a mote is under the crosshair: it comes (the client asks,
 *   CultivationPayloads.Absorb; the server checks it is theirs, a meditation's, and near);</li>
 *   <li>absorbed, it fills the reserve ({@link #RESERVE_SHARE} of Qi Max; a golden one five times as much), and once the
 *   reserve is full, condenses as {@link #ORB_TICKS} more ticks of meditation would (a golden one {@link #GOLDEN_TICKS}),
 *   with the experience orb's sound.</li>
 * </ul>
 * Drawing in every mote makes a meditation about twice as fast; the daily limit of meditation still holds.
 */
public final class MeditationOrbs {
    /** Ticks seated before the first mote, between two motes, and how many may wait at once. */
    public static final int FIRST_AFTER = 60, SPAWN_EVERY = 70, MAX_ALIVE = 5, GOLDEN_ONE_IN = 12;
    /** What one is worth: a share of Qi Max into the reserve, or ticks of condensation once it is full. */
    public static final double RESERVE_SHARE = 0.04;
    public static final int ORB_TICKS = 70, GOLDEN_TICKS = 350;
    /** How far a mote may be from its cultivator to be drawn in (blocks). */
    public static final double REACH = 9;

    private MeditationOrbs() {
    }

    /** Each tick a player meditates, seated `seated` ticks: a new mote now and then (never in the inner world). */
    public static void tick(ServerPlayer player, long seated) {
        if (seated < FIRST_AFTER || (seated - FIRST_AFTER) % SPAWN_EVERY != 0 || InnerWorldService.isInside(player)) return;
        if (motes(player).size() >= MAX_ALIVE) return;
        spawn(player, player.getRandom().nextInt(GOLDEN_ONE_IN) == 0);
    }

    /** A mote round the body: two to four blocks out, from the knees to above the head, in open air. */
    public static QiMoteEntity spawn(ServerPlayer player, boolean golden) {
        ServerLevel level = player.serverLevel();
        QiMoteEntity mote = MurimEntities.QI_MOTE.get().create(level);
        if (mote == null) return null;
        Vec3 at = player.position();
        for (int attempt = 0; attempt < 8; attempt++) {
            double a = player.getRandom().nextDouble() * Math.PI * 2, r = 2.0 + player.getRandom().nextDouble() * 2.0;
            Vec3 p = at.add(Math.cos(a) * r, 0.4 + player.getRandom().nextDouble() * 2.2, Math.sin(a) * r);
            if (!level.getBlockState(BlockPos.containing(p)).isAir() && attempt < 7) continue;
            mote.moveTo(p.x, p.y, p.z);
            break;
        }
        mote.setOwner(player.getUUID());
        mote.setKind(golden ? QiMoteEntity.GOLDEN : QiMoteEntity.MEDITATION);
        level.addFreshEntity(mote);
        level.sendParticles(ParticleTypes.END_ROD, mote.getX(), mote.getY() + 0.25, mote.getZ(), 4, 0.1, 0.1, 0.1, 0.01);
        return mote;
    }

    /** This player's meditation motes waiting round them. */
    public static List<QiMoteEntity> motes(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(QiMoteEntity.class, new AABB(player.blockPosition()).inflate(REACH + 2),
                m -> m.meditation() && player.getUUID().equals(m.owner()));
    }

    /** The client says its cultivator's gaze rests on a mote: it comes, if it is theirs, a meditation's, and near. */
    public static boolean draw(ServerPlayer player, int entityId) {
        if (!MeditationService.isMeditating(player)) return false;
        if (!(player.serverLevel().getEntity(entityId) instanceof QiMoteEntity mote)) return false;
        if (!mote.meditation() || mote.pulled() || !player.getUUID().equals(mote.owner())) return false;
        if (mote.distanceToSqr(player) > REACH * REACH) return false;
        mote.pull();
        return true;
    }

    /** A mote reaches the chest: the reserve fills, or (full) the Qi condenses; the experience orb's sound. */
    public static void absorbed(ServerPlayer player, QiMoteEntity mote) {
        boolean golden = mote.kind() == QiMoteEntity.GOLDEN;
        double max = QiService.getQiMax(player), missing = max - QiService.getQi(player);
        if (missing > 1e-6) QiService.addQi(player, Math.min(missing, max * RESERVE_SHARE * (golden ? 5 : 1)));
        else MeditationService.condense(player, golden ? GOLDEN_TICKS : ORB_TICKS);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), golden ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, golden ? 1.0F : 0.35F, golden ? 1.4F : 0.7F + player.getRandom().nextFloat() * 0.7F);
        level.sendParticles(golden ? ParticleTypes.WAX_OFF : ParticleTypes.END_ROD, player.getX(), player.getY() + 0.8, player.getZ(),
                golden ? 14 : 5, 0.25, 0.3, 0.25, 0.02);
    }
}
