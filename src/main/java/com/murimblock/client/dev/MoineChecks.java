package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.dev.EngineTest.Step;
import com.murimblock.combat.MoineFighting;
import com.murimblock.shenfa.client.SkeletalMobRenderer;
import com.murimblock.shenfa.client.SkeletalMobs;
import com.murimblock.shenfa.client.SkeletalModels;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The Moine du Vide in game ({@code -PengineGroups=moine}): the Enderman drawn in his look, calm (blindfold, rosary at
 * his neck), then a fight filmed from the side: he tears the blindfold and rosary off, palms, volleys his beads when
 * his prey backs off, steps through the Void when it runs far, bursts when struck three times in a row.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
final class MoineChecks {
    private static final int AREA = 760;
    private static final double X = AREA + 0.5, Y = 200, Z = 0.5;
    private static final String TAG = "murimblock_moine_check";
    private static final List<String> MOVES = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static Consumer<Integer> running;
    private static int at, length;
    private static Runnable after;

    private MoineChecks() {
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("moine setup", mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            SkeletalMobs.enable(true);
            EngineTest.server(mc, player -> {
                ServerLevel level = player.serverLevel();
                player.server.setDifficulty(Difficulty.NORMAL, true);
                level.setDayTime(18000);
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 120, 0, false, false));
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 16, (int) Y - 1, -16, AREA + 16, (int) Y - 1, 24)) {
                    level.setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
                }
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 16, (int) Y, -16, AREA + 16, (int) Y + 6, 24)) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
                EnderMan monk = EntityType.ENDERMAN.create(level);
                monk.moveTo(X, Y, Z + 4, 180, 0);
                monk.setYHeadRot(180);
                monk.setYBodyRot(180);
                monk.setNoAi(true);
                monk.setPersistenceRequired();
                monk.addTag(TAG);
                level.addFreshEntity(monk);
                player.connection.teleport(X, Y + 0.5, Z, 0, 12);
            });
        }));
        // Let the area load and the teleport land before the calm shots.
        steps.add(new Step("moine settle", mc -> run(60, t -> {
        }, () -> {
        })));
        steps.add(new Step("moine calm", mc -> {
            EnderMan monk = nearest(mc);
            var model = SkeletalModels.get(SkeletalMobs.MOINE.model());
            EngineTest.check("the Moine's model is loaded with its rosary joints", model != null
                    && model.armature().find("Bead_0") != null && model.armature().find("Blindfold") != null);
            EngineTest.check("endermen wear the Moine's look", monk != null && SkeletalMobs.look(monk) == SkeletalMobs.MOINE
                    && mc.getEntityRenderDispatcher().getRenderer(monk) instanceof SkeletalMobRenderer<?>);
            var clip = monk == null ? null : SkeletalMobs.baseAnimation(monk);
            EngineTest.check("and prays when calm (" + (clip == null ? null : clip.id()) + ")", clip != null
                    && clip.id().equals("moine/idle"));
            EngineTest.shoot(mc, "mob_moine_calm_front");
            EngineTest.server(mc, player -> player.connection.teleport(X + 3.2, Y + 1.2, Z + 1.6, 55, 18));
        }));
        steps.add(new Step("moine calm three quarters", mc -> {
            EngineTest.shoot(mc, "mob_moine_calm_three_quarter");
            EngineTest.server(mc, player -> player.connection.teleport(X, Y + 1.4, Z + 2.2, 0, 22));
        }));
        steps.add(new Step("moine calm close", mc -> EngineTest.shoot(mc, "mob_moine_calm_head")));
        steps.add(new Step("moine anger", mc -> {
            EngineTest.server(mc, player -> {
                player.serverLevel().getEntitiesOfClass(EnderMan.class, area()).forEach(Entity::discard);
                player.setGameMode(GameType.SURVIVAL);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 30, 4, false, false));
                player.connection.teleport(X, Y, Z, 0, 0);
                EnderMan monk = EntityType.ENDERMAN.create(player.serverLevel());
                monk.moveTo(X, Y, Z + 5, 180, 0);
                monk.setYHeadRot(180);
                monk.setYBodyRot(180);
                monk.setPersistenceRequired();
                monk.addTag(TAG);
                player.serverLevel().addFreshEntity(monk);
                monk.setTarget(player);
            });
            // A close look at him tearing the blindfold and the rosary off: in front of him, a little to his left.
            run(60, t -> {
                EnderMan monk = nearest(mc);
                if (t == 0) {
                    GifRecorder.start("moine_anger");
                    GifRecorder.camera(partial -> {
                        EnderMan m = nearest(mc);
                        Vec3 at = m == null ? new Vec3(X, Y, Z + 5) : m.getPosition(partial);
                        return new GifRecorder.View(at.add(2.2, 2.6, -3.2), at.add(0, 2.1, 0));
                    });
                }
                if (monk != null) {
                    face(mc, monk);
                }
                if (t == 6 || t == 18 || t == 30) {
                    EngineTest.shoot(mc, "mob_moine_anger_" + t);
                }
            }, () -> {
            });
        }));
        steps.add(new Step("moine fight", mc -> {
            MOVES.clear();
            MoineFighting.probe = (mob, move) -> MOVES.add(move.name());
            EngineTest.server(mc, player -> {
                player.serverLevel().getEntitiesOfClass(EnderMan.class, area()).forEach(Entity::discard);
                player.setGameMode(GameType.SURVIVAL);
                player.setHealth(player.getMaxHealth());
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 60, 3, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 60, 3, false, false));
                player.connection.teleport(X, Y, Z, 0, 0);
                EnderMan monk = EntityType.ENDERMAN.create(player.serverLevel());
                monk.moveTo(X, Y, Z + 6, 180, 0);
                monk.setPersistenceRequired();
                monk.addTag(TAG);
                player.serverLevel().addFreshEntity(monk);
                monk.setTarget(player);
            });
            // The script: stand and watch him tear his blindfold off; let him come and palm; strike three times; back
            // off to 7 blocks (the volley); run 13 blocks off (the step); stand in his window and strike.
            run(560, t -> {
                EnderMan monk = nearest(mc);
                if (t == 0) {
                    GifRecorder.start("moine_fight");
                    GifRecorder.camera(partial -> side(mc, partial));
                }
                if (monk != null) {
                    face(mc, monk);
                }
                if (t == 14 || t == 96 || t == 170 || t == 300 || t == 400 || t == 470) {
                    EngineTest.shoot(mc, "mob_moine_fight_" + t);
                }
                if (monk != null && (t == 70 || t == 74 || t == 78 || t == 330 || t == 334 || t == 450 || t == 454)) {
                    // Three blows in a row (the burst answers), then blows in his open windows.
                    EngineTest.server(mc, player -> {
                        Entity server = player.serverLevel().getEntity(monk.getId());
                        if (server != null && player.distanceTo(server) < 4) {
                            player.attack(server);
                        }
                    });
                }
                if (t == 150) {
                    away(mc, monk, 7);
                }
                if (t == 260) {
                    away(mc, monk, 13);
                }
            }, () -> {
                EngineTest.check("the Moine tears his blindfold off, then palms, volleys, steps and bursts (" + MOVES + ")",
                        MOVES.contains("ANGER") && MOVES.contains("PALM") && MOVES.contains("VOLLEY")
                                && MOVES.contains("STEP"));
                MoineFighting.probe = null;
                EngineTest.server(mc, player -> {
                    player.serverLevel().getEntitiesOfClass(EnderMan.class, area()).forEach(Entity::discard);
                    player.setGameMode(GameType.CREATIVE);
                    player.removeAllEffects();
                    player.serverLevel().setDayTime(6000);
                    player.server.setDifficulty(Difficulty.PEACEFUL, true);
                });
            });
        }));
        return steps;
    }

    private static AABB area() {
        return new AABB(AREA - 30, Y - 5, -30, AREA + 30, Y + 10, 30);
    }

    /** The player steps back {@code blocks} from the monk, along the line between them. */
    private static void away(Minecraft mc, EnderMan monk, double blocks) {
        if (monk == null) {
            return;
        }
        Vec3 from = monk.position();
        Vec3 dir = mc.player.position().subtract(from).multiply(1, 0, 1);
        Vec3 to = from.add((dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, -1) : dir.normalize()).scale(blocks));
        EngineTest.server(mc, player -> player.connection.teleport(to.x, Y, to.z, player.getYRot(), 0));
    }

    /** A side view of the fight: halfway between the two, 8 blocks off to the side. */
    private static GifRecorder.View side(Minecraft mc, float partial) {
        EnderMan monk = nearest(mc);
        Vec3 a = mc.player.getPosition(partial);
        Vec3 b = monk == null ? a.add(0, 0, 4) : monk.getPosition(partial);
        Vec3 mid = a.add(b).scale(0.5).add(0, 1.4, 0);
        Vec3 line = b.subtract(a).multiply(1, 0, 1);
        Vec3 across = line.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : new Vec3(line.z, 0, -line.x).normalize();
        double far = Math.max(7, line.length() * 0.9);
        return new GifRecorder.View(mid.add(across.scale(far)).add(0, 1.2, 0), mid);
    }

    private static void face(Minecraft mc, Entity target) {
        double dx = target.getX() - mc.player.getX(), dz = target.getZ() - mc.player.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        mc.player.setYRot(yaw);
        mc.player.setYHeadRot(yaw);
        mc.player.setXRot(-8);
    }

    private static EnderMan nearest(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return null;
        }
        return mc.level.getEntitiesOfClass(EnderMan.class, area(), Entity::isAlive).stream()
                .min(java.util.Comparator.comparingDouble(monk -> monk.distanceToSqr(mc.player))).orElse(null);
    }

    private static void run(int ticks, Consumer<Integer> tick, Runnable end) {
        at = -10;
        length = ticks;
        running = tick;
        after = end;
        EngineTest.busy = () -> running != null;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (running == null || mc.player == null || mc.isPaused()) {
            return;
        }
        if (at >= 0) {
            running.accept(at);
        }
        if (++at >= length) {
            running = null;
            GifRecorder.stop();
            after.run();
        }
    }
}
