package com.solegendary.reignofnether.survival;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.time.TimeUtils;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SurvivalServerEvents {

    private static boolean isEnabled = false;
    private static Wave nextWave = Wave.getWave(0);
    private static Difficulty difficulty = Difficulty.EASY;
    private static final ArrayList<LivingEntity> enemies = new ArrayList<>();
    private static final int STARTING_EXTRA_SECONDS = 1200; // extra time for all difficulties on wave 1
    private static final String MONSTER_OWNER_NAME = "Monsters";
    private static final String PIGLIN_OWNER_NAME = "Piglins";
    private static final String VILLAGER_OWNER_NAME = "Illagers";
    private static final List<String> ENEMY_OWNER_NAMES = List.of(MONSTER_OWNER_NAME, PIGLIN_OWNER_NAME, VILLAGER_OWNER_NAME);

    private static long lastTime = -1;

    private static ServerLevel serverLevel = null;

    // raise speed of day if
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent evt) {
        if (evt.level.isClientSide() || evt.phase != TickEvent.Phase.END)
            return;

        long time = evt.level.getDayTime();
        long normTime = TimeUtils.normaliseTime(evt.level.getDayTime());

        if (lastTime < 0) {
            lastTime = normTime;
            return;
        }
        if (!isStarted()) {
            setToStartingTime();
            return;
        }

        if (lastTime <= TimeUtils.DAWN && normTime > TimeUtils.DAWN) {
            PlayerServerEvents.sendMessageToAllPlayers(I18n.get("survival.reignofnether.dawn"), true);
            ((ServerLevel) evt.level).setDayTime(time + getDifficultyTimeModifier());
        }
        else if (lastTime <= TimeUtils.DUSK && normTime > TimeUtils.DUSK) {
            PlayerServerEvents.sendMessageToAllPlayers(I18n.get("survival.reignofnether.dusk"), true);
            startNextWave();
            ((ServerLevel) evt.level).setDayTime(time + getDifficultyTimeModifier());
        }

        lastTime = normTime;
        serverLevel = (ServerLevel) evt.level;
    }

    // register here too for command blocks
    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent evt) {
        evt.getDispatcher().register(Commands.literal("debug-end-wave")
                .executes((command) -> {
                    PlayerServerEvents.sendMessageToAllPlayers("Ending current wave");
                    for (LivingEntity entity : enemies)
                        entity.kill();
                    return 1;
                }));
        evt.getDispatcher().register(Commands.literal("debug-reset")
                .executes((command) -> {
                    PlayerServerEvents.sendMessageToAllPlayers("Resetting back to wave 1");
                    resetWaves();
                    return 1;
                }));
        evt.getDispatcher().register(Commands.literal("rts-wave-survival").then(Commands.literal("enable")
                .executes((command) -> {
                    PlayerServerEvents.sendMessageToAllPlayers("Enabled wave survival mode");
                    PlayerServerEvents.sendMessageToAllPlayers("Difficulty: " + difficulty.name());
                    PlayerServerEvents.sendMessageToAllPlayers("Time begins when the first building is placed");
                    setEnabled(true);
                    resetWaves();
                    return 1;
                })));
        evt.getDispatcher().register(Commands.literal("difficulty").then(Commands.literal("easy")
                .executes((command) -> setDifficulty(Difficulty.EASY))));
        evt.getDispatcher().register(Commands.literal("difficulty").then(Commands.literal("medium")
                .executes((command) -> setDifficulty(Difficulty.MEDIUM))));
        evt.getDispatcher().register(Commands.literal("difficulty").then(Commands.literal("hard")
                .executes((command) -> setDifficulty(Difficulty.HARD))));
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent evt) {
        if (evt.getEntity() instanceof Unit unit &&
                evt.getEntity() instanceof LivingEntity entity &&
                !evt.getLevel().isClientSide &&
                ENEMY_OWNER_NAMES.contains(unit.getOwnerName())) {

            enemies.add(entity);
            // TODO: sync with SurvivalClientEvents
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent evt) {
        if (evt.getEntity() instanceof Unit unit &&
                evt.getEntity() instanceof LivingEntity entity &&
                !evt.getLevel().isClientSide &&
                ENEMY_OWNER_NAMES.contains(unit.getOwnerName())) {

            enemies.removeIf(e -> e.getId() == entity.getId());
            // TODO: sync with SurvivalClientEvents
        }
    }

    private enum Difficulty {
        EASY,
        MEDIUM,
        HARD,
        IMPOSSIBLE
    }

    // register here too for command blocks
    public static int setDifficulty(Difficulty diff) {
        if (!isStarted()) {
            setDifficulty(Difficulty.HARD);
            PlayerServerEvents.sendMessageToAllPlayers("Difficulty set to: " + difficulty.name());
        } else {
            PlayerServerEvents.sendMessageToAllPlayers("Too late to change difficulty!");
        }
        return 1;
    }

    public static long getDifficultyTimeModifier() {
        return switch (difficulty) {
            default -> 0; // 10mins each day/night
            case MEDIUM -> 2400; // 8mins each day/night
            case HARD -> 4800; // 6mins each day/night
            case IMPOSSIBLE -> 7200; // 4mins each day/night
        };
    }

    public static void setToStartingTime() {
        serverLevel.setDayTime(TimeUtils.DAWN + getDifficultyTimeModifier());
    }

    public static boolean isEnabled() { return isEnabled; }

    public static void setEnabled(boolean enable) {
        isEnabled = enable;
        // TODO: set max population to 1000
    }

    public static boolean isStarted() {
        return !BuildingServerEvents.getBuildings().isEmpty();
    }

    public static List<LivingEntity> getCurrentEnemies() {
        return enemies;
    }

    public static boolean isWaveInProgress() {
        return !getCurrentEnemies().isEmpty();
    }

    public static void resetWaves() {
        for (LivingEntity entity : enemies)
            entity.kill();
        nextWave = Wave.getWave(0);
    }

    // triggered at nightfall
    public static void startNextWave() {
        spawnMonsterWave();
    }

    // triggered when last enemy is killed
    public static void endCurrentWave() {
        // set time to morning + some time depending on difficulty
        // raise population according to some math + scale to number of players
        // increment waveNumber
        nextWave = Wave.getWave(nextWave.number + 1);
    }

    public static List<LivingEntity> generateNewEnemies() {
        // types are based on current wave (don't allow just 1 of a big enemy early on)
        // capped by nextPopulation
        return null;
    }

    private static final int MONSTER_SPAWN_RANGE = 40;

    public static void spawnMonsterWave() {
        Random random = new Random();
        List<Building> buildings = BuildingServerEvents.getBuildings();
        int remainingPop = nextWave.population;
        if (buildings.isEmpty())
            return;

        do {
            Building building = buildings.get(random.nextInt(0, buildings.size()));
            BlockPos centrePos = building.centrePos;

            int spawnAttempts = 0;
            BlockState spawnBs;
            BlockPos spawnBp;

            do {
                int x = centrePos.getX() + random.nextInt(-MONSTER_SPAWN_RANGE / 2, MONSTER_SPAWN_RANGE / 2);
                int z = centrePos.getZ() + random.nextInt(-MONSTER_SPAWN_RANGE / 2, MONSTER_SPAWN_RANGE / 2);
                int y = serverLevel.getChunkAt(new BlockPos(x, 0, z)).getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                BlockState bs;
                do {
                    bs = serverLevel.getBlockState(new BlockPos(x, y, z));
                    if (!bs.getMaterial().isSolid() && !bs.getMaterial().isLiquid() && y > 0) {
                        y -= 1;
                    } else {
                        break;
                    }
                } while (true);
                spawnBp = new BlockPos(x, y, z);
                spawnBs = serverLevel.getBlockState(spawnBp);
                spawnAttempts += 1;
                if (spawnAttempts > 30) {
                    ReignOfNether.LOGGER.warn("Gave up trying to find a suitable monster spawn!");
                    return;
                }
            } while (!spawnBs.getMaterial().isSolid() || spawnBs.getMaterial() == Material.LEAVES
                    || spawnBs.getMaterial() == Material.WOOD || spawnBp.distSqr(centrePos) < MONSTER_SPAWN_RANGE / 4f
                    || BuildingUtils.isPosInsideAnyBuilding(serverLevel.isClientSide(), spawnBp)
                    || BuildingUtils.isPosInsideAnyBuilding(serverLevel.isClientSide(), spawnBp.above()));

            EntityType<? extends Mob> monsterType = Wave.getRandomUnitOfTier(1);

            ArrayList<Entity> entities = UnitServerEvents.spawnMobs(monsterType, serverLevel, spawnBp.above(), 1, MONSTER_OWNER_NAME);

            for (Entity entity : entities) {
                BotControls.startingCommand(entity, MONSTER_OWNER_NAME);
                if (entity instanceof Unit unit)
                    nextWave.population -= unit.getPopCost();
            }

        } while (nextWave.population > 0);

    }

    public static void spawnIllagerWave() {

    }

    public static void spawnPiglinWave() {

    }
}
