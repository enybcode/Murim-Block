package com.murimblock.anim.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.murimblock.Murimblock;
import com.murimblock.anim.Animation;
import com.murimblock.anim.AnimationPlayer;
import com.murimblock.anim.Animator;
import com.murimblock.anim.Biped;
import com.murimblock.anim.BipedPoses;
import com.murimblock.anim.LivingMotion;
import com.murimblock.anim.Pose;
import java.util.Arrays;
import java.util.Locale;
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
 * client commands (dev only, not in the lang file):
 * <ul>
 * <li>{@code /skeletal on|off}; {@code /skeletal joints on|off}, the debug skeleton view;</li>
 * <li>{@code /skeletal pose <animated|stand|test pose>}: {@code animated} (the default) runs each player's
 * {@link PlayerAnimators animator}, {@code stand} is vanilla's standing pose, to compare with vanilla;</li>
 * <li>{@code /skeletal anim play <clip> [speed] [priority] [mirrored]} plays a clip on a layer of every player's
 * animator, {@code /skeletal anim stop [priority]} fades it out, {@code /skeletal anim speed <factor>} is slow
 * motion;</li>
 * <li>{@code /skeletal motion <motion|auto>} forces a living motion, {@code /skeletal combat on|off} picks the combat
 * clips of the motion set.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SkeletalPlayers {
    public static final String ANIMATED = "animated";
    public static final String STAND = "stand";
    /** One clip shown on everyone, frame by frame ({@link #preview}): to film a clip next to Blender's render. */
    public static final String PREVIEW = "preview";

    private static boolean enabled;
    private static boolean showJoints;
    private static String poseName = ANIMATED;
    private static final AnimationPlayer PREVIEW_PLAYER = new AnimationPlayer();
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

    /** Whether players are drawn by their animators (rather than a fixed pose). */
    public static boolean animated() {
        return poseName.equals(ANIMATED);
    }

    public static boolean drawn(AbstractClientPlayer player) {
        return enabled;
    }

    /**
     * Plays an animation on a layer of every player's animator.
     *
     * @return false when no animation has this id
     */
    public static boolean play(ResourceLocation id, Animator.Priority priority, float speed, boolean mirrored) {
        Animation animation = SkeletalClips.get(id);
        if (animation == null) {
            return false;
        }
        PlayerAnimators.each(animator -> animator.play(priority, animation, speed, mirrored));
        return true;
    }

    public static void stop(Animator.Priority priority) {
        PlayerAnimators.each(animator -> animator.stop(priority));
    }

    /**
     * Shows {@code id} on every player, frozen at its start ({@link #previewPlayer()} seeks it).
     *
     * @return false when no animation has this id
     */
    public static boolean preview(ResourceLocation id) {
        Animation animation = SkeletalClips.get(id);
        if (animation == null) {
            return false;
        }
        PREVIEW_PLAYER.play(animation, 0, null);
        poseName = PREVIEW;
        return true;
    }

    public static AnimationPlayer previewPlayer() {
        return PREVIEW_PLAYER;
    }

    /** The global slow motion factor ({@code /skeletal anim speed}), on top of each clip's speed. */
    public static float slowMotion() {
        return slowMotion;
    }

    public static void slowMotion(float factor) {
        slowMotion = factor;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Minecraft.getInstance().isPaused()) {
            PREVIEW_PLAYER.tick(slowMotion);
        }
    }

    /** The pose to draw {@code player} in, helpers settled; a fresh copy the caller may change. */
    public static Pose pose(AbstractClientPlayer player, float partialTick) {
        switch (poseName) {
            case ANIMATED -> {
                return PlayerAnimators.pose(player, partialTick);
            }
            case PREVIEW -> {
                Pose pose = PREVIEW_PLAYER.pose(partialTick, Biped.ARMATURE.bindPose());
                Biped.settleHelpers(pose);
                return pose;
            }
            case STAND -> {
                boolean rightIsMain = player.getMainArm() == HumanoidArm.RIGHT;
                boolean main = !player.getMainHandItem().isEmpty(), off = !player.getOffhandItem().isEmpty();
                return BipedPoses.stand(player.tickCount + partialTick, rightIsMain ? main : off,
                        rightIsMain ? off : main, player.isCrouching(),
                        player.isPassenger() && player.getVehicle() != null && player.getVehicle().shouldRiderSit());
            }
            default -> {
                return BipedPoses.test(poseName);
            }
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        String[] priorities = Arrays.stream(Animator.Priority.values()).map(p -> p.name().toLowerCase(Locale.ROOT))
                .toArray(String[]::new);
        String[] motions = Stream.concat(Stream.of("auto"), Arrays.stream(LivingMotion.values()).map(LivingMotion::key))
                .toArray(String[]::new);
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
                                Stream.concat(Stream.of(ANIMATED, STAND), BipedPoses.TEST.keySet().stream()), builder))
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            pose(name);
                            context.getSource().sendSystemMessage(Component.literal("Skeletal pose: " + name));
                            return 1;
                        })))
                .then(Commands.literal("motion").then(Commands.argument("motion", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(motions, builder))
                        .executes(context -> {
                            String key = StringArgumentType.getString(context, "motion");
                            LivingMotion motion = LivingMotion.byKey(key);
                            if (motion == null && !key.equals("auto")) {
                                context.getSource().sendFailure(Component.literal("No motion " + key));
                                return 0;
                            }
                            PlayerAnimators.force(motion);
                            context.getSource().sendSystemMessage(Component.literal("Skeletal motion: " + key));
                            return 1;
                        })))
                .then(Commands.literal("combat").then(Commands.literal("on").executes(context -> {
                    PlayerAnimators.combat(true);
                    return 1;
                })).then(Commands.literal("off").executes(context -> {
                    PlayerAnimators.combat(false);
                    return 1;
                })))
                .then(Commands.literal("anim")
                        .then(Commands.literal("play").then(Commands.argument("clip", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        SkeletalClips.all().keySet(), builder))
                                .executes(context -> play(context, 1, "middle", false))
                                .then(Commands.argument("speed", FloatArgumentType.floatArg(0.01f, 10))
                                        .executes(context -> play(context, speed(context), "middle", false))
                                        .then(Commands.argument("priority", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        priorities, builder))
                                                .executes(context -> play(context, speed(context),
                                                        StringArgumentType.getString(context, "priority"), false))
                                                .then(Commands.argument("mirrored", BoolArgumentType.bool())
                                                        .executes(context -> play(context, speed(context),
                                                                StringArgumentType.getString(context, "priority"),
                                                                BoolArgumentType.getBool(context, "mirrored"))))))))
                        .then(Commands.literal("stop").executes(context -> {
                            stop(Animator.Priority.MIDDLE);
                            return 1;
                        }).then(Commands.argument("priority", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(priorities, builder))
                                .executes(context -> {
                                    stop(priority(StringArgumentType.getString(context, "priority")));
                                    return 1;
                                })))
                        .then(Commands.literal("speed").then(Commands.argument("factor", FloatArgumentType.floatArg(0.01f, 10))
                                .executes(context -> {
                                    slowMotion(FloatArgumentType.getFloat(context, "factor"));
                                    context.getSource().sendSystemMessage(Component.literal("Skeletal speed: " + slowMotion));
                                    return 1;
                                })))));
    }

    private static float speed(CommandContext<CommandSourceStack> context) {
        return FloatArgumentType.getFloat(context, "speed");
    }

    private static Animator.Priority priority(String name) throws CommandSyntaxException {
        try {
            return Animator.Priority.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new SimpleCommandExceptionType(Component.literal("No priority " + name)).create();
        }
    }

    private static int play(CommandContext<CommandSourceStack> context, float speed, String priority, boolean mirrored)
            throws CommandSyntaxException {
        ResourceLocation id = context.getArgument("clip", ResourceLocation.class);
        enable(true);
        pose(ANIMATED);
        if (!play(id, priority(priority), speed, mirrored)) {
            context.getSource().sendFailure(Component.literal("No skeletal clip " + id));
            return 0;
        }
        context.getSource().sendSystemMessage(Component.literal("Playing " + id));
        return 1;
    }
}
