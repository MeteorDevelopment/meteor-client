/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;

public abstract class AddAccountScreen extends WindowScreen {
    public final AccountsScreen parent;
    public WButton add;

    protected AddAccountScreen(GuiTheme theme, String title, AccountsScreen parent) {
        super(theme, title);
        this.parent = parent;
    }
}
