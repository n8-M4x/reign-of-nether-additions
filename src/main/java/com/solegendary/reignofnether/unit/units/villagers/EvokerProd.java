package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.research.ResearchClient;
import de.n8M4.research.researchItems.ResearchBarracksProduction;
import de.n8M4.research.researchItems.ResearchBarracksProductionT2;
import de.n8M4.research.researchItems.ResearchBarracksProductionT3;
import net.minecraft.client.resources.language.I18n;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.villagers.Library;
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

import java.util.List;

public class EvokerProd extends ProductionItem {

    public final static String itemName = "Evoker";
    public final static ResourceCost cost = ResourceCosts.EVOKER;

    public EvokerProd(ProductionBuilding building) {
        super(building, (int) (cost.ticks * getSpeedMultiplier()));
        this.onComplete = (Level level) -> {
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.EVOKER_UNIT.get(), building.ownerName, true);
        };
        this.foodCost = cost.food;
        this.woodCost = cost.wood;
        this.oreCost = cost.ore;
        this.popCost = cost.population;
    }

    public static double getSpeedMultiplier() {// Research Production upgrade
        if(ResearchClient.hasResearch(ResearchBarracksProductionT3.itemName)) return 0.5;
        if(ResearchClient.hasResearch(ResearchBarracksProductionT2.itemName)) return 0.7;
        if(ResearchClient.hasResearch(ResearchBarracksProduction.itemName)) return 0.9;
        return 1;
    }

    public String getItemName() {
        return EvokerProd.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding, Keybinding hotkey) {
        return new Button(
            EvokerProd.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/evoker.png"),
            hotkey,
            () -> false,
            () -> false,
            () -> BuildingClientEvents.hasFinishedBuilding(Library.buildingName),
            () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, itemName),
            null,
            List.of(
                FormattedCharSequence.forward(I18n.get("units.villagers.reignofnether.evoker"), Style.EMPTY.withBold(true)),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPopAndTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("units.villagers.reignofnether.evoker.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("units.villagers.reignofnether.evoker.tooltip2"), Style.EMPTY)
            )
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
            EvokerProd.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/evoker.png"),
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
