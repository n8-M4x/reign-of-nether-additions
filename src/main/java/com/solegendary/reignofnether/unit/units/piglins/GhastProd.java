package com.solegendary.reignofnether.unit.units.piglins;

import com.solegendary.reignofnether.research.ResearchClient;
import de.n8M4.research.researchItems.ResearchPortalProduction;
import de.n8M4.research.researchItems.ResearchPortalProductionT2;
import de.n8M4.research.researchItems.ResearchPortalProductionT3;
import net.minecraft.client.resources.language.I18n;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.building.ProductionItem;
import com.solegendary.reignofnether.building.buildings.piglins.Fortress;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class GhastProd extends ProductionItem {

    public final static String itemName = "Ghast";
    public final static ResourceCost cost = ResourceCosts.GHAST;

    public GhastProd(ProductionBuilding building) {
        super(building, (int) (cost.ticks * getSpeedMultiplier()));
        this.onComplete = (Level level) -> {
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.GHAST_UNIT.get(), building.ownerName, true);
        };
        this.foodCost = cost.food;
        this.woodCost = cost.wood;
        this.oreCost = cost.ore;
        this.popCost = cost.population;
    }

    public static double getSpeedMultiplier() {// Research Production upgrade
        if(ResearchClient.hasResearch(ResearchPortalProductionT3.itemName)) return 0.5;
        if(ResearchClient.hasResearch(ResearchPortalProductionT2.itemName)) return 0.7;
        if(ResearchClient.hasResearch(ResearchPortalProduction.itemName)) return 0.9;
        return 1;
    }

    public String getItemName() {
        return GhastProd.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding, Keybinding hotkey) {
        List<FormattedCharSequence> tooltipLines = new ArrayList<>(List.of(
                FormattedCharSequence.forward(I18n.get("units.piglins.reignofnether.ghast"), Style.EMPTY.withBold(true)),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPopAndTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("units.piglins.reignofnether.ghast.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("units.piglins.reignofnether.ghast.tooltip2"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("units.piglins.reignofnether.ghast.tooltip3"), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("units.piglins.reignofnether.ghast.tooltip4"), Style.EMPTY)
        ));

        return new Button(
                GhastProd.itemName,
                14,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/ghast.png"),
                hotkey,
                () -> false,
                () -> false,
                () -> BuildingClientEvents.hasFinishedBuilding(Fortress.buildingName),
                () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, itemName),
                null,
                tooltipLines
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
                GhastProd.itemName,
                14,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/ghast.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> BuildingServerboundPacket.cancelProduction(prodBuilding.originPos, itemName, first),
                null,
                null
        );
    }
}
