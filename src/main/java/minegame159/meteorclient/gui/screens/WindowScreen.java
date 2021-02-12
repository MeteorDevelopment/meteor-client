/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.gui.widgets.Cell;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.gui.widgets.WWindow;

public abstract class WindowScreen extends WidgetScreen {
    private final WWindow window;

    public WindowScreen(String title, boolean expanded) {
        super(title);

        window = super.add(new WWindow(title, expanded)).centerXY().getWidget();
    }

    @Override
    public <T extends WWidget> Cell<T> add(T widget) {
        return window.add(widget);
    }

    public void row() {
        window.row();
    }

    @Override
    public void clear() {
        window.clear();
    }
}
