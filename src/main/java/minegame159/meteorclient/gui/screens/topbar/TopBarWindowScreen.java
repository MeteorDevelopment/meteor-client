/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.gui.screens.topbar;

import minegame159.meteorclient.gui.widgets.Cell;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.gui.widgets.WWindow;

public abstract class TopBarWindowScreen extends TopBarScreen {
    private final WWindow window;

    public TopBarWindowScreen(TopBarType type) {
        super(type);

        window = super.add(new WWindow(type.toString(), true)).centerXY().getWidget();

        addTopBar();
        initWidgets();
    }

    protected abstract void initWidgets();

    @Override
    public <T extends WWidget> Cell<T> add(T widget) {
        return window.add(widget);
    }

    protected void row() {
        window.row();
    }

    @Override
    public void clear() {
        window.clear();
    }
}
