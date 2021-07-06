/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.settings.StringListSetting;

import java.util.ArrayList;

public class StringListSettingScreen extends WindowScreen {

    private final StringListSetting setting;
    private final ArrayList<String> strings;
    private final WTable table;
    private String newText = "";

    public StringListSettingScreen(GuiTheme theme, StringListSetting setting) {
        super(theme, "String List");
        this.setting = setting;
        strings = new ArrayList<>(setting.get());

        table = add(theme.table()).expandX().widget();
        initWidgets();
    }

    @Override
    protected void init() {
        super.init();

        table.clear();
        initWidgets();
    }

    private void initWidgets() {
        strings.removeIf(String::isEmpty);
        if (!strings.equals(setting.get())) {
            setting.set(strings);
            setting.changed();
        }

        for (int i = 0; i < setting.get().size(); i++) {
            int msgI = i;
            String message = setting.get().get(i);

            WTextBox textBox = table.add(theme.textBox(message)).expandX().widget();
            textBox.action = () -> {
                strings.set(msgI, textBox.get());
            };

            WMinus delete = table.add(theme.minus()).widget();
            delete.action = () -> {
                strings.remove(msgI);

                table.clear();
                initWidgets();
            };

            table.row();
        }

        if (!setting.get().isEmpty()) {
            table.add(theme.horizontalSeparator()).expandX();
            table.row();
        }

        WTextBox textBox = table.add(theme.textBox(newText)).minWidth(100).expandX().widget();
        textBox.action = () -> newText = textBox.get();

        WPlus add = table.add(theme.plus()).widget();
        add.action = () -> {
            strings.add(newText);
            newText = "";

            table.clear();
            initWidgets();
        };
    }
}

