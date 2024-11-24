package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.world.level.GameRules;

public class GameRuleRegistrar {

    public static GameRules.Key<GameRules.BooleanValue> LOG_FALLING;
    public static GameRules.Key<GameRules.BooleanValue> NEUTRAL_AGGRO;
    public static GameRules.Key<GameRules.IntegerValue> MAX_POPULATION;
    public static GameRules.Key<GameRules.BooleanValue> DISALLOW_WAVE_SURVIVAL;

    public static void init() {
        // do cut trees convert their logs into falling logs?
        LOG_FALLING = GameRules.register("doLogFalling", GameRules.Category.MISC,
                GameRules.BooleanValue.create(true)
        );
        // treat neutral units as enemies? this includes auto attacks, right clicks and attack moving
        NEUTRAL_AGGRO = GameRules.register("neutralAggro", GameRules.Category.MISC,
                GameRules.BooleanValue.create(false)
        );
        // set hard cap on population (max even with infinite houses)
        MAX_POPULATION = GameRules.register("maxPopulation", GameRules.Category.MISC,
                GameRules.IntegerValue.create(ResourceCosts.DEFAULT_MAX_POPULATION)
        );
        // prevent clients from joining an RTS match on wave survival mode
        DISALLOW_WAVE_SURVIVAL = GameRules.register("disallowWaveSurvival", GameRules.Category.MISC,
                GameRules.BooleanValue.create(false)
        );
    }
}
