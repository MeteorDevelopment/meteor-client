/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;

public class WModuleCategory extends WWindow {
    public WModuleCategory(Category category) {
        super(category.toString(), GuiConfig.get().getWindowConfig(get(category)).isExpanded(), true);
        type = get(category);

        action = () -> GuiConfig.get().getWindowConfig(type).setPos(x, y);

        pad(0);
        getDefaultCell().space(0);

        for (Module module : Modules.get().getGroup(category)) {
            add(new WModule(module)).fillX().expandX();
            row();
        }
    }

    private static GuiConfig.WindowType get(Category category) {
        switch (category) {
            case Combat:   return GuiConfig.WindowType.Combat;
            case Player:   return GuiConfig.WindowType.Player;
            case Movement: return GuiConfig.WindowType.Movement;
            case Render:   return GuiConfig.WindowType.Render;
            case Misc:     return GuiConfig.WindowType.Misc;
        }

        return null;
    }
}
