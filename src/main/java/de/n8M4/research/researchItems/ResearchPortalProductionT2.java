package de.n8M4.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.building.ProductionItem;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class ResearchPortalProductionT2 extends ProductionItem {

    public final static String itemName = "Prod Time Portal Tier II";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_PORTAL_PROD_TIER_II;


    public ResearchPortalProductionT2(ProductionBuilding building) {
        super(building, cost.ticks);
        this.onComplete = (Level level) -> {
            if (level.isClientSide()) {
                ResearchClient.addResearch(this.building.ownerName, ResearchPortalProductionT2.itemName);
            } else {
                ResearchServerEvents.addResearch(this.building.ownerName, ResearchPortalProductionT2.itemName);
            }
        };
        this.foodCost = cost.food;
        this.woodCost = cost.wood;
        this.oreCost = cost.ore;
    }

    public String getItemName() {
        return ResearchPortalProductionT2.itemName;
    }

    public static Button getStartButton(ProductionBuilding prodBuilding, Keybinding hotkey) {
        return new Button(ResearchPortalProductionT2.itemName,
                14,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/clock.png"),
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                hotkey,
                () -> false,
                () -> ProductionItem.itemIsBeingProduced(ResearchPortalProductionT2.itemName, prodBuilding.ownerName)
                        || ResearchClient.hasResearch(ResearchPortalProductionT2.itemName) || !ResearchClient.hasResearch(ResearchPortalProduction.itemName),
                () -> true,
                () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, itemName),
                null,
                List.of(FormattedCharSequence.forward(I18n.get("research.reignofnether.prod_tier_ii"),
                                Style.EMPTY.withBold(true)
                        ),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedTime(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("research.reignofnether.prod_tier_ii.tooltip1"), Style.EMPTY)
                )
        );
    }

    public Button getCancelButton(ProductionBuilding prodBuilding, boolean first) {
        return new Button(ResearchPortalProductionT2.itemName,
                14,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/clock.png"),
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
                null,
                () -> false,
                () -> false,
                () -> true,
                () -> BuildingServerboundPacket.cancelProduction(prodBuilding.minCorner, itemName, first),
                null,
                null
        );
    }

}
