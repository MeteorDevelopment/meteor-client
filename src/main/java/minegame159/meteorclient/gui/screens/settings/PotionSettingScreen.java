/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.screens.settings;

import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.WindowScreen;
import minegame159.meteorclient.gui.widgets.containers.WTable;
import minegame159.meteorclient.gui.widgets.pressable.WButton;
import minegame159.meteorclient.settings.PotionSetting;
import minegame159.meteorclient.utils.misc.MyPotion;

public class PotionSettingScreen extends WindowScreen {
    public PotionSettingScreen(GuiTheme theme, PotionSetting setting) {
        super(theme, "Select potion");

        WTable table = add(theme.table()).expandX().widget();

        for (MyPotion potion : MyPotion.values()) {
            table.add(theme.itemWithLabel(potion.potion, potion.potion.getName().getString()));

            WButton select = table.add(theme.button("Select")).widget();
            select.action = () -> {
                setting.set(potion);
                onClose();
            };

            table.row();
        }
    }
}
