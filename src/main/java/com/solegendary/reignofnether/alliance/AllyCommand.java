package com.solegendary.reignofnether.alliance;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.minecraft.commands.arguments.EntityArgument;

public class AllyCommand {

    public static final Map<String, String> pendingAlliances = new HashMap<>();
    public static final Set<UUID> pendingDisbands = new HashSet<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ally")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(AllyCommand::ally)));

        dispatcher.register(Commands.literal("allyconfirm")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(AllyCommand::allyConfirm)));

        dispatcher.register(Commands.literal("disband")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(AllyCommand::disband)));
    }

    private static int ally(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer allyPlayer = EntityArgument.getPlayer(context, "player");

        if (player.equals(allyPlayer)) {
            player.sendSystemMessage(Component.translatable("alliance.reignofnether.ally_self", player.getName().getString()));
            return 0;
        }
        pendingAlliances.put(allyPlayer.getName().getString(), player.getName().getString());
        context.getSource().sendSuccess(Component.translatable("alliance.reignofnether.sent_request", allyPlayer.getName().getString()), false);
        allyPlayer.sendSystemMessage(Component.translatable("alliance.reignofnether.ally_confirm", player.getName().getString(), player.getName().getString()));

        return Command.SINGLE_SUCCESS;
    }

    private static int allyConfirm(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer requesterPlayer = EntityArgument.getPlayer(context, "player");

        if (pendingAlliances.getOrDefault(player.getName().getString(), "").equals(requesterPlayer.getName().getString())) {
            AllianceSystem.addAlliance(player.getName().getString(), requesterPlayer.getName().getString());
            pendingAlliances.remove(player.getName().getString());

            context.getSource().sendSuccess(Component.translatable("alliance.reignofnether.now_allied", requesterPlayer.getName().getString()), false);
            requesterPlayer.sendSystemMessage(Component.translatable("alliance.reignofnether.ally_accepted", player.getName().getString()));
        } else {
            context.getSource().sendFailure(Component.translatable("alliance.reignofnether.no_request", requesterPlayer.getName().getString()));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int disband(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer allyPlayer = EntityArgument.getPlayer(context, "player");

        if (player.equals(allyPlayer)) {
            context.getSource().sendFailure(Component.translatable("alliance.reignofnether.disband_self"));
            return 0;
        }

        UUID playerId = player.getUUID();
        if (pendingDisbands.contains(playerId)) {
            context.getSource().sendFailure(Component.translatable("alliance.reignofnether.disband_pending", allyPlayer.getName().getString()));
            return 0;
        }

        pendingDisbands.add(playerId);
        scheduler.schedule(() -> {
            if (pendingDisbands.remove(playerId)) {
                AllianceSystem.removeAlliance(player.getName().getString(), allyPlayer.getName().getString());
                player.sendSystemMessage(Component.translatable("alliance.reignofnether.disbanded", allyPlayer.getName().getString()));
                allyPlayer.sendSystemMessage(Component.translatable("alliance.reignofnether.disbanded", player.getName().getString()));
            }
        }, 30, TimeUnit.SECONDS);

        context.getSource().sendSuccess(Component.translatable("alliance.reignofnether.disbanding", allyPlayer.getName().getString()), false);
        return Command.SINGLE_SUCCESS;
    }
}
