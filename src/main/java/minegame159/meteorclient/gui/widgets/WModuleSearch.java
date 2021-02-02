/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import net.minecraft.util.Pair;

import java.util.List;

public class WModuleSearch extends WWindow {
    private WTextBox filter;

    public WModuleSearch() {
        super("Search", GuiConfig.get().getWindowConfig(GuiConfig.WindowType.Search).isExpanded(), true);
        type = GuiConfig.WindowType.Search;

        action = () -> GuiConfig.get().getWindowConfig(type).setPos(x, y);

        initWidgets(true);
    }

    private void initWidgets(boolean first) {
        boolean focused = filter != null && filter.isFocused();

        // Search bar
        filter = add(new WTextBox(filter != null ? filter.getText() : "", 140)).fillX().expandX().getWidget();
        filter.setFocused(focused || (first && isExpanded()));
        filter.action = () -> {
            clear();
            initWidgets(false);
        };
        row();

        // Modules
        if (!filter.getText().isEmpty()) {
            // Titles
            List<Pair<Module, Integer>> modules = Modules.get().searchTitles(filter.getText());
            if (modules.size() > 0) {
                WSection section = add(new WSection("Modules", true)).getWidget();
                row();

                for (Pair<Module, Integer> pair : modules) {
                    section.add(new WModule(pair.getLeft())).fillX().expandX().space(0);
                    section.row();
                }
            }

            // Settings
            modules = Modules.get().searchSettingTitles(filter.getText());
            if (modules.size() > 0) {
                WSection section = add(new WSection("Settings", true)).getWidget();
                row();

                for (Pair<Module, Integer> pair : modules) {
                    section.add(new WModule(pair.getLeft())).fillX().expandX().space(0);
                    section.row();
                }
            }
        }
    }
}
