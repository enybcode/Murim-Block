package com.murimblock.anim.client;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.murimblock.Murimblock;
import com.murimblock.anim.Animation;
import com.murimblock.anim.AnimationPlayer;
import com.murimblock.anim.Biped;
import com.murimblock.anim.BipedPoses;
import com.murimblock.anim.Pose;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Which players the skeletal engine draws, and in what pose. Until combat mode exists it is switched by hand with
 * the client command {@code /skeletal on|off}, {@code /skeletal pose <name>} and {@code /skeletal joints on|off} (the
 * debug skeleton view), and {@code /skeletal anim play <clip> [speed]}, {@code /skeletal anim stop},
 * {@code /skeletal anim speed <factor>} (slow motion) play the clips of {@link SkeletalClips}; dev only, not in the
 * lang file.
 * Vanilla keeps the players it handles better for now: sleeping, gliding, swimming, riding, spectating.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SkeletalPlayers {
    private static boolean enabled;
    private static boolean showJoints;
    /** "stand" (vanilla's standing pose) or one of {@link BipedPoses#TEST}. */
    private static String poseName = "stand";
    private static final AnimationPlayer PLAYER = new AnimationPlayer();
    private static float slowMotion = 1;

    private SkeletalPlayers() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void enable(boolean on) {
        enabled = on;
    }

    /** Whether the debug skeleton view is on ({@code /skeletal joints on|off}). */
    public static boolean showJoints() {
        return showJoints;
    }

    public static void showJoints(boolean on) {
        showJoints = on;
    }

    public static void pose(String name) {
        poseName = name;
    }

    public static boolean drawn(AbstractClientPlayer player) {
        return enabled && !player.isSleeping() && !player.isFallFlying() && !player.isVisuallySwimming()
                && !player.isAutoSpinAttack() && !player.isPassenger() && !player.isSpectator();
    }

    /**
     * Plays an animation on every skeletal player (dev: one player for all until the animator of step 5).
     *
     * @return false when no animation has this id
     */
    public static boolean play(ResourceLocation id, float speed) {
        Animation animation = SkeletalClips.get(id);
        if (animation == null) {
            return false;
        }
        PLAYER.play(animation, speed, null);
        return true;
    }

    public static void stop() {
        PLAYER.stop();
    }

    /** The global slow motion factor ({@code /skeletal anim speed}), on top of each clip's speed. */
    public static void slowMotion(float factor) {
        slowMotion = factor;
    }

    public static AnimationPlayer player() {
        return PLAYER;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Minecraft.getInstance().isPaused()) {
            PLAYER.tick(slowMotion);
        }
    }

    /** The pose to draw {@code player} in, helpers settled; a fresh copy the caller may change. */
    public static Pose pose(AbstractClientPlayer player, float partialTick) {
        if (PLAYER.playing()) {
            Pose pose = PLAYER.pose(partialTick, Biped.ARMATURE.bindPose());
            Biped.settleHelpers(pose);
            return pose;
        }
        if (poseName.equals("stand")) {
            boolean rightIsMain = player.getMainArm() == HumanoidArm.RIGHT;
            boolean main = !player.getMainHandItem().isEmpty(), off = !player.getOffhandItem().isEmpty();
            return BipedPoses.stand(player.tickCount + partialTick, rightIsMain ? main : off, rightIsMain ? off : main,
                    player.isCrouching());
        }
        return BipedPoses.test(poseName);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("skeletal")
                .then(Commands.literal("on").executes(context -> {
                    enable(true);
                    context.getSource().sendSystemMessage(Component.literal("Skeletal players: on"));
                    return 1;
                }))
                .then(Commands.literal("off").executes(context -> {
                    enable(false);
                    context.getSource().sendSystemMessage(Component.literal("Skeletal players: off"));
                    return 1;
                }))
                .then(Commands.literal("joints").then(Commands.literal("on").executes(context -> {
                    showJoints = true;
                    return 1;
                })).then(Commands.literal("off").executes(context -> {
                    showJoints = false;
                    return 1;
                })))
                .then(Commands.literal("pose").then(Commands.argument("name", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                Stream.concat(Stream.of("stand"), BipedPoses.TEST.keySet().stream()), builder))
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            pose(name);
                            context.getSource().sendSystemMessage(Component.literal("Skeletal pose: " + name));
                            return 1;
                        })))
                .then(Commands.literal("anim")
                        .then(Commands.literal("play").then(Commands.argument("clip", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        SkeletalClips.all().keySet(), builder))
                                .executes(context -> play(context, 1))
                                .then(Commands.argument("speed", FloatArgumentType.floatArg(0.01f, 10))
                                        .executes(context -> play(context, FloatArgumentType.getFloat(context, "speed"))))))
                        .then(Commands.literal("stop").executes(context -> {
                            stop();
                            return 1;
                        }))
                        .then(Commands.literal("speed").then(Commands.argument("factor", FloatArgumentType.floatArg(0.01f, 10))
                                .executes(context -> {
                                    slowMotion(FloatArgumentType.getFloat(context, "factor"));
                                    context.getSource().sendSystemMessage(Component.literal("Skeletal speed: " + slowMotion));
                                    return 1;
                                })))));
    }

    private static int play(CommandContext<CommandSourceStack> context, float speed) {
        ResourceLocation id = context.getArgument("clip", ResourceLocation.class);
        if (!play(id, speed)) {
            context.getSource().sendFailure(Component.literal("No skeletal clip " + id));
            return 0;
        }
        enable(true);
        context.getSource().sendSystemMessage(Component.literal("Playing " + id));
        return 1;
    }
}
