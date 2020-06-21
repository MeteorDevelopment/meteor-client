package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WItemWithLabel;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.gui.widgets.WTextBox;
import minegame159.meteorclient.settings.PotionSetting;
import minegame159.meteorclient.utils.MyPotion;
import org.apache.commons.lang3.StringUtils;

public class PotionSettingScreen extends WindowScreen {
    private PotionSetting setting;
    private WTextBox filter;

    public PotionSettingScreen(PotionSetting setting) {
        super("Select Potion", true);

        this.setting = setting;

        // Filter
        filter = new WTextBox("", 200);
        filter.setFocused(true);
        filter.action = textBox -> {
            clear();
            initWidgets();
        };

        initWidgets();
    }

    private void initWidgets() {
        add(filter).fillX().expandX().getWidget();
        row();

        // Potions
        WTable table = add(new WTable()).getWidget();
        for (MyPotion potion : MyPotion.values()) {
            WItemWithLabel item = new WItemWithLabel(potion.potion);
            if (!filter.text.isEmpty()) {
                if (!StringUtils.containsIgnoreCase(item.getLabelText(), filter.text)) continue;
            }
            table.add(item);

            WButton select = table.add(new WButton("Select")).getWidget();
            select.action = button -> {
                setting.set(potion);
                onClose();
            };

            table.row();
        }
    }
}
