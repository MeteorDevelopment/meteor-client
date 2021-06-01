/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.utils;

import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.WidgetScreen;

public interface IScreenFactory {
    WidgetScreen createScreen(GuiTheme theme);
}
