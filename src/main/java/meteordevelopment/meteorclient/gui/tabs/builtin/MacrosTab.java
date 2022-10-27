/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.builtin;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.screens.EditSystemScreen;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.macros.Macro;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import net.minecraft.client.gui.screen.Screen;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MacrosTab extends Tab {
    public MacrosTab() {
        super("Macros");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new MacrosScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof MacrosScreen;
    }

    private static class MacrosScreen extends WindowTabScreen {
        public MacrosScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
        }

        @Override
        public void initWidgets() {
            WTable table = add(theme.table()).expandX().minWidth(400).widget();
            initTable(table);

            add(theme.horizontalSeparator()).expandX();

            WButton create = add(theme.button("Create")).expandX().widget();
            create.action = () -> mc.setScreen(new EditMacroScreen(theme, null, this::reload));
        }

        private void initTable(WTable table) {
            table.clear();
            if (Macros.get().isEmpty()) return;

            for (Macro macro : Macros.get()) {
                table.add(theme.label(macro.name.get() + " (" + macro.keybind.get() + ")"));

                WButton edit = table.add(theme.button(GuiRenderer.EDIT)).expandCellX().right().widget();
                edit.action = () -> mc.setScreen(new EditMacroScreen(theme, macro, this::reload));

                WMinus remove = table.add(theme.minus()).widget();
                remove.action = () -> {
                    Macros.get().remove(macro);
                    reload();
                };

                table.row();
            }
        }

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard(Macros.get());
        }

        @Override
        public boolean fromClipboard() {
            return NbtUtils.fromClipboard(Macros.get());
        }
    }

    private static class EditMacroScreen extends EditSystemScreen<Macro> {
        public EditMacroScreen(GuiTheme theme, Macro value, Runnable reload) {
            super(theme, value, reload);
        }

        @Override
        public Macro create() {
            return new Macro();
        }

        @Override
        public boolean save() {
            if (value.name.get().isBlank()
                || value.messages.get().isEmpty()
                || !value.keybind.get().isSet()
            ) return false;

            if (isNew) {
                for (Macro m : Macros.get()) {
                    if (value.equals(m)) return false;
                }
            }

            if (isNew) Macros.get().add(value);
            else Macros.get().save();

            return true;
        }

        @Override
        public Settings getSettings() {
            return value.settings;
        }
    }
}
