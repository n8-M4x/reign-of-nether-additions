package com.solegendary.reignofnether.survival;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.MeleeAttackBuildingGoal;
import com.solegendary.reignofnether.unit.goals.MeleeAttackUnitGoal;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WaveEnemy {

    private static final int PERIODIC_COMMAND_INTERVAL = 100;
    private static final int IDLE_COMMAND_INTERVAL = 100;

    public final Unit unit;
    private long idleTicks = 0;
    private long ticks = 0;

    private BlockPos lastOnPos;

    public WaveEnemy(Unit unit) {
        this.unit = unit;
        this.lastOnPos = getEntity().getOnPos();
    }

    public LivingEntity getEntity() {
        return ((LivingEntity) unit);
    }

    public void tick(long ticksToAdd) {
        ticks += ticksToAdd;

        boolean isAttacking = unit.getTargetGoal().getTarget() != null;
        if (!isAttacking &&
            unit instanceof AttackerUnit aUnit &&
            aUnit.getAttackBuildingGoal() instanceof MeleeAttackBuildingGoal mabg &&
            mabg.isAttacking())
            isAttacking = true;

        BlockPos onPos = getEntity().getOnPos();
        if (onPos.equals(lastOnPos) && !isAttacking)
            idleTicks += ticksToAdd;
        else
            idleTicks = 0;

        lastOnPos = onPos;

        if (ticks == ticksToAdd * 10)
            startingCommand();

        if (ticks % PERIODIC_COMMAND_INTERVAL == 0)
            periodicCommand();

        if (idleTicks % IDLE_COMMAND_INTERVAL == 0)
            idleCommand();
    }

    // done shortly after spawn
    public void startingCommand() {
        attackMoveNearestBuilding();
    }

    // done every X ticks
    public void periodicCommand() {
        attackMoveNearestBuilding();
    }

    // done if the unit didn't change position in X ticks
    public void idleCommand() {
        attackMoveNearestBuilding();
    }

    // done when attacked
    public void retaliateCommand() {

    }

    private void attackMoveNearestBuilding() {
        unit.resetBehaviours();

        Entity entity = (Entity) unit;
        List<Building> buildings = BuildingServerEvents.getBuildings().stream()
                .sorted(Comparator.comparing(b -> b.centrePos.distToCenterSqr(entity.getEyePosition())))
                .toList();

        BlockPos targetBp = null;
        if (!buildings.isEmpty())
            targetBp = buildings.get(0).centrePos;

        if (targetBp != null)
            UnitServerEvents.addActionItem(unit.getOwnerName(), UnitAction.ATTACK_MOVE, -1,
                    new int[]{entity.getId()},  targetBp, new BlockPos(0,0,0));
    }

    private void attackMoveRandomBuilding() {
        unit.resetBehaviours();

        ArrayList<Building> buildings = BuildingServerEvents.getBuildings();
        Collections.shuffle(buildings);

        BlockPos targetBp = null;
        if (!buildings.isEmpty())
            targetBp = buildings.get(0).centrePos;

        if (targetBp != null)
            UnitServerEvents.addActionItem(unit.getOwnerName(), UnitAction.ATTACK_MOVE, -1,
                    new int[]{((Entity) unit).getId()},  targetBp, new BlockPos(0,0,0));
    }

    private void attackMoveNearestUnit(AttackerUnit unit, String ownerName) {

    }

    private void attackNearestWorker(AttackerUnit unit, String ownerName) {

    }
}
