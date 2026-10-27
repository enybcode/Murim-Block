package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.dev.EngineTest.Step;
import com.murimblock.shenfa.Animator;
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
@net.neoforged.fml.common.EventBusSubscriber(modid = Murimblock.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
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
                // The row's chunks stay loaded with their entities while the group runs.
                for (int x = (AREA - 8) >> 4; x <= (AREA + ROW.size() * 4 + 8) >> 4; x++) {
                    for (int z = -1; z <= 0; z++) {
                        level.setChunkForced(x, z, true);
                    }
                }
                // Evening: undead would burn in the sun.
                level.setDayTime(12900);
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 4, (int) Y - 1, -6, AREA + ROW.size() * 4,
                        (int) Y - 1, 10)) {
                    level.setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
                }
                player.connection.teleport(AREA + ROW.size() * 2, Y + 6, -14, 0, 20);
            });
        }));
        // Mobs are added once the player stands there: in chunks whose entities are not loaded they would be lost.
        steps.add(new Step("mobs spawn", mc -> {
            EngineTest.server(mc, player -> {
                ServerLevel level = player.serverLevel();
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
                    boolean added = level.addFreshEntity(entity);
                    if (!added || entity.isRemoved()) {
                        Murimblock.LOGGER.warn("Engine test: {} not added ({}, removed {})", ROW.get(i), added,
                                entity.getRemovalReason());
                    }
                }
                Murimblock.LOGGER.info("Engine test: {} spawned, player at {}", ROW.size(), player.blockPosition());
            });
        }));
        steps.add(new Step("mob rigs", mc -> {
            EngineTest.server(mc, player -> Murimblock.LOGGER.info("Engine test mob row on the server: {}", player.serverLevel()
                    .getEntities((Entity) null, new AABB(AREA - 8, Y - 300, -10, AREA + ROW.size() * 4 + 8, Y + 10, 14),
                            entity -> !(entity instanceof Player)).stream().map(entity -> entity.getType().toShortString()
                            + "@" + Math.round(entity.getX()) + "," + Math.round(entity.getY())).toList()));
            List<LivingEntity> mobs = mobs(mc);
            Murimblock.LOGGER.info("Engine test mob row: {}", mobs.stream().map(mob -> mob.getType().toShortString()
                    + "@" + Math.round(mob.getX()) + "," + Math.round(mob.getY()) + "," + Math.round(mob.getZ())).toList());
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
        steps.add(new Step("mob attacks filmed", mc -> filmAttacks(mc)));
        steps.add(new Step("mob attacks saved", mc -> EngineTest.check("every patched mob is filmed mid-attack",
                filming == null)));
        return steps;
    }

    /** One mob to film: which, and the moment of its attack (seconds) to film it at. */
    private record Take(LivingEntity mob, ResourceLocation clip, float at) {
    }

    private static java.util.Deque<Take> filming;
    private static Take current;
    private static com.mojang.blaze3d.platform.NativeImage sheet;
    private static int cell, wait;
    private static boolean frozen;
    private static int tries;
    private static boolean started;
    private static final int CELL_W = 300, CELL_H = 300, COLUMNS = 6;
    /** A mob's three moments sit side by side; each new clip starts from its beginning. */

    /** Each patched mob of the row, seen from its side, frozen in the middle of its first attack's window. */
    private static void filmAttacks(Minecraft mc) {
        filming = new java.util.ArrayDeque<>();
        for (LivingEntity mob : mobs(mc)) {
            com.murimblock.combat.MobPatches patch = com.murimblock.combat.MobPatches.of(mob.getType());
            if (patch == null) {
                continue;
            }
            ResourceLocation clip = patch.attacks().get(0).clip();
            var animation = MobShenfa.clip(mob, clip);
            if (animation == null || animation.attack() == null) {
                continue;
            }
            var phase = animation.attack().phases().get(0);
            for (float at : new float[] {phase.windup(), (phase.activeFrom() + phase.activeTo()) / 2, phase.recovery()}) {
                filming.add(new Take(mob, clip, at));
            }
        }
        int rows = (filming.size() + COLUMNS - 1) / COLUMNS;
        sheet = new com.mojang.blaze3d.platform.NativeImage(CELL_W * COLUMNS, CELL_H * Math.max(1, rows), true);
        sheet.fillRect(0, 0, sheet.getWidth(), sheet.getHeight(), 0xFF202020);
        cell = 0;
        current = null;
        EngineTest.busy = () -> filming != null;
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onFrame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
        if (filming == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (wait > 0) {
            wait--;
            return;
        }
        if (current == null) {
            current = filming.poll();
            if (current == null) {
                java.io.File file = new java.io.File(mc.gameDirectory, "screenshots/film/mob_attacks.png");
                file.getParentFile().mkdirs();
                try {
                    sheet.writeToFile(file);
                } catch (java.io.IOException e) {
                    Murimblock.LOGGER.error("Engine test: could not save {}", file, e);
                }
                sheet.close();
                sheet = null;
                filming = null;
                EngineTest.busy = () -> false;
                return;
            }
            LivingEntity mob = current.mob();
            // From in front and a little to its left (the row runs along X, so not from the side).
            double size = Math.max(3, mob.getBbHeight() * 1.6 + 1.6);
            double camX = mob.getX() + size * 0.45, camZ = mob.getZ() + size;
            float yaw = (float) Math.toDegrees(Math.atan2(mob.getZ() - camZ, mob.getX() - camX)) - 90;
            EngineTest.server(mc, player -> player.connection.teleport(camX, mob.getY() + mob.getBbHeight() * 0.5 - 1.4,
                    camZ, yaw, 0));
            frozen = false;
            started = false;
            tries = 0;
            wait = 12;
            return;
        }
        LivingEntity mob = current.mob();
        if (++tries > 240 || mob.isRemoved()) {
            Murimblock.LOGGER.warn("Engine test: could not film {} playing {}", mob.getType().toShortString(),
                    current.clip());
            current = null;
            return;
        }
        if (!frozen) {
            if (!started) {
                MobShenfa.unfreeze(mob);
                MobShenfa.stop(mob, Animator.Priority.HIGH);
                MobShenfa.play(mob, Animator.Priority.HIGH, current.clip(), 1, false);
                started = true;
                return;
            }
            if (MobShenfa.time(mob, Animator.Priority.HIGH) >= current.at()) {
                MobShenfa.freeze(mob, 40);
                frozen = true;
                wait = 3;
            }
            return;
        }
        try (com.mojang.blaze3d.platform.NativeImage shot = net.minecraft.client.Screenshot.takeScreenshot(
                mc.getMainRenderTarget())) {
            int x = Math.max(0, shot.getWidth() / 2 - CELL_W / 2), y = Math.max(0, shot.getHeight() / 2 - CELL_H / 2);
            shot.copyRect(sheet, x, y, (cell % COLUMNS) * CELL_W, (cell / COLUMNS) * CELL_H, CELL_W, CELL_H, false,
                    false);
        }
        Murimblock.LOGGER.info("Engine test mob film cell {}: {} {}", cell, mob.getType().toShortString(),
                current.clip());
        cell++;
        current = null;
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
