/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.screens.settings;

import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.settings.PotionSetting;
import minegame159.meteorclient.utils.misc.MyPotion;
import org.apache.commons.lang3.StringUtils;

public class PotionSettingScreen extends WindowScreen {
    private final PotionSetting setting;
    private final WTextBox filter;

    private String filterText = "";

    public PotionSettingScreen(PotionSetting setting) {
        super("Select Potion", true);

        this.setting = setting;

        // Filter
        filter = new WTextBox("", 400);
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.getText().trim();

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
            if (!filterText.isEmpty()) {
                if (!StringUtils.containsIgnoreCase(item.getLabelText(), filterText)) continue;
            }
            table.add(item);

            WButton select = table.add(new WButton("Select")).getWidget();
            select.action = () -> {
                setting.set(potion);
                onClose();
            };
            table.add(new WHorizontalSeparator());
        }
    }
}
