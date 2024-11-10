package com.solegendary.reignofnether.survival;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.ZombiePiglinUnit;
import com.solegendary.reignofnether.unit.units.monsters.ZombieUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import java.util.Comparator;
import java.util.List;

// class for performing UnitActions like moving and attacking
// only used serverside
public class BotControls {

    // done on spawn
    public static void startingCommand(Entity entity, String ownerName) {
        if (entity instanceof AttackerUnit unit) {
            ((Unit) unit).resetBehaviours();

            if (entity instanceof ZombieUnit ||
                entity instanceof ZombiePiglinUnit)
                attackMoveNearestBuilding(unit, ownerName);
        }
    }

    // done every few ticks
    public static void reactionCommand(Entity entity, String ownerName) {

    }

    private static void attackMoveNearestBuilding(AttackerUnit unit, String ownerName) {
        Entity entity = (Entity) unit;
        List<Building> buildings = BuildingServerEvents.getBuildings().stream()
                    .sorted(Comparator.comparing(b -> b.centrePos.distToCenterSqr(entity.getEyePosition())))
                    .toList();

        BlockPos targetBp = null;
        if (!buildings.isEmpty())
            targetBp = buildings.get(0).centrePos;

        if (targetBp != null)
            UnitServerEvents.addActionItem(ownerName, UnitAction.ATTACK_MOVE, -1,
                    new int[]{entity.getId()},  targetBp, new BlockPos(0,0,0));
    }

    private static void attackMoveNearestUnit(AttackerUnit unit, String ownerName) {

    }

    private static void attackNearestWorker(AttackerUnit unit, String ownerName) {

    }
}
