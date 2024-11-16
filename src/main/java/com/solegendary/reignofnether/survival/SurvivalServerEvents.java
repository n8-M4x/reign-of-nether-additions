package com.solegendary.reignofnether.survival;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.time.TimeUtils;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SurvivalServerEvents {

    private static boolean isEnabled = false;
    private static Wave nextWave = Wave.getWave(0);
    private static WaveDifficulty difficulty = WaveDifficulty.EASY;
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

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || evt.level.isClientSide() || evt.level.dimension() != Level.OVERWORLD)
            return;

        serverLevel = (ServerLevel) evt.level;

        if (!isEnabled())
            return;

        ticks += 1;
        if (ticks % 4 != 0)
            return;

        long time = evt.level.getDayTime();
        long normTime = TimeUtils.normaliseTime(evt.level.getDayTime());

        if (!isStarted()) {
            setToStartingTime();
            return;
        }

        // TODO: add or subtract penalty time based on how fast the wave was cleared
        if (bonusDayTicks > 0 && TimeUtils.isDay(evt.level.getDayTime())) {
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
            startNextWave((ServerLevel) evt.level);
        }
        if (lastTime <= TimeUtils.DAWN && normTime > TimeUtils.DAWN) {
            PlayerServerEvents.sendMessageToAllPlayers(I18n.get("survival.reignofnether.dawn"), true);
        }

        else if (!TimeUtils.isDay(normTime) && isWaveInProgress()) {
            ((ServerLevel) evt.level).setDayTime(TimeUtils.DUSK + 6000);
            ticksToClearLastWave += 4;
        }

        int enemyCount = getCurrentEnemies().size();
        if (enemyCount < lastEnemyCount && enemyCount <= 3) {
            if (enemyCount == 0)
                endCurrentWave((ServerLevel) evt.level);
            else
                PlayerServerEvents.sendMessageToAllPlayers(enemyCount + " enemies remain.");
        }
        lastTime = time;
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
                    for (LivingEntity entity : enemies)
                        entity.kill();
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
        }
    }

    public static void reset() {
        for (LivingEntity entity : enemies)
            entity.kill();
        nextWave = Wave.getWave(0);
        difficulty = WaveDifficulty.EASY;
        isEnabled = false;
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (isEnabled())
            SurvivalClientboundPacket.enableAndSetDifficulty(difficulty);
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

    public static long getDifficultyTimeModifier() {
        return switch (difficulty) {
            default -> 0; // 10mins each day/night
            case MEDIUM -> 2400; // 8mins each day/night
            case HARD -> 4800; // 6mins each day/night
            case EXTREME -> 7200; // 4mins each day/night
        };
    }

    public static long getDayLength() {
        return 12000 - getDifficultyTimeModifier();
    }

    public static void setToStartingTime() {
        serverLevel.setDayTime(TimeUtils.DAWN + getDifficultyTimeModifier());
    }

    public static boolean isEnabled() { return isEnabled; }

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



    // triggered at nightfall
    public static void startNextWave(ServerLevel level) {
        spawnMonsterWave(level);
    }

    // triggered when last enemy is killed
    public static void endCurrentWave(ServerLevel level) {
        nextWave = Wave.getWave(nextWave.number + 1);
        PlayerServerEvents.sendMessageToAllPlayers("Your enemies have been defeated... for now.", false);

        // set bonusDayTicks to pause daytime based on ticksToClearLastWave - up to a maximum of getDayLength()
        // fast-forward day time if the player took too long (to a max of 30s before the next night)

        if (ticksToClearLastWave < getDayLength()) {
            bonusDayTicks = getDayLength() - ticksToClearLastWave;
            level.setDayTime(TimeUtils.DAWN - 200);

            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                if (bonusDayTicks >= 200) {
                    PlayerServerEvents.sendMessageToAllPlayers("Their swift defeat gives you more time to prepare today (+" +
                            TimeUtils.getTimeStrFromTicks(bonusDayTicks) + ")", true);
                }
            });
        } else {
            long penaltyTicks = Math.min(-getDayLength() + 600, getDayLength() - ticksToClearLastWave) ;

            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                if (penaltyTicks >= 200) {
                    PlayerServerEvents.sendMessageToAllPlayers("The prolonged battle means the next night comes sooner (" +
                            TimeUtils.getTimeStrFromTicks(Math.abs(penaltyTicks)) + ")", true);
                }
            });
            level.setDayTime(TimeUtils.DAWN - penaltyTicks);
        }
        ticksToClearLastWave = 0;
    }

    private static final int MONSTER_MAX_SPAWN_RANGE = 120;
    private static final int MONSTER_MIN_SPAWN_RANGE = 90;

    public static void spawnMonsterWave(ServerLevel level) {
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
                int y = level.getChunkAt(new BlockPos(x, 0, z)).getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

                spawnBp =  MiscUtil.getHighestNonAirBlock(level, new BlockPos(x, y, z), true);
                spawnBs = level.getBlockState(spawnBp);
                spawnAttempts += 1;
                if (spawnAttempts > 30) {
                    ReignOfNether.LOGGER.warn("Gave up trying to find a suitable monster spawn!");
                    return;
                }
                Vec3 vec3 = new Vec3(x,y,z);
                Building b = BuildingUtils.findClosestBuilding(false, vec3, (b1) -> true);
                distSqrToNearestBuilding = b.centrePos.distToCenterSqr(vec3);

            } while (spawnBs.getMaterial() == Material.LEAVES
                    || spawnBs.getMaterial() == Material.WOOD
                    || distSqrToNearestBuilding < (MONSTER_MIN_SPAWN_RANGE / 2f) * (MONSTER_MIN_SPAWN_RANGE / 2f)
                    || BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), spawnBp)
                    || BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), spawnBp.above()));

            EntityType<? extends Mob> monsterType = Wave.getRandomUnitOfTier(1);

            if (spawnBs.getMaterial().isLiquid())
                spawnBp = spawnBp.above();

            ArrayList<Entity> entities = UnitServerEvents.spawnMobs(monsterType, level,
                    monsterType.getDescription().getString().contains("spider") ? spawnBp.above().above(): spawnBp.above(),
                    1, MONSTER_OWNER_NAME);

            for (Entity entity : entities) {
                if (spawnBs.getMaterial().isLiquid()) {

                    level.setBlockAndUpdate(spawnBp, Blocks.ICE.defaultBlockState());

                    List<BlockPos> bps = List.of(spawnBp.north(), spawnBp.east(), spawnBp.south(), spawnBp.west(),
                            spawnBp.north().east(),
                            spawnBp.south().west(),
                            spawnBp.north().east(),
                            spawnBp.south().west());

                    // Frostwalker effect provided in LivingEntityMixin, but it only happens on changing block positions on the ground
                    for (BlockPos bp : bps) {
                        BlockState bs = level.getBlockState(bp);
                        if (bs.getMaterial().isLiquid())
                            level.setBlockAndUpdate(bp, Blocks.ICE.defaultBlockState());
                    }
                }
                BotControls.startingCommand(entity, MONSTER_OWNER_NAME);
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
