package de.n8M4.building.buildings.monsters;

import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingBlockData;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.building.buildings.monsters.Graveyard;
import com.solegendary.reignofnether.building.buildings.monsters.Mausoleum;
import com.solegendary.reignofnether.building.buildings.villagers.Barracks;
import com.solegendary.reignofnether.building.buildings.villagers.TownCentre;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class MonsterMine extends ProductionBuilding {

    public final static String buildingName = "Monster Ore Mine";
    public final static String structureName = "mine_monsters";
    public final static ResourceCost cost = ResourceCosts.MINE_MONSTERS;

    private static final int ICE_CHECK_TICKS_MAX = 100;
    private int ticksToNextIceCheck = ICE_CHECK_TICKS_MAX;


    public MonsterMine(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation), false);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.portraitBlock = Blocks.POLISHED_BLACKSTONE_BRICK_WALL;
        this.icon = new ResourceLocation("minecraft", "textures/item/minecart.png");
        this.foodCost = cost.food;
        this.woodCost = cost.wood;
        this.oreCost = cost.ore;
        this.popSupply = cost.population;
        this.startingBlockTypes.add(Blocks.BLACKSTONE);
        this.explodeChance = 0;

        BASE_MS_PER_BUILD = 150;
    }

    public Faction getFaction() {return Faction.MONSTERS;}

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
                MonsterMine.buildingName,
                new ResourceLocation("minecraft", "textures/item/minecart.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == MonsterMine.class,
                () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.EXPLAIN_BUILDINGS),
                () -> BuildingClientEvents.hasFinishedBuilding(Graveyard.buildingName) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                () -> BuildingClientEvents.setBuildingToPlace(MonsterMine.class),
                null,
                List.of(
                        FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.mine"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.mine.tooltip2"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.mine.tooltip3"), Style.EMPTY)
                ),
                null

        );
    }
}