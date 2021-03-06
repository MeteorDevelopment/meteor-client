/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;

public class WModuleCategory extends WWindow {
    public WModuleCategory(Category category) {
        super(category.toString(), category.windowConfig.isExpanded(), true, GuiConfig.get().displayIcons ? category.icon : null);
        this.type = GuiConfig.WindowType.Category;
        this.category = category;

        action = () -> getWindowConfig().setPos(x, y);

        pad(0);
        getDefaultCell().space(0);

        for (Module module : Modules.get().getGroup(category)) {
            add(new WModule(module)).fillX().expandX();
            row();
        }
    }

}
