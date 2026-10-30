package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.dev.EngineTest.Step;
import com.murimblock.client.innerworld.TrialEntry;
import com.murimblock.combat.DiscipleFighting;
import com.murimblock.innerworld.TrialZombie;
import com.murimblock.registry.MurimEntities;
import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.client.PlayerAnimators;
import com.murimblock.shenfa.client.SkeletalMobs;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The mortal trial on Shenfa ({@code -PengineGroups=trial}; storyboard {@code docs/mobs/boards/epreuve.png}): one of
 * the dead climbs out of its grave and only then walks, the Grave Warden comes out of the mausoleum and fights at its
 * slower pace, the player gets up from the ground. Filmed from the side.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
final class TrialChecks {
    private static final int AREA = 1020;
    private static final double X = AREA + 0.5, Y = 200, Z = 0.5;
    private static Consumer<Integer> running;
    private static int at, length;
    private static Runnable after;

    private TrialChecks() {
    }

    private static String middle(Mob mob) {
        Animation clip = mob == null ? null : SkeletalMobs.playing(mob, Animator.Priority.MIDDLE);
        return clip == null ? null : clip.id();
    }

    /** Spawns one of the dead (or the Warden) {@code ahead} blocks south of the player, facing it; its id lands in
     * {@code id}. */
    private static void spawn(Minecraft mc, boolean warden, double ahead, int[] id) {
        EngineTest.server(mc, player -> {
            TrialZombie zombie = MurimEntities.TRIAL_ZOMBIE.get().create(player.serverLevel());
            zombie.moveTo(X, Y, Z + ahead, 180, 0);
            zombie.setYHeadRot(180);
            zombie.setYBodyRot(180);
            if (warden) {
                zombie.makeWarden();
            }
            zombie.setTarget(player);
            player.serverLevel().addFreshEntity(zombie);
            id[0] = zombie.getId();
        });
    }

    private static Mob mob(Minecraft mc, int[] id) {
        return mc.level.getEntity(id[0]) instanceof Mob mob ? mob : null;
    }

