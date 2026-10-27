package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.dev.EngineTest.Step;
import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.ClipEvent;
import com.murimblock.shenfa.client.CameraShake;
import com.murimblock.shenfa.client.ClipEvents;
import com.murimblock.shenfa.client.Footsteps;
import com.murimblock.shenfa.client.PlayerAnimators;
import com.murimblock.shenfa.client.SkeletalPlayers;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Step 1.1 in game, on the real player: step sounds come from the feet as they land (vanilla's muted), a clip's root
 * motion carries the body the way it faces and stops at a wall, and clip events (a sound, footsteps, a particle at a
 * joint, a camera shake) fire once each at their time.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
final class EventChecks {
    private static final int AREA = 360;
    private static final double X = AREA + 0.5, Y = 200, Z = 0.5;
    /** A wall across the floor, its near face at this z. */
    private static final int WALL = 8;
    private static final ResourceLocation STEP_IN = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID,
            "fists/step_in");
    private static final ResourceLocation WAVE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "test/wave");
    /** The step_in root travels 12 model pixels: 12 × 0.9375 / 16 blocks. */
    private static final double STEP_IN_TRAVEL = 12 * 0.9375 / 16;

    /** A scripted stretch of ticks: {@code tick} runs each tick, {@code end} once after the last. */
    private record Run(int ticks, Consumer<Integer> tick, Runnable end) {
    }

    private static Run running;
    private static int at;
    private static final List<ClipEvent> fired = new ArrayList<>();
    private static float mostShake;

    private EventChecks() {
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("events setup", EventChecks::setup));
        steps.add(new Step("footsteps while walking", mc -> {
            PlayerAnimators.combat(false);
            at(mc, X - 4, Z, -90);
            start(new Run(40, t -> {
                if (t == 0) {
                    Footsteps.played = Footsteps.heard = Footsteps.muted = 0;
                }
                mc.options.keyUp.setDown(t >= 5);
            }, () -> {
                mc.options.keyUp.setDown(false);
                // 35 ticks at 4.3 blocks a second and 0.9 block a step: about 8 steps.
                EngineTest.check("walking plays a step per foot landing (" + Footsteps.played + " in 1.75 s)",
                        Footsteps.played >= 5 && Footsteps.played <= 12);
                EngineTest.check("vanilla's step sounds are muted (" + Footsteps.muted + " of " + Footsteps.heard + ")",
                        Footsteps.heard > 0 && Footsteps.muted == Footsteps.heard);
            }));
        }));
        steps.add(new Step("footsteps of the boxing shuffle", mc -> {
            PlayerAnimators.combat(true);
            at(mc, X - 4, Z, -90);
            start(new Run(40, t -> {
                if (t == 0) {
                    Footsteps.played = 0;
                }
                mc.options.keyUp.setDown(t >= 5);
            }, () -> {
                mc.options.keyUp.setDown(false);
                // Recorded only: the shuffle still slides (a walking leftover), so its feet plant less often.
                Murimblock.LOGGER.info("Engine test events: the boxing shuffle played {} steps in 1.75 s",
                        Footsteps.played);
            }));
        }));
        steps.add(stepIn("root motion facing south", 0, false, 0, 1));
        steps.add(stepIn("root motion facing east", -90, false, 1, 0));
        steps.add(stepIn("root motion mirrored", 0, true, 0, 1));
        steps.add(new Step("root motion stops at a wall", mc -> {
            at(mc, X, WALL - 0.9, 0);
            Vec3[] from = new Vec3[1];
            start(new Run(20, t -> {
                if (t == 2) {
                    from[0] = mc.player.position();
                    SkeletalPlayers.play(STEP_IN, Animator.Priority.HIGH, 1, false);
                }
            }, () -> {
                double z = mc.player.position().z;
                EngineTest.check(String.format("the wall stops the lunge (z %.2f to %.2f, the wall at %d)", from[0].z,
                        z, WALL), z > from[0].z + 0.2 && z <= WALL - 0.3 + 1e-3);
            }));
        }));
        steps.add(new Step("events of a looping clip", mc -> {
            at(mc, X, Z, 0);
            start(new Run(45, t -> {
                if (t == 2) {
                    fired.clear();
                    mostShake = 0;
                    SkeletalPlayers.play(WAVE, Animator.Priority.HIGH, 1, false);
                }
                mostShake = Math.max(mostShake, CameraShake.now());
            }, () -> {
                SkeletalPlayers.stop(Animator.Priority.HIGH);
                // 43 ticks of a 2 s loop with its events at 1 s: they fire once, 20 ticks in.
                EngineTest.check("the particle event fires once (" + count("particle") + ")", count("particle") == 1);
                EngineTest.check("the shake event fires once (" + count("shake") + ")", count("shake") == 1);
                EngineTest.check(String.format("and shakes the camera (%.2f)", mostShake), mostShake > 0.3f);
            }));
        }));
        steps.add(new Step("events done", mc -> {
            ClipEvents.probe(null);
            PlayerAnimators.combat(false);
            SkeletalPlayers.enable(false);
            EngineTest.busy = () -> false;
        }));
        return steps;
    }

    /** Plays step_in standing still facing {@code yaw}: the body moves its travel along (dx, dz), its events fired. */
    private static Step stepIn(String name, float yaw, boolean mirrored, int dx, int dz) {
        return new Step(name, mc -> {
            at(mc, X, Z, yaw);
            Vec3[] from = new Vec3[1];
            int[] played = new int[1];
            start(new Run(20, t -> {
                if (t == 2) {
                    fired.clear();
                    played[0] = Footsteps.played;
                    from[0] = mc.player.position();
                    SkeletalPlayers.play(STEP_IN, Animator.Priority.HIGH, 1, mirrored);
                }
            }, () -> {
                Vec3 moved = mc.player.position().subtract(from[0]);
                double along = moved.x * dx + moved.z * dz, across = moved.x * dz - moved.z * dx;
                EngineTest.check(String.format("%s: the body travels %.2f blocks ahead (clip %.2f), %.2f aside", name,
                        along, STEP_IN_TRAVEL, across), Math.abs(along - STEP_IN_TRAVEL) < 0.1 && Math.abs(across) < 0.05);
                EngineTest.check(name + ": a swing sound and two footsteps fire (" + types() + ")",
                        types().equals(List.of("sound", "footstep", "footstep")));
                String first = fired.size() > 1 ? fired.get(1).string("foot", "") : "";
                EngineTest.check(name + ": the first footstep is the " + (mirrored ? "right" : "left") + " foot",
                        first.equals(mirrored ? "right" : "left"));
                EngineTest.check(name + ": the footsteps sound (" + (Footsteps.played - played[0]) + ")",
                        Footsteps.played - played[0] >= 2);
            }));
        });
    }

    private static List<String> types() {
        return fired.stream().map(ClipEvent::type).toList();
    }

    private static long count(String type) {
        return fired.stream().filter(event -> event.type().equals(type)).count();
    }

    private static void setup(Minecraft mc) {
        mc.options.pauseOnLostFocus = false;
        mc.options.hideGui = true;
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        SkeletalPlayers.pose(SkeletalPlayers.ANIMATED);
        SkeletalPlayers.enable(true);
        PlayerAnimators.force(null);
        PlayerAnimators.combat(true);
        ClipEvents.probe(fired::add);
        EngineTest.server(mc, player -> {
            ServerLevel level = player.serverLevel();
            for (BlockPos pos : BlockPos.betweenClosed(AREA - 8, (int) Y - 1, -6, AREA + 8, (int) Y - 1, WALL + 2)) {
                level.setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
            }
            for (BlockPos pos : BlockPos.betweenClosed(AREA - 8, (int) Y, WALL, AREA + 8, (int) Y + 2, WALL)) {
                level.setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
            }
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.removeAllEffects();
        });
        mc.player.getAbilities().flying = false;
        EngineTest.busy = () -> running != null;
    }

    private static void at(Minecraft mc, double x, double z, float yaw) {
        EngineTest.server(mc, player -> player.connection.teleport(x, Y, z, yaw, 0));
        mc.player.setYRot(yaw);
        mc.player.setYBodyRot(yaw);
        mc.player.setXRot(0);
        mc.player.setDeltaMovement(0, 0, 0);
    }

    /** Starts {@code run} some ticks from now, once the teleport has reached the client and the body has landed. */
    private static void start(Run run) {
        at = -10;
        running = run;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (running == null || mc.player == null || mc.isPaused()) {
            return;
        }
        if (at >= 0) {
            running.tick().accept(at);
        }
        if (++at >= running.ticks()) {
            Run done = running;
            running = null;
            done.end().run();
        }
    }
}
