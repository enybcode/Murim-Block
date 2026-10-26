package com.murimblock.anim.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.murimblock.Murimblock;
import com.murimblock.anim.BipedPoses;
import com.murimblock.anim.Pose;
import java.util.stream.Stream;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Which players the skeletal engine draws, and in what pose. Until combat mode exists it is switched by hand with
 * the client command {@code /skeletal on|off}, {@code /skeletal pose <name>} and {@code /skeletal joints on|off} (the
 * debug skeleton view); dev only, not in the lang file.
 * Vanilla keeps the players it handles better for now: sleeping, gliding, swimming, riding, spectating.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SkeletalPlayers {
    private static boolean enabled;
    private static boolean showJoints;
    /** "stand" (vanilla's standing pose) or one of {@link BipedPoses#TEST}. */
    private static String poseName = "stand";

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

    /** The pose to draw {@code player} in, helpers settled; a fresh copy the caller may change. */
    public static Pose pose(AbstractClientPlayer player, float partialTick) {
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
                        }))));
    }
}
