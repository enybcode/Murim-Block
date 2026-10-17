package com.murimblock.cultivation.inner;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A mote of Qi, drawn as a bright glowing point, bobbing. Two uses:
 * <ul>
 *   <li>scattered over the inner world (the mission "Gather the Scattered Qi"): its owner gathers it by touching it;</li>
 *   <li>gathering round a cultivator who meditates (MeditationOrbs, Enzo 17/10/2026, like experience orbs): it drifts
 *   round them, and comes when they turn their gaze on it ({@link #pull}); a golden one now and then is worth more. It
 *   fades if left alone, or as soon as they stop.</li>
 * </ul>
 */
public class QiMoteEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(QiMoteEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(QiMoteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PULLED = SynchedEntityData.defineId(QiMoteEntity.class, EntityDataSerializers.BOOLEAN);
    /** What a mote is: the inner world's mission, a meditation's, a meditation's golden one. */
    public static final int MISSION = 0, MEDITATION = 1, GOLDEN = 2;
    /** A meditation mote fades after this long if not drawn in (ticks). */
    public static final int MEDITATION_LIFETIME = 320;
    /** How fast a drawn mote flies in (blocks a tick), and how slowly a free one drifts round its cultivator (radians). */
    static final double PULL_SPEED = 0.32, DRIFT = 0.012;
    /** It fades away after this long if not gathered (ticks). */
    public static final int LIFETIME = 2400;

    public QiMoteEntity(EntityType<? extends QiMoteEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public void setOwner(UUID owner) {
        entityData.set(OWNER, Optional.of(owner));
    }

    public int kind() { return entityData.get(KIND); }
    public void setKind(int kind) { entityData.set(KIND, kind); }
    /** Whether it gathers round a meditation (not the inner world's mission). */
    public boolean meditation() { return kind() != MISSION; }
    public boolean pulled() { return entityData.get(PULLED); }
    /** Drawn in by its cultivator's gaze: it flies to them and is absorbed. */
    public void pull() { entityData.set(PULLED, true); }

    @Nullable public UUID owner() {
        return entityData.get(OWNER).orElse(null);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, Optional.empty());
        builder.define(KIND, MISSION);
        builder.define(PULLED, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (tickCount > (meditation() ? MEDITATION_LIFETIME : LIFETIME)) {
            discard();
            return;
        }
        UUID owner = owner();
        ServerPlayer player = owner == null ? null : server.getServer().getPlayerList().getPlayer(owner);
        if (player == null || player.level() != level()) {
            if (tickCount > 40) discard();
            return;
        }
        if (meditation()) {
            meditationTick(player);
            return;
        }
        if (player.getBoundingBox().inflate(0.6).contains(position())) {
            InnerMissions.moteGathered(player, this);
            discard();
        }
    }

    /** Round a meditation: drifting slowly round the seated body, or flying into its chest once drawn. */
    private void meditationTick(ServerPlayer player) {
        if (!com.murimblock.cultivation.MeditationService.isMeditating(player)) {
            discard();
            return;
        }
        net.minecraft.world.phys.Vec3 chest = player.position().add(0, 0.7, 0), here = position();
        if (pulled()) {
            net.minecraft.world.phys.Vec3 to = chest.subtract(here);
            if (to.length() <= PULL_SPEED + 0.2) {
                com.murimblock.cultivation.MeditationOrbs.absorbed(player, this);
                discard();
                return;
            }
            setPos(here.add(to.normalize().scale(PULL_SPEED)));
            return;
        }
        double dx = here.x - chest.x, dz = here.z - chest.z, c = Math.cos(DRIFT), s = Math.sin(DRIFT);
        setPos(chest.x + dx * c - dz * s, here.y, chest.z + dx * s + dz * c);
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
        entityData.set(KIND, tag.getInt("Kind"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID owner = owner();
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("Kind", kind());
    }
}
