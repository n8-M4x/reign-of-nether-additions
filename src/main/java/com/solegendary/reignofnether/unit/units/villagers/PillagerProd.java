package com.solegendary.reignofnether.unit.units.villagers;

import de.n8M4.research.researchItems.ResearchBarracksProduction;
import de.n8M4.research.researchItems.ResearchBarracksProductionT2;
import de.n8M4.research.researchItems.ResearchBarracksProductionT3;
import net.minecraft.client.resources.language.I18n;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.building.ProductionItem;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.researchItems.ResearchPillagerCrossbows;
import com.solegendary.reignofnether.research.researchItems.ResearchVindicatorAxes;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class PillagerProd extends ProductionItem {

    public final static String itemName = "Pillager";
    public final static ResourceCost cost = ResourceCosts.PILLAGER;

    public PillagerProd(ProductionBuilding building) {
        super(building,  (int) (cost.ticks * getSpeedMultiplier()));
        this.onComplete = (Level level) -> {
            if (!level.isClientSide())
                building.produceUnit((ServerLevel) level, EntityRegistrar.PILLAGER_UNIT.get(), building.ownerName, true);
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
        return PillagerProd.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding, Keybinding hotkey) {
        List<FormattedCharSequence> tooltipLines = new ArrayList<>(List.of(
            FormattedCharSequence.forward(I18n.get("units.villagers.reignofnether.pillager"), Style.EMPTY.withBold(true)),
            ResourceCosts.getFormattedCost(cost),
            ResourceCosts.getFormattedPopAndTime(cost),
            FormattedCharSequence.forward("", Style.EMPTY),
            FormattedCharSequence.forward(I18n.get("units.villagers.reignofnether.pillager.tooltip1"), Style.EMPTY)
        ));
        return new Button(
            PillagerProd.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/pillager.png"),
            hotkey,
            () -> false,
            () -> false,
            () -> true,
            () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, itemName),
            null,
            tooltipLines
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(
            PillagerProd.itemName,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/pillager.png"),
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
