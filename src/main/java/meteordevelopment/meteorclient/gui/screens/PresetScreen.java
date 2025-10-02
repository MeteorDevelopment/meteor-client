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

public class PresetScreen<T> extends WindowScreen {
    Setting<T> setting;
    Runnable onChanged;

    public PresetScreen(GuiTheme theme, Setting<T> setting, Runnable onChanged) {
        super(theme, "Presets");
        this.setting = setting;
        this.onChanged = onChanged;
    }

    @Override
    public void initWidgets() {
        WTable table = add(theme.table()).expandX().minWidth(200).widget();
        initTable(table);
    }

    private void initTable(WTable table) {
        table.clear();

        for (Preset<T> preset : Presets.get().getPresetForSetting(setting)) {
            table.add(theme.label(preset.name)).expandCellX();

            WButton load = table.add(theme.button("Load")).widget();
            load.action = () -> {
                setting.set(preset.get());
                if (onChanged != null) onChanged.run();
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
