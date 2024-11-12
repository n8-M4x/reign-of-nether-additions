package com.solegendary.reignofnether.survival;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.time.TimeUtils;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;
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
    public static final String MONSTER_OWNER_NAME = "Monsters";
    public static final String PIGLIN_OWNER_NAME = "Piglins";
    public static final String VILLAGER_OWNER_NAME = "Illagers";
    public static final List<String> ENEMY_OWNER_NAMES = List.of(MONSTER_OWNER_NAME, PIGLIN_OWNER_NAME, VILLAGER_OWNER_NAME);

    private static long lastTime = -1;
    private static long lastEnemyCount = 0;
    private static long ticks = 0;
    private static long ticksToClearLastWave = 0;
    private static long bonusDayTicks = 0;

    private static ServerLevel serverLevel = null;

    // raise speed of day if
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent evt) {
        if (evt.level.isClientSide() || evt.phase != TickEvent.Phase.END || !isEnabled())
            return;
        ticks += 1;
        if (ticks % 4 != 0)
            return;

        serverLevel = (ServerLevel) evt.level;

        long time = evt.level.getDayTime();
        long normTime = TimeUtils.normaliseTime(evt.level.getDayTime());

        if (!isStarted()) {
            setToStartingTime();
            return;
        }

        // TODO: add or subtract penalty time based on how fast the wave was cleared
        if (bonusDayTicks > 0) {
            ((ServerLevel) evt.level).setDayTime(TimeUtils.DAWN + 100);
            bonusDayTicks -= 4;
        }

        if (lastTime <= TimeUtils.DUSK - 600 && normTime > TimeUtils.DUSK - 600) {
            PlayerServerEvents.sendMessageToAllPlayers(I18n.get("survival.reignofnether.dusksoon"), true);
            SoundClientboundPacket.playSoundOnClient(SoundAction.USE_PORTAL);
        }
        if (lastTime <= TimeUtils.DUSK && normTime > TimeUtils.DUSK) {
            PlayerServerEvents.sendMessageToAllPlayers(I18n.get("survival.reignofnether.dusk"), true);
            SoundClientboundPacket.playSoundOnClient(SoundAction.RANDOM_CAVE_AMBIENCE);
        }
        if (lastTime <= TimeUtils.DUSK + 100 && normTime > TimeUtils.DUSK + 100) {
            startNextWave();
        }
        else if (!TimeUtils.isDay(normTime) && isWaveInProgress()) {
            ((ServerLevel) evt.level).setDayTime(TimeUtils.DUSK + 6000);
            ticksToClearLastWave += 4;
        }

        int enemyCount = getCurrentEnemies().size();
        if (enemyCount < lastEnemyCount && enemyCount <= 3) {
            if (enemyCount == 0)
                PlayerServerEvents.sendMessageToAllPlayers(enemyCount + " enemies remain.");
            else
                endCurrentWave();
        }
        lastEnemyCount = enemyCount;
    }

    // TODO: change these to GUI buttons
    /*
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
        evt.getDispatcher().register(Commands.literal("rts-waves").then(Commands.literal("start")
                .executes((command) -> {
                    if (isStarted()) {
                        PlayerServerEvents.sendMessageToAllPlayers("Too late to start wave survival. Use /rts-reset and try again.");
                    } else {
                        PlayerServerEvents.sendMessageToAllPlayers("Enabled wave survival mode");
                        PlayerServerEvents.sendMessageToAllPlayers("Difficulty: " + difficulty.name() + " (Change with /rts-difficulty)");
                        PlayerServerEvents.sendMessageToAllPlayers("Time begins when the first building is placed");
                        setEnabled(true);
                        resetWaves();
                    }
                    return 1;
                })));
        evt.getDispatcher().register(Commands.literal("rts-difficulty").then(Commands.literal("easy")
                .executes((command) -> setDifficulty(Difficulty.EASY))));
        evt.getDispatcher().register(Commands.literal("rts-difficulty").then(Commands.literal("medium")
                .executes((command) -> setDifficulty(Difficulty.MEDIUM))));
        evt.getDispatcher().register(Commands.literal("rts-difficulty").then(Commands.literal("hard")
                .executes((command) -> setDifficulty(Difficulty.HARD))));
        evt.getDispatcher().register(Commands.literal("rts-difficulty").then(Commands.literal("extreme")
                .executes((command) -> setDifficulty(Difficulty.EXTREME))));
    }
     */

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
        EXTREME
    }

    // register here too for command blocks
    public static int setDifficulty(Difficulty diff) {
        if (!isStarted() && isEnabled()) {
            difficulty = diff;
            PlayerServerEvents.sendMessageToAllPlayers("Difficulty set to: " + difficulty.name());
            PlayerServerEvents.sendMessageToAllPlayers("Day length: " + (10 - getDifficultyTimeModifier()/60) + " mins");
        } else if (!isEnabled()) {
            PlayerServerEvents.sendMessageToAllPlayers("You are not playing Wave Survival.");
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
            case EXTREME -> 7200; // 4mins each day/night
        };
    }

    public static long getDayLength() {
        return 24000 - getDifficultyTimeModifier();
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
        return !BuildingServerEvents.getBuildings().isEmpty() &&
                !PlayerServerEvents.rtsPlayers.isEmpty();
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
        nextWave = Wave.getWave(nextWave.number + 1);
        PlayerServerEvents.sendMessageToAllPlayers("Your enemies have been defeated... for now.", true);

        // set bonusDayTicks to pause daytime based on ticksToClearLastWave - up to a maximum of getDayLength()
        // fast-forward day time if the player took too long (to a max of 35s before the next night)
        long ticksToClearInv = Math.max(-getDayLength()/2 + 700, 24000 - ticksToClearLastWave);

        if (ticksToClearInv > 0) {
            bonusDayTicks = ticksToClearInv;
            PlayerServerEvents.sendMessageToAllPlayers(I18n.get("survival.reignofnether.dawn"), true);
            PlayerServerEvents.sendMessageToAllPlayers("Their swift defeat gives you more time to prepare (+" +
                    TimeUtils.getTimeStrFromTicks(ticksToClearInv) + ")", true);
            serverLevel.setDayTime(TimeUtils.DAWN + 10);

        } else {
            PlayerServerEvents.sendMessageToAllPlayers("The prolonged battle means the next night comes sooner (-" +
                    TimeUtils.getTimeStrFromTicks(Math.abs(ticksToClearInv)) + ")", true);
            serverLevel.setDayTime(TimeUtils.DAWN - ticksToClearInv);
        }
        ticksToClearLastWave = 0;
    }

    private static final int MONSTER_MAX_SPAWN_RANGE = 60;
    private static final int MONSTER_MIN_SPAWN_RANGE = 40;

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
            double distSqrToNearestBuilding = 0;

            do {
                int x = centrePos.getX() + random.nextInt(-MONSTER_MAX_SPAWN_RANGE / 2, MONSTER_MAX_SPAWN_RANGE / 2);
                int z = centrePos.getZ() + random.nextInt(-MONSTER_MAX_SPAWN_RANGE / 2, MONSTER_MAX_SPAWN_RANGE / 2);
                int y = serverLevel.getChunkAt(new BlockPos(x, 0, z)).getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

                spawnBp =  MiscUtil.getHighestNonAirBlock(serverLevel, new BlockPos(x, y, z), true);
                spawnBs = serverLevel.getBlockState(spawnBp);
                spawnAttempts += 1;
                if (spawnAttempts > 30) {
                    ReignOfNether.LOGGER.warn("Gave up trying to find a suitable monster spawn!");
                    return;
                }
                Vec3 vec3 = new Vec3(x,y,z);
                Building b = BuildingUtils.findClosestBuilding(false, vec3, (b1) -> true);
                distSqrToNearestBuilding = b.centrePos.distToCenterSqr(vec3);

            } while (!spawnBs.getMaterial().isSolid() || spawnBs.getMaterial() == Material.LEAVES
                    || spawnBs.getMaterial() == Material.WOOD || distSqrToNearestBuilding < (MONSTER_MIN_SPAWN_RANGE / 2f) * (MONSTER_MIN_SPAWN_RANGE / 2f)
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
