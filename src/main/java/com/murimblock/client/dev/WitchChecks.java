package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.dev.EngineTest.Step;
import com.murimblock.combat.WitchFighting;
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
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The Empoisonneuse des Cinq Venins in game ({@code -PengineGroups=witch}): the witch in her look at rest, then a fight
 * filmed from the side: her vials from afar, her needles and her glide when the player comes close, languor when the
 * player runs, her elixir once struck below half her life.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
final class WitchChecks {
    private static final int AREA = 1000;
    private static final double X = AREA + 0.5, Y = 200, Z = 0.5;
    private static final String TAG = "murimblock_witch_check";
    private static final List<String> MOVES = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static Consumer<Integer> running;
    private static int at, length;
    private static Runnable after;

    private WitchChecks() {
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("witch setup", mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            SkeletalMobs.enable(true);
            EngineTest.server(mc, player -> {
                ServerLevel level = player.serverLevel();
                player.server.setDifficulty(Difficulty.NORMAL, true);
                level.setDayTime(6000);
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 16, (int) Y - 1, -16, AREA + 16, (int) Y - 1, 24)) {
                    level.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
                }
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 16, (int) Y, -16, AREA + 16, (int) Y + 6, 24)) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
                level.getEntitiesOfClass(Entity.class, area(),
                        entity -> !(entity instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
                Witch witch = EntityType.WITCH.create(level);
                witch.moveTo(X, Y, Z + 3, 180, 0);
                witch.setYHeadRot(180);
                witch.setYBodyRot(180);
                witch.setNoAi(true);
                witch.setPersistenceRequired();
                witch.addTag(TAG);
                level.addFreshEntity(witch);
                player.setGameMode(GameType.CREATIVE);
                player.connection.teleport(X, Y + 0.3, Z, 0, 10);
            });
        }));
        // Let the area load and the teleport land before the shots.
        steps.add(new Step("witch settle", mc -> run(60, t -> {
        }, () -> {
        })));
        steps.add(new Step("witch calm", mc -> {
            Witch witch = nearest(mc);
            EngineTest.check("witches wear the Empoisonneuse's look", witch != null
                    && SkeletalMobs.look(witch) == SkeletalMobs.EMPOISONNEUSE
                    && mc.getEntityRenderDispatcher().getRenderer(witch) instanceof SkeletalMobRenderer<?>);
            var clip = witch == null ? null : SkeletalMobs.baseAnimation(witch);
            EngineTest.check("and plays with a vial at rest (" + (clip == null ? null : clip.id()) + ")", clip != null
                    && clip.id().equals("witch/idle"));
            EngineTest.shoot(mc, "mob_witch_front");
            EngineTest.server(mc, player -> player.connection.teleport(X + 2.2, Y + 0.6, Z + 1.4, 58, 12));
        }));
        steps.add(new Step("witch three quarters", mc -> {
            EngineTest.shoot(mc, "mob_witch_three_quarter");
            EngineTest.server(mc, player -> player.connection.teleport(X, Y + 1.2, Z + 1.6, 0, 16));
        }));
        steps.add(new Step("witch close", mc -> EngineTest.shoot(mc, "mob_witch_head")));
        steps.add(new Step("witch fight", mc -> {
            MOVES.clear();
            WitchFighting.probe = (mob, move) -> MOVES.add(move.name());
            EngineTest.server(mc, player -> {
                player.serverLevel().getEntitiesOfClass(Witch.class, area()).forEach(Entity::discard);
                player.setGameMode(GameType.SURVIVAL);
                player.setHealth(player.getMaxHealth());
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 60, 3, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 60, 3, false, false));
                player.connection.teleport(X, Y, Z, 0, 0);
                Witch witch = EntityType.WITCH.create(player.serverLevel());
                witch.moveTo(X, Y, Z + 7, 180, 0);
                witch.setPersistenceRequired();
                witch.addTag(TAG);
                player.serverLevel().addFreshEntity(witch);
                witch.setTarget(player);
            });
            // The script: stand 7 blocks off (a vial); walk up to her (the needles, the glide back); run (languor);
            // strike her below half her life (her elixir).
            run(420, t -> {
                Witch witch = nearest(mc);
                if (t == 0) {
                    GifRecorder.start("witch_fight");
                    GifRecorder.camera(partial -> side(mc, partial));
                }
                if (witch != null) {
                    face(mc, witch);
                }
                if (t == 14 || t == 40 || t == 130 || t == 150 || t == 260 || t == 330) {
                    EngineTest.shoot(mc, "mob_witch_fight_" + t);
                }
                if (t == 120 && witch != null) {
                    away(mc, witch, 2);
                }
                if (t >= 200 && t <= 260 && t % 10 == 0 && witch != null) {
                    away(mc, witch, 4 + (t - 200) / 10.0);
                }
                if (t == 300 && witch != null) {
                    EngineTest.server(mc, player -> {
                        Entity server = player.serverLevel().getEntity(witch.getId());
                        if (server instanceof Witch body) {
                            body.setHealth(body.getMaxHealth() * 0.4f);
                            body.hurt(body.damageSources().playerAttack(player), 0.5f);
                        }
                    });
                }
            }, () -> {
                EngineTest.check("she throws her vials, lashes her needles, glides back and drinks (" + MOVES + ")",
                        MOVES.contains("THROW") && MOVES.contains("NEEDLES") && MOVES.contains("GLIDE")
                                && MOVES.contains("DRINK"));
                WitchFighting.probe = null;
                EngineTest.server(mc, player -> {
                    player.serverLevel().getEntitiesOfClass(Entity.class, area(),
                            entity -> entity.getTags().contains(TAG)).forEach(Entity::discard);
                    player.setGameMode(GameType.CREATIVE);
                    player.removeAllEffects();
                    player.server.setDifficulty(Difficulty.PEACEFUL, true);
                });
            });
        }));
        return steps;
    }

    private static AABB area() {
        return new AABB(AREA - 30, Y - 5, -30, AREA + 30, Y + 10, 30);
    }

    /** The player steps back {@code blocks} from the poisoner, along the line between them. */
    private static void away(Minecraft mc, Witch golem, double blocks) {
        if (golem == null) {
            return;
        }
        Vec3 from = golem.position();
        Vec3 dir = mc.player.position().subtract(from).multiply(1, 0, 1);
        Vec3 to = from.add((dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, -1) : dir.normalize()).scale(blocks));
        EngineTest.server(mc, player -> player.connection.teleport(to.x, Y, to.z, player.getYRot(), 0));
    }

    /** A side view of the fight: halfway between the two, 8 blocks off to the side. */
    private static GifRecorder.View side(Minecraft mc, float partial) {
        Witch golem = nearest(mc);
        Vec3 a = mc.player.getPosition(partial);
        Vec3 b = golem == null ? a.add(0, 0, 4) : golem.getPosition(partial);
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

    private static Witch nearest(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return null;
        }
        return mc.level.getEntitiesOfClass(Witch.class, area(), Entity::isAlive).stream()
                .min(java.util.Comparator.comparingDouble(golem -> golem.distanceToSqr(mc.player))).orElse(null);
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