    private static void discard(Minecraft mc, int[] id) {
        EngineTest.server(mc, player -> {
            Entity entity = player.serverLevel().getEntity(id[0]);
            if (entity != null) {
                entity.discard();
            }
        });
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("trial setup", mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            EngineTest.server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                // The test world is peaceful: the dead would not fight.
                player.server.setDifficulty(Difficulty.NORMAL, true);
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 12, (int) Y - 1, -12, AREA + 12, (int) Y - 1, 12)) {
                    level.setBlockAndUpdate(pos, Blocks.COARSE_DIRT.defaultBlockState());
                }
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 12, (int) Y, -12, AREA + 12, (int) Y + 6, 12)) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
                level.getEntitiesOfClass(Entity.class, new AABB(AREA - 20, Y - 5, -20, AREA + 20, Y + 10, 20),
                        entity -> !(entity instanceof Player)).forEach(Entity::discard);
                player.setGameMode(GameType.SURVIVAL);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 120, 4, false, false));
                player.connection.teleport(X, Y, Z, 0, 0);
            });
        }));
        steps.add(new Step("trial settle", mc -> run(40, t -> {
        }, () -> {
        })));
        int[] dead = new int[1];
        steps.add(new Step("trial climb", mc -> {
            spawn(mc, false, 4, dead);
            GifRecorder.start("trial_climb");
            String[] seen = new String[1];
            double[] start = new double[1];
            run(80, t -> {
                Mob mob = mob(mc, dead);
                if (t == 0 && mob != null) {
                    GifRecorder.camera(GifRecorder.side(mob));
                }
                if (t == 0) {
                    seen[0] = middle(mob);
                    start[0] = mob == null ? 0 : mob.getZ();
                }
                if (t == 30) {
                    EngineTest.shoot(mc, "trial_climb_30");
                }
                if (t == 25 && mob != null) {
                    // Still coming out: it has not moved.
                    start[0] = Math.abs(mob.getZ() - start[0]) < 0.05 ? start[0] : Double.NaN;
                }
            }, () -> {
                Mob mob = mob(mc, dead);
                EngineTest.check("one of the dead is a Disciple déchu",
                        mob != null && SkeletalMobs.look(mob) == SkeletalMobs.DISCIPLE);
                EngineTest.check("it climbs out of its grave (" + seen[0] + ")", seen[0] != null && seen[0].endsWith("disciple/climb"));
                EngineTest.check("it stays put while it climbs", !Double.isNaN(start[0]));
                EngineTest.check(String.format("then it comes for the player (%.2f blocks off)", mob == null ? -1 : mob.distanceTo(mc.player)),
                        mob != null && mob.distanceTo(mc.player) < 3.8);
                discard(mc, dead);
            });
        }));
        int[] warden = new int[1];
        steps.add(new Step("trial warden", mc -> {
            spawn(mc, true, 6, warden);
            GifRecorder.start("trial_warden");
            String[] seen = new String[1];
            List<DiscipleFighting.Move> moves = new ArrayList<>();
            float[] pace = {0, -1};
            DiscipleFighting.probe = (mob, move) -> moves.add(move);
            run(200, t -> {
                Mob mob = mob(mc, warden);
                if (t == 0 && mob != null) {
                    GifRecorder.camera(GifRecorder.side(mob));
                }
                if (t == 0) {
                    seen[0] = middle(mob);
                }
                if (t == 34) {
                    EngineTest.shoot(mc, "trial_warden_roar");
                }
                String clip = middle(mob);
                if (t > 60 && mob != null && clip != null && !clip.endsWith("warden_entrance")) {
                    // How far its move's clip went in a tick: 0.85 of a tick at its pace.
                    float time = SkeletalMobs.time(mob, Animator.Priority.MIDDLE) * 20;
                    if (pace[1] >= 0 && time > pace[1] && time - pace[1] < 1.5f && pace[0] == 0) {
                        pace[0] = time - pace[1];
                    }
                    pace[1] = time;
                } else {
                    pace[1] = -1;
                }
            }, () -> {
                DiscipleFighting.probe = null;
                Mob mob = mob(mc, warden);
                EngineTest.check("the Grave Warden wears its own body",
                        mob != null && SkeletalMobs.look(mob) == SkeletalMobs.WARDEN);
                EngineTest.check("it comes out of the mausoleum (" + seen[0] + ")",
                        seen[0] != null && seen[0].endsWith("disciple/warden_entrance"));
                EngineTest.check("then it fights as a disciple (" + moves + ")", !moves.isEmpty());
                EngineTest.check(String.format("at its slower pace (%.2f of a tick per tick)", pace[0]),
                        Math.abs(pace[0] - TrialZombie.WARDEN_PACE) < 0.03f);
                discard(mc, warden);
            });
        }));
        steps.add(new Step("trial rise", mc -> {
            GifRecorder.start("trial_rise");
            GifRecorder.camera(GifRecorder.side(mc.player));
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            TrialEntry.rise(mc.player);
            String[] seen = new String[1];
            boolean[] held = new boolean[1];
            run(80, t -> {
                if (t == 4) {
                    Animation clip = PlayerAnimators.playing(mc.player, Animator.Priority.LOWEST);
                    seen[0] = clip == null ? null : clip.id();
                    held[0] = PlayerAnimators.bodyHeld.test(mc.player);
                }
                if (t == 30 || t == 45) {
                    EngineTest.shoot(mc, "trial_rise_" + t);
                }
            }, () -> {
                EngineTest.check("the player gets up on Shenfa (" + seen[0] + ")", seen[0] != null && seen[0].endsWith("trial/rise"));
                EngineTest.check("its body is held meanwhile", held[0]);
                EngineTest.check("and free once up", !PlayerAnimators.bodyHeld.test(mc.player));
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.hideGui = false;
                EngineTest.server(mc, player -> player.server.setDifficulty(Difficulty.PEACEFUL, true));
            });
        }));
        return steps;
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
