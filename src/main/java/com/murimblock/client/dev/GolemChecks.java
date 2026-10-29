package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.dev.EngineTest.Step;
import com.murimblock.combat.GolemFighting;
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
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The Gardien-talisman in game ({@code -PengineGroups=golem}): the Iron Golem in its look, a still statue beside a
 * villager and a zombie it leaves be; then the player strikes the villager, filmed from the side: it wakes, sky fist,
 * earth bell (the ground fractures), chain when the player backs off, and back to its place once no one is left.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
final class GolemChecks {
    private static final int AREA = 940;
    private static final double X = AREA + 0.5, Y = 200, Z = 0.5;
    private static final String TAG = "murimblock_golem_check";
    private static final List<String> MOVES = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static Consumer<Integer> running;
    private static int at, length;
    private static Runnable after;

    private GolemChecks() {
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("golem setup", mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            SkeletalMobs.enable(true);
            EngineTest.server(mc, player -> {
                ServerLevel level = player.serverLevel();
                player.server.setDifficulty(Difficulty.NORMAL, true);
                level.setDayTime(18000);
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 120, 0, false, false));
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 16, (int) Y - 1, -16, AREA + 16, (int) Y - 1, 24)) {
                    level.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
                }
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 16, (int) Y, -16, AREA + 16, (int) Y + 6, 24)) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
                level.getEntitiesOfClass(Entity.class, area(), entity -> !(entity instanceof net.minecraft.world.entity.player.Player))
                        .forEach(Entity::discard);
                IronGolem golem = EntityType.IRON_GOLEM.create(level);
                golem.moveTo(X, Y, Z + 5, 180, 0);
                golem.setYHeadRot(180);
                golem.setYBodyRot(180);
                golem.addTag(TAG);
                level.addFreshEntity(golem);
                Villager villager = EntityType.VILLAGER.create(level);
                villager.moveTo(X + 3, Y, Z + 2, 90, 0);
                villager.setNoAi(true);
                villager.addTag(TAG);
                level.addFreshEntity(villager);
                Zombie zombie = EntityType.ZOMBIE.create(level);
                zombie.moveTo(X - 3, Y, Z + 3, 270, 0);
                zombie.setNoAi(true);
                zombie.addTag(TAG);
                level.addFreshEntity(zombie);
                player.setGameMode(GameType.CREATIVE);
                player.connection.teleport(X, Y + 0.5, Z, 0, 8);
            });
        }));
        // Let the area load and the teleport land before the shots.
        steps.add(new Step("golem settle", mc -> run(60, t -> {
        }, () -> {
        })));
        steps.add(new Step("golem statue", mc -> {
            IronGolem golem = nearest(mc);
            var model = SkeletalModels.get(SkeletalMobs.GOLEM.model());
            EngineTest.check("the Gardien's model is loaded with its talisman", model != null
                    && model.armature().find("Talisman") != null);
            EngineTest.check("iron golems wear the Gardien's look", golem != null
                    && SkeletalMobs.look(golem) == SkeletalMobs.GOLEM
                    && mc.getEntityRenderDispatcher().getRenderer(golem) instanceof SkeletalMobRenderer<?>);
            var clip = golem == null ? null : SkeletalMobs.baseAnimation(golem);
            EngineTest.check("and stands as a statue (" + (clip == null ? null : clip.id()) + ")", clip != null
                    && clip.id().equals("golem/statue"));
            EngineTest.shoot(mc, "mob_golem_statue_front");
            EngineTest.server(mc, player -> player.connection.teleport(X + 3.6, Y + 1.4, Z + 2.2, 55, 18));
        }));
        steps.add(new Step("golem statue three quarters", mc -> {
            EngineTest.shoot(mc, "mob_golem_statue_three_quarter");
            EngineTest.server(mc, player -> player.connection.teleport(X, Y + 1.6, Z + 2.4, 0, 14));
        }));
        steps.add(new Step("golem statue close", mc -> {
            EngineTest.shoot(mc, "mob_golem_statue_head");
            EngineTest.server(mc, player -> player.connection.teleport(X, Y, Z, 0, 8));
        }));
        steps.add(new Step("golem still", mc -> {
            Vec3[] from = new Vec3[1];
            float[] yaw = new float[1];
            run(100, t -> {
                IronGolem golem = nearest(mc);
                if (t == 0 && golem != null) {
                    from[0] = golem.position();
                    yaw[0] = golem.yBodyRot;
                }
            }, () -> {
                IronGolem golem = nearest(mc);
                EngineTest.check("it stays still, a zombie at its side", golem != null && from[0] != null
                        && golem.position().distanceTo(from[0]) < 0.1 && Math.abs(golem.yBodyRot - yaw[0]) < 1
                        && golem.getTarget() == null);
            });
        }));
        steps.add(new Step("golem fight", mc -> {
            MOVES.clear();
            GolemFighting.probe = (mob, move) -> MOVES.add(move.name());
            EngineTest.server(mc, player -> {
                player.serverLevel().getEntitiesOfClass(Zombie.class, area()).forEach(Entity::discard);
                player.setGameMode(GameType.SURVIVAL);
                player.setHealth(player.getMaxHealth());
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 60, 3, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 60, 3, false, false));
                // A bystander beside the player: the earth bell strikes all around, not only its prey.
                var pig = EntityType.PIG.create(player.serverLevel());
                pig.moveTo(X + 0.6, Y, Z + 2.2, 0, 0);
                pig.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 60, 2, false, false));
                pig.setNoAi(true);
                pig.addTag(TAG);
                player.serverLevel().addFreshEntity(pig);
                player.connection.teleport(X + 2, Y, Z + 1, 0, 0);
            });
            // The script: strike the villager (it wakes); stand in reach (the sky fist, then the bell); back off to
            // 6 blocks (the chain); then out of its reach (creative): it walks back to its place and turns to stone.
            boolean[] home = {false};
            run(640, t -> {
                IronGolem golem = nearest(mc);
                if (t == 0) {
                    GifRecorder.start("golem_fight");
                    GifRecorder.camera(partial -> side(mc, partial));
                }
                if (t == 6) {
                    EngineTest.server(mc, player -> player.serverLevel().getEntitiesOfClass(Villager.class, area())
                            .forEach(player::attack));
                }
                if (golem != null) {
                    face(mc, golem);
                }
                if (t == 14 || t == 60 || t == 140 || t == 230 || t == 300 || t == 600) {
                    EngineTest.shoot(mc, "mob_golem_fight_" + t);
                }
                if (t == 210) {
                    away(mc, golem, 6);
                }
                if (t == 330) {
                    EngineTest.server(mc, player -> player.setGameMode(GameType.CREATIVE));
                }
                if (t == 630 && golem != null) {
                    home[0] = golem.position().distanceTo(new Vec3(X, Y, Z + 5)) < 1.2 && golem.getTarget() == null;
                }
            }, () -> {
                EngineTest.check("harming a villager wakes it, then the sky fist, the bell and the chain (" + MOVES + ")",
                        MOVES.contains("WAKE") && MOVES.contains("SKY") && MOVES.contains("BELL")
                                && MOVES.contains("CHAIN"));
                EngineTest.check("with no one to punish it goes back to its place", home[0]);
                var pigs = mc.level.getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class, area());
                EngineTest.check("the bell strikes a bystander too (" + pigs.stream().map(p -> p.getHealth()).toList() + ")",
                        pigs.stream().anyMatch(p -> p.getHealth() < p.getMaxHealth()));
                GolemFighting.probe = null;
                EngineTest.server(mc, player -> {
                    player.serverLevel().getEntitiesOfClass(Entity.class, area(),
                            entity -> entity.getTags().contains(TAG)).forEach(Entity::discard);
                    player.setGameMode(GameType.CREATIVE);
                    player.removeAllEffects();
                    player.serverLevel().setDayTime(6000);
                    player.server.setDifficulty(Difficulty.PEACEFUL, true);
                });
            });
        }));
        steps.add(new Step("golem death", mc -> {
            EngineTest.server(mc, player -> {
                player.setGameMode(GameType.CREATIVE);
                player.connection.teleport(X, Y + 0.5, Z, 0, 10);
                IronGolem golem = EntityType.IRON_GOLEM.create(player.serverLevel());
                golem.moveTo(X, Y, Z + 4, 180, 0);
                golem.setYHeadRot(180);
                golem.setYBodyRot(180);
                golem.addTag(TAG);
                player.serverLevel().addFreshEntity(golem);
            });
            // Struck down: its talisman flies off, it kneels, and falls apart into its stone parts.
            boolean[] flew = {false};
            int[] most = {0};
            run(150, t -> {
                if (t == 0) {
                    GifRecorder.start("golem_death");
                    GifRecorder.camera(partial -> new GifRecorder.View(new Vec3(X + 3.2, Y + 2.6, Z + 0.6),
                            new Vec3(X, Y + 1.2, Z + 4)));
                }
                if (t == 20) {
                    EngineTest.server(mc, player -> player.serverLevel().getEntitiesOfClass(IronGolem.class, area())
                            .forEach(golem -> golem.hurt(golem.damageSources().playerAttack(player), 1000)));
                }
                flew[0] |= com.murimblock.shenfa.client.GolemCollapse.talismanFlying();
                most[0] = Math.max(most[0], com.murimblock.shenfa.client.GolemCollapse.pieces());
                if (t == 30 || t == 44 || t == 60) {
                    EngineTest.shoot(mc, "mob_golem_death_" + t);
                }
            }, () -> {
                EngineTest.check("at its death the talisman flies off", flew[0]);
                EngineTest.check("and the statue falls apart (" + most[0] + " pieces)", most[0] >= 20);
                EngineTest.server(mc, player -> player.serverLevel().getEntitiesOfClass(Entity.class, area(),
                        entity -> entity.getTags().contains(TAG)).forEach(Entity::discard));
            });
        }));
        return steps;
    }

    private static AABB area() {
        return new AABB(AREA - 30, Y - 5, -30, AREA + 30, Y + 10, 30);
    }

    /** The player steps back {@code blocks} from the guardian, along the line between them. */
    private static void away(Minecraft mc, IronGolem golem, double blocks) {
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
        IronGolem golem = nearest(mc);
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

    private static IronGolem nearest(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return null;
        }
        return mc.level.getEntitiesOfClass(IronGolem.class, area(), Entity::isAlive).stream()
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
