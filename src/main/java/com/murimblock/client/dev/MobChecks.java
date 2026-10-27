package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.dev.EngineTest.Step;
import com.murimblock.shenfa.client.mob.MobRig;
import com.murimblock.shenfa.client.mob.MobShenfa;
import com.murimblock.shenfa.client.mob.PartRig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Mobs on Shenfa in game: every vanilla mob stands in a row (no AI), its rig is read from its model and logged
 * (the part names clips are keyed on), and each plays a clip of its own through the hook in the renderer.
 */
final class MobChecks {
    private static final int AREA = 420;
    private static final double Y = 200;
    /** Mobs that fight, in the row; the order is the row's. */
    static final List<EntityType<?>> ROW = List.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED,
            EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIFIED_PIGLIN, EntityType.SKELETON, EntityType.STRAY,
            EntityType.WITHER_SKELETON, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.VINDICATOR,
            EntityType.PILLAGER, EntityType.EVOKER, EntityType.WITCH, EntityType.ILLUSIONER, EntityType.SPIDER,
            EntityType.CAVE_SPIDER, EntityType.CREEPER, EntityType.ENDERMAN, EntityType.IRON_GOLEM, EntityType.RAVAGER,
            EntityType.HOGLIN, EntityType.ZOGLIN, EntityType.BLAZE, EntityType.SLIME, EntityType.MAGMA_CUBE,
            EntityType.PHANTOM, EntityType.GUARDIAN, EntityType.WARDEN, EntityType.WOLF, EntityType.POLAR_BEAR,
            EntityType.GOAT, EntityType.BEE, EntityType.VEX, EntityType.SILVERFISH, EntityType.ENDERMITE,
            EntityType.BREEZE);

    private MobChecks() {
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("mobs setup", mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            EngineTest.server(mc, player -> {
                // The test world is peaceful: monsters would vanish.
                player.server.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
                ServerLevel level = player.serverLevel();
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 4, (int) Y - 1, -6, AREA + ROW.size() * 4,
                        (int) Y - 1, 10)) {
                    level.setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
                }
                level.getEntities((Entity) null, new AABB(AREA - 8, Y - 4, -10, AREA + ROW.size() * 4 + 8, Y + 10, 14),
                        entity -> !(entity instanceof Player)).forEach(Entity::discard);
                for (int i = 0; i < ROW.size(); i++) {
                    Entity entity = ROW.get(i).create(level);
                    if (entity instanceof Mob mob) {
                        mob.setNoAi(true);
                        mob.setPersistenceRequired();
                        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(AREA, Y, 0)),
                                MobSpawnType.COMMAND, null);
                    }
                    entity.moveTo(AREA + i * 4 + 0.5, Y, 4.5, 180, 0);
                    level.addFreshEntity(entity);
                }
                player.connection.teleport(AREA + ROW.size() * 2, Y + 6, -14, 0, 20);
            });
        }));
        steps.add(new Step("mob rigs", mc -> {
            List<LivingEntity> mobs = mobs(mc);
            EngineTest.check("every mob of the row is there (" + mobs.size() + " of " + ROW.size() + ")",
                    mobs.size() == ROW.size());
            int rigged = 0;
            for (LivingEntity mob : mobs) {
                MobRig rig = MobShenfa.rig(mob);
                if (rig != null) {
                    rigged++;
                }
                Murimblock.LOGGER.info("Engine test mob rig {}: {}", BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()),
                        rig == null ? "none" : rig.biped() ? "biped" : String.join(", ", ((PartRig) rig).names()));
            }
            EngineTest.check("every mob has a rig (" + rigged + ")", rigged == mobs.size());
        }));
        return steps;
    }

    /** The row's mobs, west to east. */
    static List<LivingEntity> mobs(Minecraft mc) {
        List<LivingEntity> mobs = new ArrayList<>(mc.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(AREA - 2, Y - 2, 0, AREA + ROW.size() * 4 + 2, Y + 6, 9), entity -> !(entity instanceof Player)));
        mobs.sort((a, b) -> Double.compare(a.getX(), b.getX()));
        return mobs;
    }

    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
    }
}
