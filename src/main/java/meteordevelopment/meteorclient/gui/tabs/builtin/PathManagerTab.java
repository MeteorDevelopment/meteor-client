/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.builtin;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.pathing.PathManagers;
import net.minecraft.client.gui.screen.Screen;

public class PathManagerTab extends Tab {
    public PathManagerTab() {
        super(PathManagers.get().getName());
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new PathManagerScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof PathManagerScreen;
    }

    private static class PathManagerScreen extends WindowTabScreen {
        public PathManagerScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);

            PathManagers.get().getSettings().get().onActivated();
        }

        @Override
        public void initWidgets() {
            WTextBox filter = add(theme.textBox("")).minWidth(400).expandX().widget();
            filter.setFocused(true);
            filter.action = () -> {
                clear();

                add(filter);
                add(theme.settings(PathManagers.get().getSettings().get(), filter.get().trim())).expandX();
            };

            add(theme.settings(PathManagers.get().getSettings().get(), filter.get().trim())).expandX();
        }

        @Override
        protected void onClosed() {
            PathManagers.get().getSettings().save();
        }
    }
}
