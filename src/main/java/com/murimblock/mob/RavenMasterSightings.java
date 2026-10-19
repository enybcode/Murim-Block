package com.murimblock.mob;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Where and when the Raven Master shows himself (docs/mobs/raven-master.md). He wanders alone: there is at most one of
 * him in the world at a time, and he goes from one cultivator to the next. From Qi Condensation on, about every
 * {@link #INTERVAL} ticks (ten minutes) a player out under the sky sees him: far off ({@link #NEAR} to {@link #FAR}
 * blocks), within their view ({@link #VIEW_HALF_ANGLE} degrees either side of where they look), perched on a high
 * exposed spot (a tree top, a rock, a roof: at least {@link #MIN_RISE} blocks above the ground round it) that their eyes
 * reach, watching them for 20 to 30 s (RavenMasterEntity). Once a player has beaten him he comes to them half as often.
 * Each player's next sighting is kept in their persistent data, so it survives a restart. He needs doMobSpawning and a
 * difficulty other than peaceful, as any monster.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class RavenMasterSightings {
    /** The realm from which he shows himself. */
    public static final CultivationRealm REALM = CultivationRealm.QI_CONDENSATION;
    /** About this long between his sightings to one player (ticks: 10 minutes), a quarter more or less; twice once beaten. */
    public static final int INTERVAL = 12000;
    /** How often the players are looked over (ticks), and the wait when no perch was found (ticks: 30 s). */
    static final int CHECK_EVERY = 100, RETRY = 600;
    /** How far from the player he perches (blocks), how far either side of their look (degrees), how high over the ground round his perch. */
    public static final int NEAR = 40, FAR = 80, VIEW_HALF_ANGLE = 40, MIN_RISE = 3;
    /** Candidate columns tried for a perch. */
    static final int TRIES = 96;
    static final String NEXT = "murimblock.raven_master.next", BEATEN = "murimblock.raven_master.beaten";

    private RavenMasterSightings() { }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (level.getGameTime() % CHECK_EVERY != 0 || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        if (level.getDifficulty() == Difficulty.PEACEFUL || abroad(level)) return;
        long now = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            if (!eligible(player)) continue;
            CompoundTag data = player.getPersistentData();
            if (!data.contains(NEXT)) {
                // The first time a player is ready for him, he comes some time later, not at once.
                data.putLong(NEXT, now + interval(player, level.getRandom()));
                continue;
            }
            if (now < data.getLong(NEXT)) continue;
            boolean shown = appear(level, player) != null;
            data.putLong(NEXT, now + (shown ? interval(player, level.getRandom()) : RETRY));
            if (shown) return;
        }
    }

    /** Whether the Raven Master is out in this world now (he is unique). */
    public static boolean abroad(ServerLevel level) {
        return !level.getEntities(MurimEntities.RAVEN_MASTER.get(), e -> e.isAlive()).isEmpty();
    }

    /** Whether he may show himself to this player: Qi Condensation or above, in survival or adventure, under the sky. */
    public static boolean eligible(ServerPlayer player) {
        if (player.isSpectator() || player.isCreative() || !player.isAlive() || !realmAllows(player)) return false;
        return player.level().canSeeSky(BlockPos.containing(player.getEyePosition()));
    }

    /** Whether this player's cultivation is high enough for him to take notice: Qi Condensation or above. */
    public static boolean realmAllows(Player player) {
        return CultivationService.getRealm(player).ordinal() >= REALM.ordinal();
    }

    /** Ticks to his next sighting for this player: ten minutes, a quarter more or less, twice that once beaten. */
    public static int interval(ServerPlayer player, RandomSource random) {
        int base = beaten(player) ? INTERVAL * 2 : INTERVAL;
        return base * 3 / 4 + random.nextInt(base / 2 + 1);
    }

    public static boolean beaten(Player player) {
        return player.getPersistentData().getBoolean(BEATEN);
    }

    static void markBeaten(ServerPlayer player) {
        player.getPersistentData().putBoolean(BEATEN, true);
    }

    /**
     * Shows him to this player now, if a perch is found (findPerch): he stands there, facing them, watching for 20 to
     * 30 s. Null if there is no perch (or he could not be made).
     */
    @Nullable public static RavenMasterEntity appear(ServerLevel level, ServerPlayer player) {
        Vec3 perch = findPerch(level, player, level.getRandom()).orElse(null);
        if (perch == null) return null;
        RavenMasterEntity master = MurimEntities.RAVEN_MASTER.get().create(level);
        if (master == null) return null;
        double dx = player.getX() - perch.x, dz = player.getZ() - perch.z;
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
        master.moveTo(perch.x, perch.y, perch.z, yaw, 0);
        master.setYHeadRot(yaw);
        master.yBodyRot = yaw;
        master.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(perch)), MobSpawnType.EVENT, null);
        int ticks = RavenMasterEntity.WATCH_MIN + level.getRandom().nextInt(RavenMasterEntity.WATCH_MAX - RavenMasterEntity.WATCH_MIN + 1);
        master.watch(player, ticks);
        level.addFreshEntity(master);
        return master;
    }

    /**
     * A perch for him before this player: of {@link #TRIES} columns {@link #NEAR} to {@link #FAR} blocks off within their
     * view, the top of each (leaves count: a tree top will do) with room above to stand, at least {@link #MIN_RISE}
     * blocks above the ground round it (three blocks away all round, leaves not counted), his eyes there in clear sight
     * of the player's; the one standing highest over its surroundings.
     */
    public static Optional<Vec3> findPerch(ServerLevel level, ServerPlayer player, RandomSource random) {
        Vec3 eye = player.getEyePosition();
        Vec3 best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < TRIES; i++) {
            double yaw = Math.toRadians(player.getYRot() + (random.nextDouble() * 2 - 1) * VIEW_HALF_ANGLE);
            double distance = NEAR + random.nextDouble() * (FAR - NEAR);
            int x0 = Mth.floor(player.getX() - Math.sin(yaw) * distance), z0 = Mth.floor(player.getZ() + Math.cos(yaw) * distance);
            if (!level.isLoaded(new BlockPos(x0, level.getMinBuildHeight(), z0))) continue;
            // Up to the highest top near, step by step: the crown of the tree or the rock this column falls on.
            int x = x0, z = z0, top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x0, z0);
            for (int climb = 0; climb < 4; climb++) {
                int cx = x, cz = z;
                for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                    int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx + dx, cz + dz);
                    if (h > top) {
                        top = h;
                        x = cx + dx;
                        z = cz + dz;
                    }
                }
                if (x == cx && z == cz) break;
            }
            double off = Math.hypot(x + 0.5 - player.getX(), z + 0.5 - player.getZ());
            if (off < NEAR || off > FAR) continue;
            BlockPos feet = new BlockPos(x, top, z);
            BlockPos below = feet.below();
            if (level.getBlockState(below).getCollisionShape(level, below).isEmpty() || !level.getFluidState(below).isEmpty()) continue;
            if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) continue;
            int ground = Integer.MAX_VALUE;
            for (int dx = -3; dx <= 3; dx += 3) for (int dz = -3; dz <= 3; dz += 3) {
                if (dx == 0 && dz == 0) continue;
                ground = Math.min(ground, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz));
            }
            int rise = feet.getY() - ground;
            if (rise < MIN_RISE) continue;
            Vec3 stand = new Vec3(x + 0.5, feet.getY(), z + 0.5), his = stand.add(0, 1.62, 0);
            var hit = level.clip(new ClipContext(eye, his, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS && hit.getLocation().distanceTo(his) > 0.6) continue;
            double score = rise + feet.getY() * 0.25;
            if (score > bestScore) {
                bestScore = score;
                best = stand;
            }
        }
        return Optional.ofNullable(best);
    }
}
