package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.gui.widgets.WCheckbox;
import minegame159.meteorclient.gui.widgets.WLabel;

public class AutoCraftScreen extends WindowScreen {
    public AutoCraftScreen() {
        super("Auto Craft", true);

        // Craft by one
        WLabel craftByOneLabel = add(new WLabel("Craft by one:")).getWidget();
        craftByOneLabel.tooltip = "Craft items one by one.";

        WCheckbox craftByOne = add(new WCheckbox(Config.INSTANCE.autoCraft.isCraftByOne())).getWidget();
        craftByOne.action = checkbox -> Config.INSTANCE.autoCraft.setCraftByOne(checkbox.checked);
        craftByOne.tooltip = "Craft items one by one.";
        row();

        // Stop when no ingredients
        WLabel stopWhenNoIngredientsLabel = add(new WLabel("Stop when no ingredients:")).getWidget();
        stopWhenNoIngredientsLabel.tooltip = "Stop crafting items when you run out of ingredients.";

        WCheckbox stopWhenNoIngredients = add(new WCheckbox(Config.INSTANCE.autoCraft.isStopWhenNoIngredients())).getWidget();
        stopWhenNoIngredients.action = checkbox -> Config.INSTANCE.autoCraft.setStopWhenNoIngredients(checkbox.checked);
        stopWhenNoIngredients.tooltip = "Stop crafting items when you run out of ingredients.";
    }
}
