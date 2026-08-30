package com.murimblock.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.murimblock.cultivation.BreakthroughType;
import com.murimblock.cultivation.CultivationData;
import com.murimblock.cultivation.CultivationProgression;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.qi.QiService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class CultivationCommands {
    private static final int ADMIN_PERMISSION_LEVEL = 2;
    private static final DynamicCommandExceptionType UNKNOWN_REALM =
            new DynamicCommandExceptionType(name -> Component.literal("Unknown cultivation realm: " + name));
    private static final DynamicCommandExceptionType UNKNOWN_STAGE =
            new DynamicCommandExceptionType(name -> Component.literal("Unknown cultivation stage: " + name));

    private CultivationCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cultivation")
                .then(Commands.literal("check")
                        .executes(context -> check(context, context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> check(context, EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(ADMIN_PERMISSION_LEVEL))
                        .then(realmArgument()
                                .then(stageArgument()
                                        .executes(context -> set(
                                                context,
                                                context.getSource().getPlayerOrException(),
                                                getRealm(context),
                                                getStage(context)
                                        ))))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(realmArgument()
                                        .then(stageArgument()
                                                .executes(context -> set(
                                                        context,
                                                        EntityArgument.getPlayer(context, "player"),
                                                        getRealm(context),
                                                        getStage(context)
                                                ))))))
                .then(Commands.literal("advance")
                        .requires(source -> source.hasPermission(ADMIN_PERMISSION_LEVEL))
                        .executes(context -> advance(context, context.getSource().getPlayerOrException(), false))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> advance(context, EntityArgument.getPlayer(context, "player"), false))))
                .then(Commands.literal("forceadvance")
                        .requires(source -> source.hasPermission(ADMIN_PERMISSION_LEVEL))
                        .executes(context -> advance(context, context.getSource().getPlayerOrException(), true))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> advance(context, EntityArgument.getPlayer(context, "player"), true))))
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(ADMIN_PERMISSION_LEVEL))
                        .executes(context -> reset(context, context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> reset(context, EntityArgument.getPlayer(context, "player"))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> realmArgument() {
        return Commands.argument("realm", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        java.util.Arrays.stream(CultivationRealm.values())
                                .map(CultivationRealm::serializedName),
                        builder
                ));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> stageArgument() {
        return Commands.argument("stage", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        java.util.Arrays.stream(CultivationStage.values())
                                .map(CultivationStage::serializedName),
                        builder
                ));
    }

    private static int check(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        CultivationData current = CultivationService.getCultivation(target);
        Optional<CultivationData> next = CultivationProgression.getNext(current);
        Optional<Double> required = CultivationProgression.getRequiredQiMaxForNext(current);
        double qiMax = QiService.getQiMax(target);
        boolean canAttempt = CultivationProgression.canAttemptBreakthrough(current, qiMax);

        context.getSource().sendSuccess(() -> Component.literal("Cultivation : " + current.displayName()), false);
        context.getSource().sendSuccess(() -> Component.literal("Qi Max : " + format(qiMax)), false);
        context.getSource().sendSuccess(
                () -> Component.literal("Prochain stade : " + next.map(CultivationData::displayName).orElse("Aucun")),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("Qi Max requis : " + required.map(CultivationCommands::format).orElse("Aucun")),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("Breakthrough : " + (canAttempt ? "disponible" : "indisponible")),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("Type : " + CultivationProgression.getBreakthroughType(current)
                        .map(CultivationCommands::format)
                        .orElse("Aucun")),
                false
        );
        return 1;
    }

    private static int set(
            CommandContext<CommandSourceStack> context,
            ServerPlayer target,
            CultivationRealm realm,
            CultivationStage stage
    ) {
        CultivationService.setCultivation(target, realm, stage);
        context.getSource().sendSuccess(
                () -> Component.literal("Cultivation definie : " + new CultivationData(realm, stage).displayName()),
                true
        );
        return 1;
    }

    private static int advance(CommandContext<CommandSourceStack> context, ServerPlayer target, boolean force) {
        CultivationData current = CultivationService.getCultivation(target);
        Optional<CultivationData> next = CultivationProgression.getNext(current);
        if (next.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Breakthrough impossible. Dernier stade atteint."));
            return 0;
        }

        Optional<Double> required = CultivationProgression.getRequiredQiMaxForNext(current);
        double qiMax = QiService.getQiMax(target);
        if (!force && !CultivationProgression.canAttemptBreakthrough(current, qiMax)) {
            context.getSource().sendFailure(Component.literal("Breakthrough impossible. Qi Max insuffisant."));
            context.getSource().sendFailure(Component.literal("Qi Max : " + format(qiMax)
                    + " / Requis : " + required.map(CultivationCommands::format).orElse("Aucun")));
            return 0;
        }

        boolean advanced = force
                ? CultivationService.forceAdvance(target)
                : CultivationService.advanceAfterSuccessfulBreakthrough(target);
        if (!advanced) {
            context.getSource().sendFailure(Component.literal("Breakthrough impossible."));
            return 0;
        }

        context.getSource().sendSuccess(
                () -> Component.literal(current.displayName() + " -> " + next.get().displayName()),
                true
        );
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        CultivationService.resetCultivation(target);
        context.getSource().sendSuccess(
                () -> Component.literal("Cultivation reinitialisee : " + CultivationData.initial().displayName()),
                true
        );
        return 1;
    }

    private static CultivationRealm getRealm(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "realm");
        return CultivationRealm.bySerializedName(name)
                .orElseThrow(() -> UNKNOWN_REALM.create(name));
    }

    private static CultivationStage getStage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "stage");
        return CultivationStage.bySerializedName(name)
                .orElseThrow(() -> UNKNOWN_STAGE.create(name));
    }

    private static String format(BreakthroughType type) {
        return type == BreakthroughType.MAJOR ? "Major" : "Minor";
    }

    private static String format(double value) {
        return BigDecimal.valueOf(value)
                .setScale(3, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
