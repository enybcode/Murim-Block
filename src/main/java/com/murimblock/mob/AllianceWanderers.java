package com.murimblock.mob;

import com.murimblock.Murimblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The Alliance's soldiers out in the world, besides the outposts' garrisons (docs/mobs/alliance-soldiers.md):
 * <ul>
 *   <li><b>patrols</b>: a sergeant leading two spear guards, a sword disciple and an archer, walking the Alliance's lands
 *   (biome tag alliance_patrols: every Murim biome but the Cult's desert and the inner world). About one every ten
 *   minutes near a player who has no patrol within 128 blocks;</li>
 *   <li><b>warrior monks</b> on the mountain roads (biome tag warrior_monks: the cloud-sea peaks, the jade karst, the
 *   Tianshan and the Kunlun), alone or two together, about one every five minutes near a player with fewer than two
 *   monks about him.</li>
 * </ul>
 * They come 24 to 48 blocks from the player, on the surface, and are gone again once nobody is near (not persistent).
 * Neutral like every soldier. They need doMobSpawning, as any mob.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class AllianceWanderers {
    public static final TagKey<Biome> PATROL_BIOMES = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "alliance_patrols"));
    public static final TagKey<Biome> MONK_BIOMES = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "warrior_monks"));
    /** How often each player's surroundings are rolled for (ticks), and the odds then. */
    static final int INTERVAL = 600, PATROL_ODDS = 20, MONK_ODDS = 10;
    /** How far from the player they come (blocks), and how far around him one already counts. */
    static final int NEAR = 24, FAR = 48, PATROL_SPACE = 128, MONK_SPACE = 96;

    private AllianceWanderers() { }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (level.getGameTime() % INTERVAL != 0 || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            if (random.nextInt(PATROL_ODDS) == 0 && around(level, player, PATROL_SPACE, AllianceSoldierEntity::onPatrol) == 0) {
                BlockPos at = spot(level, player, random);
                if (at != null && level.getBiome(at).is(PATROL_BIOMES)) spawnPatrol(level, at);
            }
            if (random.nextInt(MONK_ODDS) == 0 && around(level, player, MONK_SPACE, s -> s.kind() == AllianceSoldierEntity.Kind.WARRIOR_MONK && !s.isPersistenceRequired()) < 2) {
                BlockPos at = spot(level, player, random);
                if (at != null && level.getBiome(at).is(MONK_BIOMES)) spawnMonks(level, at, random.nextInt(3) == 0 ? 2 : 1);
            }
        }
    }

    /** Wandering soldiers of this sort about the player. */
    private static int around(ServerLevel level, ServerPlayer player, int range, java.util.function.Predicate<AllianceSoldierEntity> which) {
        return level.getEntitiesOfClass(AllianceSoldierEntity.class, new AABB(player.blockPosition()).inflate(range), which).size();
    }

    /** A spot on the surface 24 to 48 blocks from the player, dry and with room to stand, in a loaded chunk; or null. */
    static BlockPos spot(ServerLevel level, ServerPlayer player, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        int distance = NEAR + random.nextInt(FAR - NEAR + 1);
        int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * distance), z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return null;
        BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        return standable(level, at) ? at : null;
    }

    static boolean standable(ServerLevel level, BlockPos at) {
        return level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)
                && level.getFluidState(at.below()).isEmpty() && level.getBlockState(at).isAir() && level.getBlockState(at.above()).isAir();
    }

    /** A patrol at this spot: the sergeant leads, two spear guards, a sword disciple and an archer follow him. */
    public static List<AllianceSoldierEntity> spawnPatrol(ServerLevel level, BlockPos at) {
        List<AllianceSoldierEntity> patrol = new ArrayList<>();
        AllianceSoldierEntity leader = spawn(level, MurimEntities.ALLIANCE_SERGEANT.get(), at);
        if (leader == null) return patrol;
        leader.leadPatrol();
        patrol.add(leader);
        List<EntityType<AllianceSoldierEntity>> others = List.of(MurimEntities.ALLIANCE_SPEAR_GUARD.get(), MurimEntities.ALLIANCE_SPEAR_GUARD.get(),
                MurimEntities.ALLIANCE_SWORD_DISCIPLE.get(), MurimEntities.ALLIANCE_ARCHER.get());
        for (int i = 0; i < others.size(); i++) {
            BlockPos beside = at.offset(i % 2 == 0 ? -1 - i / 2 : 1 + i / 2, 0, -1 - i / 2);
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, beside);
            AllianceSoldierEntity member = spawn(level, others.get(i), standable(level, ground) ? ground : at);
            if (member == null) continue;
            member.followPatrol(leader);
            patrol.add(member);
        }
        return patrol;
    }

    /** One or two warrior monks walking the mountain roads. */
    public static List<AllianceSoldierEntity> spawnMonks(ServerLevel level, BlockPos at, int count) {
        List<AllianceSoldierEntity> monks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AllianceSoldierEntity monk = spawn(level, MurimEntities.ALLIANCE_WARRIOR_MONK.get(), i == 0 ? at : at.east());
            if (monk != null) monks.add(monk);
        }
        return monks;
    }

    private static AllianceSoldierEntity spawn(ServerLevel level, EntityType<AllianceSoldierEntity> type, BlockPos at) {
        return type.spawn(level, at, MobSpawnType.PATROL);
    }

}
