package com.solegendary.reignofnether.survival;

public class SurvivalClientEvents {

    public static int waveNumber;
    public static boolean isEnabled;
    public static WaveDifficulty difficulty = WaveDifficulty.EASY;

    public static int getMinutesPerDay() {
        return switch (SurvivalClientEvents.difficulty) {
            case EASY -> 10;
            case MEDIUM -> 8;
            case HARD -> 6;
            case EXTREME -> 4;
        };
    }
}
