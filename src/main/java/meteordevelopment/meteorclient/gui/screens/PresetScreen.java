/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.presets.Preset;
import meteordevelopment.meteorclient.systems.presets.Presets;

public class PresetScreen<T extends Setting<?>> extends WindowScreen {
    T setting;

    public PresetScreen(GuiTheme theme, T setting) {
        super(theme, "Presets for " + setting.getClass().getTypeName());
    }

    @Override
    public void initWidgets() {
        WTable table = add(theme.table()).expandX().minWidth(400).widget();
        initTable(table);

        add(theme.horizontalSeparator()).expandX();

        WButton create = add(theme.button("Save")).expandX().widget();
        create.action = () -> {

        };
    }

    private void initTable(WTable table) {
        table.clear();

        for (Preset<?> profile : Presets.get().getAll()) {
            table.add(theme.label(profile.name)).expandCellX();


            WButton load = table.add(theme.button("Load")).widget();
            load.action = () -> {
                //load
            };

            WMinus remove = table.add(theme.minus()).widget();
            remove.action = () -> {
                //remove
                reload();
            };

            table.row();
        }
    }
}
