/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.tabs.builtin;

import motordevelopment.motorclient.gui.GuiTheme;
import motordevelopment.motorclient.gui.GuiThemes;
import motordevelopment.motorclient.gui.tabs.Tab;
import motordevelopment.motorclient.gui.tabs.TabScreen;
import net.minecraft.client.gui.screen.Screen;

public class ModulesTab extends Tab {
    public ModulesTab() {
        super("Modules");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return theme.modulesScreen();
    }

    @Override
    public boolean isScreen(Screen screen) {
        return GuiThemes.get().isModulesScreen(screen);
    }
}
