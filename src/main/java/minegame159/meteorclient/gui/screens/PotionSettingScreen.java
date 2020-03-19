package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.PanelListScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WGrid;
import minegame159.meteorclient.gui.widgets.WItemWithLabel;
import minegame159.meteorclient.gui.widgets.WTextBox;
import minegame159.meteorclient.settings.PotionSetting;
import minegame159.meteorclient.utils.MyPotion;
import org.apache.commons.lang3.StringUtils;

public class PotionSettingScreen extends PanelListScreen {
    private PotionSetting setting;
    private WTextBox filter;

    public PotionSettingScreen(PotionSetting setting) {
        super("Select Potion");

        this.setting = setting;

        // Filter
        filter = new WTextBox("", 32);
        filter.boundingBox.fullWidth = true;
        filter.action = textBox -> {
            clear();
            initWidgets();
        };

        initWidgets();
    }

    private void initWidgets() {
        add(filter);

        // Potions
        WGrid grid = add(new WGrid(4, 4, 2));
        for (MyPotion potion : MyPotion.values()) {
            WItemWithLabel item = new WItemWithLabel(potion.potion);
            if (!filter.text.isEmpty()) {
                if (!StringUtils.containsIgnoreCase(item.getLabelText(), filter.text)) continue;
            }

            WButton select = new WButton("Select");
            select.action = () -> {
                setting.set(potion);
                onClose();
            };

            grid.addRow(item, select);
        }

        layout();
    }
}
