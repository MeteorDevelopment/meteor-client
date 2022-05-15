/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.CustomStatListSetting;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class CustomStatListSettingScreen extends LeftRightListSettingScreen<Identifier> {

    public CustomStatListSettingScreen(GuiTheme theme, CustomStatListSetting setting) {
        super(theme, "Select Custom statistic", setting, setting.get(), Registry.CUSTOM_STAT);
    }

    @Override
    protected WWidget getValueWidget(Identifier value) {
        return theme.label(getValueName(value));
    }

    @Override
    protected String getValueName(Identifier value) {
        return new TranslatableText("stat." + value.toString().replace(':', '.')).getString();
    }
}
