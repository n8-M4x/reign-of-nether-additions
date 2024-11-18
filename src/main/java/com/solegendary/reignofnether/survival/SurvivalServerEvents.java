package com.solegendary.reignofnether.survival;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.time.TimeUtils;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SurvivalServerEvents {

    private static boolean isEnabled = false;
    public static Wave nextWave = Wave.getWave(0);
    private static WaveDifficulty difficulty = WaveDifficulty.EASY;
    private static final ArrayList<WaveEnemy> enemies = new ArrayList<>();
    public static final String MONSTER_OWNER_NAME = "Monsters";
    public static final String PIGLIN_OWNER_NAME = "Piglins";
    public static final String VILLAGER_OWNER_NAME = "Illagers";
    public static final List<String> ENEMY_OWNER_NAMES = List.of(MONSTER_OWNER_NAME, PIGLIN_OWNER_NAME, VILLAGER_OWNER_NAME);

    public static final long TICK_INTERVAL = 4;
    private static long lastTime = -1;
    private static long lastEnemyCount = 0;
    private static long ticks = 0;

    private static ServerLevel serverLevel = null;

    public static void saveStage(ServerLevel level) {
        SurvivalSaveData survivalData = SurvivalSaveData.getInstance(level);
        survivalData.isEnabled = isEnabled;
        survivalData.waveNumber = nextWave.number;
        survivalData.difficulty = difficulty;
        survivalData.save();
        level.getDataStorage().save();
        ReignOfNether.LOGGER.info("saved survival data in serverevents");
    }

    @SubscribeEvent
    public static void loadWaveData(ServerStartedEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);
        if (level != null) {
            SurvivalSaveData survivalData = SurvivalSaveData.getInstance(level);
            isEnabled = survivalData.isEnabled;
            nextWave = Wave.getWave(survivalData.waveNumber);
            difficulty = survivalData.difficulty;

            if (isEnabled()) {
                SurvivalClientboundPacket.enableAndSetDifficulty(difficulty);
                SurvivalClientboundPacket.setWaveNumber(nextWave.number);
            }
            ReignOfNether.LOGGER.info("loaded survival data: isEnabled: " + isEnabled());
            ReignOfNether.LOGGER.info("loaded survival data: nextWave: " + nextWave.number);
            ReignOfNether.LOGGER.info("loaded survival data: difficulty: " + difficulty);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || evt.level.isClientSide() || evt.level.dimension() != Level.OVERWORLD)
            return;

        serverLevel = (ServerLevel) evt.level;

        if (!isEnabled())
            return;

        ticks += 1;
        if (ticks % TICK_INTERVAL != 0)
            return;

        long time = evt.level.getDayTime();
        long normTime = TimeUtils.normaliseTime(evt.level.getDayTime());

        if (!isStarted()) {
            setToStartingDayTime();
            return;
        }

        if (lastTime <= TimeUtils.DUSK - 600 && normTime > TimeUtils.DUSK - 600) {
            PlayerServerEvents.sendMessageToAllPlayers("survival.reignofnether.dusksoon", true);
            SoundClientboundPacket.playSoundForAllPlayers(SoundAction.RANDOM_CAVE_AMBIENCE);
        }
        if (lastTime <= TimeUtils.DUSK && normTime > TimeUtils.DUSK) {
            PlayerServerEvents.sendMessageToAllPlayers("survival.reignofnether.dusk", true);
            SoundClientboundPacket.playSoundForAllPlayers(SoundAction.RANDOM_CAVE_AMBIENCE);
            setToStartingNightTime();
        }
        if (lastTime <= TimeUtils.DUSK + getDifficultyTimeModifier() + 100 &&
            normTime > TimeUtils.DUSK + getDifficultyTimeModifier() + 100) {
            startNextWave((ServerLevel) evt.level);
        }
        if (lastTime <= TimeUtils.DAWN && normTime > TimeUtils.DAWN && nextWave.number > 1) {
            PlayerServerEvents.sendMessageToAllPlayers("survival.reignofnether.dawn", true);
            setToStartingDayTime();
        }

        int enemyCount = getCurrentEnemies().size();
        if (enemyCount < lastEnemyCount && enemyCount <= 3) {
            if (enemyCount == 0)
                waveCleared((ServerLevel) evt.level);
            else if (enemyCount == 1) {
                PlayerServerEvents.sendMessageToAllPlayers("survival.reignofnether.remaining_enemies_one");
            } else {
                PlayerServerEvents.sendMessageToAllPlayers("survival.reignofnether.remaining_enemies", false, enemyCount);
            }
        }
        for (WaveEnemy enemy : enemies)
            enemy.tick(TICK_INTERVAL);

        lastTime = normTime;
        lastEnemyCount = enemyCount;
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent evt) {
        evt.getDispatcher().register(Commands.literal("debug-spawn")
                .executes((command) -> {
                    spawnMonsterWave(command.getSource().getLevel());
                    return 1;
                }));
        evt.getDispatcher().register(Commands.literal("debug-end-wave")
                .executes((command) -> {
                    PlayerServerEvents.sendMessageToAllPlayers("Ending current wave");
                    for (WaveEnemy enemy : enemies)
                        enemy.getEntity().kill();
                    return 1;
                }));
        evt.getDispatcher().register(Commands.literal("debug-reset")
                .executes((command) -> {
                    PlayerServerEvents.sendMessageToAllPlayers("Resetting back to wave 1");
                    reset();
                    return 1;
                }));
    }

    public static void enable(WaveDifficulty diff) {
        if (!isEnabled()) {
            reset();
            difficulty = diff;
            isEnabled = true;
            SurvivalClientboundPacket.enableAndSetDifficulty(difficulty);
            if (serverLevel != null)
                saveStage(serverLevel);
        }
    }

    public static void reset() {
        for (WaveEnemy enemy : enemies)
            enemy.getEntity().kill();
        nextWave = Wave.getWave(0);
        difficulty = WaveDifficulty.EASY;
        isEnabled = false;
        if (serverLevel != null)
            saveStage(serverLevel);
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (isEnabled()) {
            SurvivalClientboundPacket.enableAndSetDifficulty(difficulty);
            SurvivalClientboundPacket.setWaveNumber(nextWave.number);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent evt) {
        if (evt.getEntity() instanceof Unit unit &&
                evt.getEntity() instanceof LivingEntity entity &&
                !evt.getLevel().isClientSide &&
                ENEMY_OWNER_NAMES.contains(unit.getOwnerName())) {

            enemies.add(new WaveEnemy(unit));
            // TODO: sync with SurvivalClientEvents
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent evt) {
        if (evt.getEntity() instanceof Unit unit &&
                evt.getEntity() instanceof LivingEntity entity &&
                !evt.getLevel().isClientSide &&
                ENEMY_OWNER_NAMES.contains(unit.getOwnerName())) {

            enemies.removeIf(e -> e.getEntity().getId() == entity.getId());
            // TODO: sync with SurvivalClientEvents
        }
    }

    // standard vanilla length is 20mins for a full day/night cycle (24000)
    // 1min == 1200, but is applied twice per cycle (dawn and dusk), so effectively 1min == 600
    public static long getDifficultyTimeModifier() {
        return switch (difficulty) {
            default -> 3000; // 15mins per day
            case MEDIUM -> 4800; // 12mins per day
            case HARD -> 6600; // 9mins per day
            case EXTREME -> 8400; // 6mins per day
        };
    }

    public static long getDayLength() {
        return 12000 - getDifficultyTimeModifier();
    }

    public static void setToStartingDayTime() {
        serverLevel.setDayTime(TimeUtils.DAWN + getDifficultyTimeModifier());
    }

    public static void setToStartingNightTime() {
        serverLevel.setDayTime(TimeUtils.DUSK + getDifficultyTimeModifier());
    }

    public static boolean isEnabled() { return isEnabled; }

    public static boolean isStarted() {
        for (RTSPlayer player : PlayerServerEvents.rtsPlayers)
            if (BuildingUtils.getTotalCompletedBuildingsOwned(false, player.name) > 0)
                return true;
        return false;
    }

    public static List<WaveEnemy> getCurrentEnemies() {
        return enemies;
    }

    public static boolean isWaveInProgress() {
        return !getCurrentEnemies().isEmpty();
    }

    // triggered at nightfall
    public static void startNextWave(ServerLevel level) {
        spawnMonsterWave(level);
    }

    // triggered when last enemy is killed
    public static void waveCleared(ServerLevel level) {
        nextWave = Wave.getWave(nextWave.number + 1);
        SurvivalClientboundPacket.setWaveNumber(nextWave.number);
        PlayerServerEvents.sendMessageToAllPlayers("survival.reignofnether.wave_cleared", true);
        SoundClientboundPacket.playSoundForAllPlayers(SoundAction.ALLY);
        saveStage(level);
    }

    private static final int MONSTER_MAX_SPAWN_RANGE = 80;
    private static final int MONSTER_MIN_SPAWN_RANGE = 60;

    public static void spawnMonsterWave(ServerLevel level) {
        Random random = new Random();
        List<Building> buildings = BuildingServerEvents.getBuildings();
        int remainingPop = nextWave.population * PlayerServerEvents.rtsPlayers.size();
        if (buildings.isEmpty())
            return;

        do {
            int spawnAttempts = 0;
            BlockState spawnBs;
            BlockPos spawnBp;
            double distSqrToNearestBuilding = 0;

            do {
                Building building = buildings.get(random.nextInt(0, buildings.size()));
                BlockPos centrePos = building.centrePos;

                int x = centrePos.getX() + random.nextInt(-MONSTER_MAX_SPAWN_RANGE, MONSTER_MAX_SPAWN_RANGE);
                int z = centrePos.getZ() + random.nextInt(-MONSTER_MAX_SPAWN_RANGE, MONSTER_MAX_SPAWN_RANGE);
                int y = level.getChunkAt(new BlockPos(x, 0, z)).getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

                spawnBp = MiscUtil.getHighestNonAirBlock(level, new BlockPos(x, y, z), true);
                spawnBs = level.getBlockState(spawnBp);
                spawnAttempts += 1;
                if (spawnAttempts > 30) {
                    ReignOfNether.LOGGER.warn("Gave up trying to find a suitable monster spawn!");
                    return;
                }
                Vec3 vec3 = new Vec3(x,y,z);
                Building b = BuildingUtils.findClosestBuilding(false, vec3, (b1) -> true);
                if (b != null)
                    distSqrToNearestBuilding = b.centrePos.distToCenterSqr(vec3);

            } while (spawnBs.getMaterial() == Material.LEAVES
                    || spawnBs.getMaterial() == Material.WOOD
                    || distSqrToNearestBuilding < (MONSTER_MIN_SPAWN_RANGE) * (MONSTER_MIN_SPAWN_RANGE)
                    || BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), spawnBp)
                    || BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), spawnBp.above()));

            EntityType<? extends Mob> monsterType = nextWave.getRandomUnitOfTier();

            if (spawnBs.getMaterial().isLiquid())
                spawnBp = spawnBp.above();

            ArrayList<Entity> entities = UnitServerEvents.spawnMobs(monsterType, level,
                    monsterType.getDescription().getString().contains("spider") ? spawnBp.above().above(): spawnBp.above(),
                    1, MONSTER_OWNER_NAME);

            for (Entity entity : entities) {
                if (spawnBs.getMaterial().isLiquid()) {

                    level.setBlockAndUpdate(spawnBp, Blocks.FROSTED_ICE.defaultBlockState());

                    List<BlockPos> bps = List.of(spawnBp.north(), spawnBp.east(), spawnBp.south(), spawnBp.west(),
                            spawnBp.north().east(),
                            spawnBp.south().west(),
                            spawnBp.north().east(),
                            spawnBp.south().west());

                    // Frostwalker effect provided in LivingEntityMixin, but it only happens on changing block positions on the ground
                    for (BlockPos bp : bps) {
                        BlockState bs = level.getBlockState(bp);
                        if (bs.getMaterial().isLiquid() ||
                            bs.getMaterial() == Material.WATER_PLANT ||
                            bs.getMaterial() == Material.REPLACEABLE_WATER_PLANT)
                            level.setBlockAndUpdate(bp, Blocks.FROSTED_ICE.defaultBlockState());
                    }
                }
                if (entity instanceof Unit unit)
                    remainingPop -= unit.getPopCost();
            }

        } while (remainingPop > 0);
    }

    public static void spawnIllagerWave() {

    }

    public static void spawnPiglinWave() {

    }
}
