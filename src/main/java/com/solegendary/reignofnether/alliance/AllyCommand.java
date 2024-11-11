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

import net.minecraft.commands.arguments.EntityArgument;

public class AllyCommand {

    public static final Map<String, String> pendingAlliances = new HashMap<>(); // Tracks pending alliance requests
    public static final Set<UUID> pendingDisbands = new HashSet<>(); // Tracks pending disbands by player UUID

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

    // Initiates an ally request
    private static int ally(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer allyPlayer = EntityArgument.getPlayer(context, "player");

        if (player.equals(allyPlayer)) {
            player.sendSystemMessage(Component.translatable("alliance.reignofnether.ally_self", player.getName().getString()));
            return 0;
        }
        // Record the pending alliance
        pendingAlliances.put(allyPlayer.getName().getString(), player.getName().getString());
        context.getSource().sendSuccess(Component.translatable("alliance.reignofnether.sent_request", allyPlayer.getName().getString()), false);
        allyPlayer.sendSystemMessage(Component.translatable("alliance.reignofnether.ally_confirm", player.getName().getString(), player.getName().getString()));

        return Command.SINGLE_SUCCESS;
    }

    // Confirms an ally request
    private static int allyConfirm(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer requesterPlayer = EntityArgument.getPlayer(context, "player");

        // Check if there is a pending alliance request from the specified player
        if (pendingAlliances.getOrDefault(player.getName().getString(), "").equals(requesterPlayer.getName().getString())) {
            // Create the alliance
            AllianceSystem.addAlliance(player.getName().getString(), requesterPlayer.getName().getString());
            pendingAlliances.remove(player.getName().getString());

            context.getSource().sendSuccess(Component.translatable("alliance.reignofnether.now_allied", requesterPlayer.getName().getString()), false);
            requesterPlayer.sendSystemMessage(Component.translatable("alliance.reignofnether.ally_accepted", player.getName().getString()));
        } else {
            context.getSource().sendFailure(Component.translatable("alliance.reignofnether.no_request", requesterPlayer.getName().getString()));
        }

        return Command.SINGLE_SUCCESS;
    }

    // Initiates disbanding an alliance with a delay
    private static int disband(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer allyPlayer = EntityArgument.getPlayer(context, "player");

        // Schedule disbanding after 30 seconds (600 ticks)
        UUID playerId = player.getUUID();
        pendingDisbands.add(playerId);
        player.getServer().tell(new TickTask(player.getServer().getTickCount() + 600, () -> {
            // Check if still pending
            if (pendingDisbands.remove(playerId)) {
                AllianceSystem.removeAlliance(player.getName().getString(), allyPlayer.getName().getString());
                player.sendSystemMessage(Component.translatable("alliance.reignofnether.disbanded", allyPlayer.getName().getString()));
            }
        }));

        context.getSource().sendSuccess(Component.translatable("alliance.reignofnether.disbanding", allyPlayer.getName().getString()), false);
        return Command.SINGLE_SUCCESS;
    }
}