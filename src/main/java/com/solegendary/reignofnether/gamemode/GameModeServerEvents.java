package com.solegendary.reignofnether.gamemode;

import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.solegendary.reignofnether.player.PlayerClientboundPacket;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.survival.SurvivalClientboundPacket;
import com.solegendary.reignofnether.survival.SurvivalServerEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Map;

public class GameModeServerEvents {

    private static GameMode getGameMode() {
        // if a beacon exists, return ClientGameMode.KOTH;
        if (SurvivalServerEvents.isEnabled())
            return GameMode.SURVIVAL;

        return GameMode.STANDARD;
    }

    private static boolean isGameModeLocked() {
        return !PlayerServerEvents.rtsPlayers.isEmpty();
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (isGameModeLocked())
            GameModeClientboundPacket.setAndLockAllClientGameModes(getGameMode());
        if (evt.getEntity().getLevel().getGameRules().getRule(GameRuleRegistrar.DISALLOW_WAVE_SURVIVAL).get())
            GameModeClientboundPacket.disallowSurvival();
    }

    @SubscribeEvent
    public static void onCommandUsed(CommandEvent evt) {
        List<ParsedCommandNode<CommandSourceStack>> nodes = evt.getParseResults().getContext().getNodes();
        if (nodes.size() >= 3 &&
                nodes.get(0).getNode().getName().equals("gamerule") &&
                nodes.get(1).getNode().getName().equals("disallowWaveSurvival")) {

            Map<String, ParsedArgument<CommandSourceStack, ?>> args = evt.getParseResults().getContext().getArguments();
            if (args.containsKey("value")) {
                boolean value = (boolean) args.get("value").getResult();
                if (value)
                    GameModeClientboundPacket.disallowSurvival();
                else
                    GameModeClientboundPacket.allowSurvival();
            }
        }
    }
}
