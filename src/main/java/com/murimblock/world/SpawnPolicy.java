package com.murimblock.world;

import com.murimblock.Murimblock;
import com.murimblock.mob.MountainBanditEntity;
import com.murimblock.mob.MurimEntities;
import com.murimblock.world.structure.CrabLairBuilder;
import com.murimblock.world.structure.CrabLairPlacement;
import com.murimblock.world.structure.SanctuaryPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.village.VillageSiegeEvent;

/** Natural overworld ecology. Commands, eggs, spawners and authored structure residents retain their own rules. */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class SpawnPolicy {
    private static final String PENDING_CHECK = "murimblock:pending_spawn_check";
    private SpawnPolicy() { }
    public static boolean randomSpawn(MobSpawnType reason) {
        return reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION
                || reason == MobSpawnType.PATROL || reason == MobSpawnType.REINFORCEMENT;
    }
    private static boolean randomSpawn(EntityType<?> type, MobSpawnType reason) {
        return randomSpawn(reason) || (reason == MobSpawnType.EVENT
                && (type == EntityType.WANDERING_TRADER || type == EntityType.TRADER_LLAMA));
    }
    /** Five blocks below the solid surface: a leaf canopy alone never turns the forest into a cave. */
    public static boolean underground(ServerLevelAccessor level, BlockPos pos) {
        return pos.getY() <= level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) - 5;
    }
    public static boolean checkBanditSpawnRules(EntityType<? extends MountainBanditEntity> type,
            ServerLevelAccessor world, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (!Monster.checkAnyLightMonsterSpawnRules(type, world, reason, pos, random)) return false;
        if (!randomSpawn(reason)) return true;
        if (!world.getLevel().dimension().equals(Level.OVERWORLD) || underground(world, pos)) return false;
        long time = Math.floorMod(world.getLevel().getDayTime(), 24000L);
        return (time >= 13000 && time < 23000) || random.nextInt(4) == 0;
    }
    /** All registered structures, their yards included, plus the mod's unique hand-built sites and explicit regions. */
    public static boolean protectedStructure(ServerLevel level, BlockPos pos) {
        if (SpawnProtection.get(level).contains(pos)) return true;
        for (var start : level.structureManager().startsForStructure(new ChunkPos(pos), s -> true))
            if (start.isValid() && start.getBoundingBox().isInside(pos)) return true;
        if (!level.dimension().equals(Level.OVERWORLD)) return false;
        var sanctuary = SanctuaryPlacement.get(level);
        BlockPos centre = sanctuary.centre();
        if (centre != null && SanctuaryPlacement.near(level, pos, 40)
                && pos.getY() >= centre.getY() - 8 && pos.getY() <= sanctuary.top() + 30) return true;
        BlockPos lair = CrabLairPlacement.get(level).centre();
        return lair != null && Math.abs(pos.getX() - lair.getX()) <= CrabLairBuilder.RADIUS + 4
                && Math.abs(pos.getZ() - lair.getZ()) <= CrabLairBuilder.RADIUS + 4
                && pos.getY() >= lair.getY() - 8 && pos.getY() <= lair.getY() + CrabLairBuilder.DEPTH + 4;
    }
    private static boolean forbidden(EntityType<?> type, ServerLevelAccessor world, BlockPos pos, MobSpawnType reason) {
        if (!randomSpawn(type, reason)) return false;
        // Generation workers only have a bounded chunk view. Loading referenced starts or SavedData through
        // ServerLevel here can deadlock generation. Finalize marks the new entity for its first server tick.
        if (world instanceof WorldGenRegion) return false;
        ServerLevel level = world.getLevel();
        if (protectedStructure(level, pos)) return true;
        if (!level.dimension().equals(Level.OVERWORLD)) return false;
        if (type == MurimEntities.MOUNTAIN_BANDIT.get()) return underground(world, pos);
        return type.getCategory() == MobCategory.MONSTER
                && BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals("minecraft") && !underground(world, pos);
    }
    @SubscribeEvent public static void placement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (forbidden(event.getEntityType(), event.getLevel(), event.getPos(), event.getSpawnType()))
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
    }
    @SubscribeEvent public static void position(MobSpawnEvent.PositionCheck event) {
        if (forbidden(event.getEntity().getType(), event.getLevel(), event.getEntity().blockPosition(), event.getSpawnType()))
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
    }
    /** Phantom and patrol spawners skip ordinary placement/position checks. */
    @SubscribeEvent public static void finalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getLevel() instanceof WorldGenRegion && randomSpawn(event.getEntity().getType(), event.getSpawnType())) {
            event.getEntity().getPersistentData().putBoolean(PENDING_CHECK, true);
            return;
        }
        if (forbidden(event.getEntity().getType(), event.getLevel(), event.getEntity().blockPosition(), event.getSpawnType()))
            event.setSpawnCancelled(true);
    }

    /** Runs once, before AI, after the generated chunk is loaded. The marker survives saving a proto-chunk. */
    @SubscribeEvent public static void firstTick(EntityTickEvent.Pre event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !entity.getPersistentData().getBoolean(PENDING_CHECK)) return;
        entity.getPersistentData().remove(PENDING_CHECK);
        if (forbidden(entity.getType(), level, entity.blockPosition(), MobSpawnType.CHUNK_GENERATION)) {
            entity.discard();
            event.setCanceled(true);
        }
    }

    /** Siege zombies finalize before receiving their position; use the siege's actual candidate position. */
    @SubscribeEvent public static void villageSiege(VillageSiegeEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && forbidden(EntityType.ZOMBIE, level, BlockPos.containing(event.getAttemptedSpawnPos()), MobSpawnType.NATURAL))
            event.setCanceled(true);
    }
}
