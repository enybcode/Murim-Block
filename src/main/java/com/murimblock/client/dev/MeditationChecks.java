package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.cultivation.StageBreakthrough;
import com.murimblock.client.cultivation.vision.ClosedEyes;
import com.murimblock.client.dev.EngineTest.Step;
import com.murimblock.cultivation.Meditation;
import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.client.PlayerAnimators;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The lotus meditation on Shenfa ({@code -PengineGroups=meditation}): the player sits down, breathes, is lifted by a
 * stage breakthrough and stands up; the camera follows the head in first person and glides behind for the
 * breakthrough. Filmed from the side, with first-person shots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
final class MeditationChecks {
    private static final int AREA = 980;
    private static final double X = AREA + 0.5, Y = 200, Z = 0.5;
    private static Consumer<Integer> running;
    private static int at, length;
    private static Runnable after;

    private MeditationChecks() {
    }

    private static String seat(Minecraft mc) {
        Animation clip = PlayerAnimators.playing(mc.player, Animator.Priority.LOWEST);
        return clip == null ? null : clip.id();
    }

    /** The camera's height above the feet (blocks). */
    private static double eye(Minecraft mc) {
        return mc.gameRenderer.getMainCamera().getPosition().y - mc.player.getY();
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("meditation setup", mc -> {
            // The checks film the seated body: the eyes stay open.
            ClosedEyes.autoClose = false;
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            EngineTest.server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 12, (int) Y - 1, -12, AREA + 12, (int) Y - 1, 12)) {
                    level.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
                }
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 12, (int) Y, -12, AREA + 12, (int) Y + 5, 12)) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
                level.setBlockAndUpdate(new BlockPos(AREA, (int) Y, 4), Blocks.LANTERN.defaultBlockState());
                level.getEntitiesOfClass(Entity.class, new AABB(AREA - 20, Y - 5, -20, AREA + 20, Y + 10, 20),
                        entity -> !(entity instanceof Player)).forEach(Entity::discard);
                player.setGameMode(GameType.SURVIVAL);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 120, 4, false, false));
                player.connection.teleport(X, Y, Z, 0, 0);
            });
        }));
        steps.add(new Step("meditation settle", mc -> run(40, t -> {
        }, () -> {
        })));
        double[] standing = new double[1];
        steps.add(new Step("meditation sit", mc -> {
            standing[0] = eye(mc);
            GifRecorder.start("meditation_sit");
            GifRecorder.camera(GifRecorder.side(mc.player));
            EngineTest.server(mc, Meditation::start);
            String[] first = new String[1];
            run(36, t -> {
                if (t == 4) {
                    first[0] = seat(mc);
                }
                if (t == 10) {
                    EngineTest.shoot(mc, "meditation_fp_sitting");
                }
            }, () -> {
                EngineTest.check("sitting down plays the sit clip (" + first[0] + ")", "meditation/sit".equals(first[0]));
                EngineTest.check("then the lotus (" + seat(mc) + ")", seat(mc) != null && seat(mc).startsWith("meditation/lotus"));
                EngineTest.check("the body is held: no planted feet, no held items", PlayerAnimators.bodyHeld.test(mc.player));
                EngineTest.check(String.format("the eye goes down with the head (%.2f -> %.2f)", standing[0], eye(mc)),
                        standing[0] > 1.5 && eye(mc) < 1.15 && eye(mc) > 0.7);
                EngineTest.shoot(mc, "meditation_fp_seated");
            });
        }));
        steps.add(new Step("meditation breathe", mc -> {
            double[] range = {9, -9};
            run(100, t -> {
                range[0] = Math.min(range[0], eye(mc));
                range[1] = Math.max(range[1], eye(mc));
            }, () -> EngineTest.check(String.format("the eye rises and falls with the breath (%.3f)", range[1] - range[0]),
                    range[1] - range[0] > 0.01 && range[1] - range[0] < 0.15));
        }));
        steps.add(new Step("meditation look down", mc -> {
            mc.player.setXRot(70);
            run(10, t -> {
            }, () -> {
                EngineTest.shoot(mc, "meditation_fp_down");
                mc.player.setXRot(0);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                mc.options.hideGui = true;
            });
        }));
        steps.add(new Step("meditation front", mc -> run(6, t -> {
        }, () -> {
            EngineTest.shoot(mc, "meditation_front");
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = false;
        })));
        steps.add(new Step("meditation breakthrough", mc -> {
            GifRecorder.start("meditation_breakthrough");
            GifRecorder.camera(GifRecorder.side(mc.player));
            StageBreakthrough.start(mc.player.getId(), 0, 2);
            String[] lifted = new String[1];
            CameraType[] middle = new CameraType[1];
            run(90, t -> {
                if (t == 20) {
                    Animation clip = PlayerAnimators.playing(mc.player, Animator.Priority.LOW);
                    lifted[0] = clip == null ? null : clip.id();
                    middle[0] = mc.options.getCameraType();
                }
                if (t == 4 || t == 36 || t == 66) {
                    EngineTest.shoot(mc, "meditation_breakthrough_" + t);
                }
            }, () -> {
                EngineTest.check("a breakthrough lifts the body (" + lifted[0] + ")", "meditation/breakthrough".equals(lifted[0]));
                EngineTest.check("the camera steps behind meanwhile, then back to the eye",
                        middle[0] == CameraType.THIRD_PERSON_BACK && mc.options.getCameraType() == CameraType.FIRST_PERSON);
                EngineTest.check("still seated after it (" + seat(mc) + ")", seat(mc) != null && seat(mc).startsWith("meditation/lotus"));
            });
        }));
        steps.add(new Step("meditation stand", mc -> {
            GifRecorder.start("meditation_stand");
            GifRecorder.camera(GifRecorder.side(mc.player));
            EngineTest.server(mc, player -> Meditation.stop(player, null));
            String[] first = new String[1];
            run(40, t -> {
                if (t == 4) {
                    first[0] = seat(mc);
                }
            }, () -> {
                EngineTest.check("standing up plays the stand clip (" + first[0] + ")", "meditation/stand".equals(first[0]));
                EngineTest.check("then the body is free again", !PlayerAnimators.bodyHeld.test(mc.player));
                EngineTest.check(String.format("and the eye back up (%.2f)", eye(mc)), eye(mc) > 1.5);
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
