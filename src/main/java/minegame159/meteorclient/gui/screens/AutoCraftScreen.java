package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.gui.PanelListScreen;
import minegame159.meteorclient.gui.widgets.WCheckbox;
import minegame159.meteorclient.gui.widgets.WGrid;
import minegame159.meteorclient.gui.widgets.WLabel;

public class AutoCraftScreen extends PanelListScreen {
    public AutoCraftScreen() {
        super("Auto Craft");
        parent = mc.currentScreen;

        WGrid grid = add(new WGrid(4, 4, 2));

        // Craft by one
        WCheckbox craftByOne = new WCheckbox(Config.INSTANCE.autoCraft.craftByOne);
        WLabel craftByOneLabel = new WLabel("Craft by one:");
        craftByOneLabel.tooltip = "Craft items one by one.";
        craftByOne.setAction(wCheckbox -> Config.INSTANCE.autoCraft.craftByOne = wCheckbox.checked);
        craftByOne.tooltip = "Craft items one by one.";
        grid.addRow(craftByOneLabel, craftByOne);

        // Stop when no ingredients
        WCheckbox stopWhenNoIngredients = new WCheckbox(Config.INSTANCE.autoCraft.stopWhenNoIngredients);
        WLabel stopWhenNoIngredientsLabel = new WLabel("Stop when no ingredients:");
        stopWhenNoIngredientsLabel.tooltip = "Stop crafting items when you run out of ingredients.";
        stopWhenNoIngredients.setAction(wCheckbox -> Config.INSTANCE.autoCraft.stopWhenNoIngredients = wCheckbox.checked);
        stopWhenNoIngredients.tooltip = "Stop crafting items when you run out of ingredients.";
        grid.addRow(stopWhenNoIngredientsLabel, stopWhenNoIngredients);

        layout();
    }
}
