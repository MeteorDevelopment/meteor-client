/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.presets.Preset;
import meteordevelopment.meteorclient.systems.presets.Presets;

import java.util.List;

public class PresetScreen<T extends Setting<?>> extends WindowScreen {
    T setting;

    public PresetScreen(GuiTheme theme, T setting) {
        super(theme, "Presets");
        this.setting = setting;
    }

    @Override
    public void initWidgets() {
        WTable table = add(theme.table()).expandX().minWidth(400).widget();
        initTable(table);
    }

    private void initTable(WTable table) {
        List<Preset<T>> presets = Presets.get().getPresetForSetting(setting);
        table.clear();

        for (Preset<T> preset : presets) {
            table.add(theme.label(preset.name)).expandCellX();

            WButton load = table.add(theme.button("Load")).widget();
            load.action = () -> {
                setting = preset.setting;
                close();
            };

            WMinus remove = table.add(theme.minus()).widget();
            remove.action = () -> {
                Presets.get().remove(preset);
                initTable(table);
            };

            table.row();
        }

        table.add(theme.horizontalSeparator()).expandX();
        table.row();

        WTextBox name = table.add(theme.textBox("")).minWidth(400).expandX().widget();
        WButton create = table.add(theme.button("Save")).expandX().widget();
        create.action = () -> {
            if (name.get().isBlank()) return;
            Presets.get().add(new Preset<>(name.get(), setting));
            initTable(table);
        };
    }
}
