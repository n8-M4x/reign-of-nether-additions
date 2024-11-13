package com.solegendary.reignofnether.gamemode;

import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.survival.SurvivalClientEvents;
import com.solegendary.reignofnether.survival.WaveDifficulty;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class ClientGameModeHelper {

    public static RTSGameMode gameMode = RTSGameMode.STANDARD;
    public static boolean gameModeLocked = false; // locked with startRTS() in any gamemode, unlocked with /rts-reset

    public static void cycleGameMode() {
        if (gameModeLocked)
            return;
        switch (gameMode) {
            case STANDARD -> gameMode = RTSGameMode.SURVIVAL;
            case SURVIVAL -> gameMode = RTSGameMode.KOTH;
            case KOTH -> gameMode = RTSGameMode.STANDARD;
            default -> gameMode = RTSGameMode.STANDARD;
        }
    }

    public static void cycleWaveDifficulty() {
        switch (SurvivalClientEvents.difficulty) {
            case EASY -> SurvivalClientEvents.difficulty = WaveDifficulty.MEDIUM;
            case MEDIUM -> SurvivalClientEvents.difficulty = WaveDifficulty.HARD;
            case HARD -> SurvivalClientEvents.difficulty = WaveDifficulty.EXTREME;
            case EXTREME -> SurvivalClientEvents.difficulty = WaveDifficulty.EASY;
        }
    }

    private static String getLockedString() {
        return gameModeLocked ? " " + I18n.get("hud.gamemodebuttons.locked") : "";
    }

    // TODO: on startRTS(), send a packet to everyone to set and lock the gamemode

    // all gamemodes are controlled by 1 button, cycled with right-click
    // left click provides functionality specific to the gamemode, eg. changing wave survival difficulty
    public static Button getButton() {
        Button button = switch (gameMode) {
            case STANDARD -> new Button(
                    "Standard",
                    Button.itemIconSize,
                    new ResourceLocation("minecraft", "textures/block/grass_block_side.png"),
                    (Keybinding) null,
                    () -> false,
                    () -> false,
                    () -> !gameModeLocked,
                    () -> {},
                    ClientGameModeHelper::cycleGameMode,
                    List.of(
                            FormattedCharSequence.forward(I18n.get("hud.gamemodebuttons.standard1") +
                                    getLockedString(), Style.EMPTY.withBold(true)),
                            FormattedCharSequence.forward("", Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemodebuttons.standard2"), Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemodebuttons.standard3"), Style.EMPTY),
                            FormattedCharSequence.forward("", Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemodebuttons.changemode"), Style.EMPTY)
                    )
            );
            case SURVIVAL -> new Button(
                    "Survival",
                    Button.itemIconSize,
                    switch (SurvivalClientEvents.difficulty) {
                        case EASY -> new ResourceLocation("minecraft", "textures/item/wooden_sword.png");
                        case MEDIUM -> new ResourceLocation("minecraft", "textures/item/iron_sword.png");
                        case HARD -> new ResourceLocation("minecraft", "textures/item/diamond_sword.png");
                        case EXTREME -> new ResourceLocation("minecraft", "textures/item/netherite_sword.png");
                    },
                    (Keybinding) null,
                    () -> false,
                    () -> false,
                    () -> !gameModeLocked,
                    ClientGameModeHelper::cycleWaveDifficulty, // TODO: send packet to server to set difficulty
                    ClientGameModeHelper::cycleGameMode,
                    List.of(
                            FormattedCharSequence.forward(I18n.get("hud.gamemodebuttons.survival1") +
                                    getLockedString(), Style.EMPTY.withBold(true)),
                            FormattedCharSequence.forward("", Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemodebuttons.survival2"), Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemodebuttons.survival3"), Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemodebuttons.survival4",
                                    SurvivalClientEvents.difficulty, SurvivalClientEvents.getMinutesPerDay()), Style.EMPTY),
                            FormattedCharSequence.forward("", Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemodebuttons.survival5"), Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemodebuttons.changemode"), Style.EMPTY)
                    )
            );
            case KOTH -> new Button(
                    "King of the Beacon",
                    Button.itemIconSize,
                    new ResourceLocation("minecraft", "textures/item/nether_star.png"),
                    (Keybinding) null,
                    () -> false,
                    () -> false,
                    () -> !gameModeLocked,
                    () -> {},
                    ClientGameModeHelper::cycleGameMode,
                    List.of(
                            FormattedCharSequence.forward(I18n.get("hud.gamemodebuttons.koth1") +
                                    getLockedString(), Style.EMPTY.withBold(true)),
                            FormattedCharSequence.forward("", Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemodebuttons.koth2"), Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemodebuttons.koth3"), Style.EMPTY),
                            FormattedCharSequence.forward("", Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemodebuttons.koth4"), Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemodebuttons.changemode"), Style.EMPTY)
                    )
            );
            case SANDBOX -> null;
        };
        if (button != null)
            button.tooltipOffsetY = 45;
        return button;
    }
}
