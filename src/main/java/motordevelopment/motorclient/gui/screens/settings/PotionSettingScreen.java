/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.screens.settings;

import motordevelopment.motorclient.gui.GuiTheme;
import motordevelopment.motorclient.gui.WindowScreen;
import motordevelopment.motorclient.gui.widgets.containers.WTable;
import motordevelopment.motorclient.gui.widgets.pressable.WButton;
import motordevelopment.motorclient.settings.PotionSetting;
import motordevelopment.motorclient.utils.misc.MyPotion;
import net.minecraft.client.resource.language.I18n;

public class PotionSettingScreen extends WindowScreen {
    private final PotionSetting setting;

    public PotionSettingScreen(GuiTheme theme, PotionSetting setting) {
        super(theme, "Select Potion");

        this.setting = setting;
    }

    @Override
    public void initWidgets() {
        WTable table = add(theme.table()).expandX().widget();

        for (MyPotion potion : MyPotion.values()) {
            table.add(theme.itemWithLabel(potion.potion, I18n.translate(potion.potion.getItem().getTranslationKey())));

            WButton select = table.add(theme.button("Select")).widget();
            select.action = () -> {
                setting.set(potion);
                close();
            };

            table.row();
        }
    }
}
