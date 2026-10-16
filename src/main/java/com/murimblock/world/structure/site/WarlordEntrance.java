package com.murimblock.world.structure.site;

import com.murimblock.Murimblock;
import com.murimblock.mob.BanditWarlordEntity;
import com.murimblock.mob.MurimEntities;
import com.murimblock.network.WarlordEntrancePayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Bandit Warlord's entrance, once a camp's garrison is dead (Enzo, 16/10/2026: "he falls from the sky like a meteor,
 * with fire, and there will be a cinematic when he spawns"): the war drum before the longhall beats three times, then he
 * falls from the sky onto the plaza, wrapped in fire (BanditWarlordEntity.fallFromSky); where he lands the camp becomes a
 * crater (WarlordCrater). The players in the camp watch it as a film (client/WarlordCinematic), sent at the start.
 * <p>Timing, in ticks from the last death: the drum at {@link #BEATS}, the warlord let fall from {@link #FALL_HEIGHT}
 * blocks above the plaza at {@link #FALL_AT}; he lands some 30 ticks later.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class WarlordEntrance {
    public static final int[] BEATS = {10, 22, 34};
    public static final int FALL_AT = 50, FALL_HEIGHT = 70;
    /** The war drum, in the camp's own frame (BanditCamp.longhall): local x 8..9, z -22..-20. */
    static final int DRUM_X = 8, DRUM_Z = -21;

    private static final class Pending {
        final ServerLevel level;
        final long camp;
        final BlockPos plaza, drum;
        int ticks;

        Pending(ServerLevel level, long camp, BlockPos plaza, BlockPos drum) {
            this.level = level;
            this.camp = camp;
            this.plaza = plaza;
            this.drum = drum;
        }
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    private WarlordEntrance() { }

    /** Whether this camp's warlord is on his way. */
    static boolean pending(long camp) {
        return PENDING.stream().anyMatch(p -> p.camp == camp);
    }

    /** Begins the entrance of a camp's warlord, if that camp stands here (its structure found); else nothing. */
    static void begin(ServerLevel level, long camp) {
        if (pending(camp)) return;
        StructureStart start = find(level, camp);
        if (start == null) return;
        SitePiece piece = null;
        for (StructurePiece p : start.getPieces()) if (p instanceof SitePiece site) { piece = site; break; }
        if (piece == null) return;
        BlockPos origin = piece.origin();
        Canvas frame = Canvas.dry(origin.getX(), origin.getZ(), piece.rotation());
        BlockPos plaza = origin.above();
        BlockPos drum = new BlockPos(frame.wx(DRUM_X, DRUM_Z), origin.getY() + 2, frame.wz(DRUM_X, DRUM_Z));
        beginAt(level, camp, plaza, drum);
    }

    /** Begins the entrance over this plaza, the drum beating here (a camp placed by hand: tests and tours). */
    public static void beginAt(ServerLevel level, long camp, BlockPos plaza, BlockPos drum) {
        if (pending(camp)) return;
        PENDING.add(new Pending(level, camp, plaza, drum));
        WarlordEntrancePayload film = new WarlordEntrancePayload(plaza, drum);
        for (ServerPlayer player : level.players()) if (BanditCampCount.inside(player, plaza)) {
            PacketDistributor.sendToPlayer(player, film);
            // Nothing hurts them while they watch (some 7 s), nor the fall into the crater after.
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 4, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false));
        }
    }

    @Nullable private static StructureStart find(ServerLevel level, long camp) {
        BlockPos at = BlockPos.of(camp);
        for (StructureStart start : level.structureManager().startsForStructure(new ChunkPos(at),
                s -> s instanceof SiteStructure site && site.plan() == SitePlan.BANDIT_CAMP)) {
            if (!start.isValid()) continue;
            for (StructurePiece p : start.getPieces())
                if (p instanceof SitePiece site && site.origin().getX() == at.getX() && site.origin().getZ() == at.getZ()) return start;
        }
        return null;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        for (Pending p : new ArrayList<>(PENDING)) {
            p.ticks++;
            for (int beat : BEATS) if (p.ticks == beat) {
                p.level.playSound(null, p.drum, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.HOSTILE, 6.0F, 0.5F);
                p.level.playSound(null, p.drum, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.6F, 0.4F);
            }
            if (p.ticks == FALL_AT) {
                PENDING.remove(p);
                fall(p.level, p.camp, p.plaza);
            }
        }
    }

    /** Lets the warlord fall from the sky onto the plaza. */
    static BanditWarlordEntity fall(ServerLevel level, long camp, BlockPos plaza) {
        BanditWarlordEntity warlord = MurimEntities.BANDIT_WARLORD.get().create(level);
        if (warlord == null) return null;
        warlord.moveTo(plaza.getX() + 0.5, Math.min(plaza.getY() + FALL_HEIGHT, level.getMaxBuildHeight() - 8), plaza.getZ() + 0.5, 180, 0);
        warlord.finalizeSpawn(level, level.getCurrentDifficultyAt(plaza), MobSpawnType.EVENT, null);
        warlord.fallFromSky(camp);
        level.addFreshEntity(warlord);
        BanditCampCount.markWarlord(level, camp);
        level.playSound(null, plaza.getX(), plaza.getY() + 30, plaza.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0F, 0.6F);
        return warlord;
    }

    /** Tests: lets a camp's warlord fall now. */
    public static BanditWarlordEntity fallForTests(ServerLevel level, long camp, BlockPos plaza) {
        return fall(level, camp, plaza);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }
}
