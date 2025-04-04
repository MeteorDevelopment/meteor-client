/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.screens;

import motordevelopment.motorclient.gui.GuiTheme;
import motordevelopment.motorclient.gui.WindowScreen;
import motordevelopment.motorclient.gui.utils.Cell;
import motordevelopment.motorclient.gui.widgets.WWidget;
import motordevelopment.motorclient.gui.widgets.containers.WContainer;
import motordevelopment.motorclient.systems.modules.render.marker.BaseMarker;

public class MarkerScreen extends WindowScreen {
    private final BaseMarker marker;
    private WContainer settingsContainer;

    public MarkerScreen(GuiTheme theme, BaseMarker marker) {
        super(theme, marker.name.get());

        this.marker = marker;
    }

    @Override
    public void initWidgets() {
        // Settings
        if (!marker.settings.groups.isEmpty()) {
            settingsContainer = add(theme.verticalList()).expandX().widget();
            settingsContainer.add(theme.settings(marker.settings)).expandX();
        }

        // Custom widget
        WWidget widget = getWidget(theme);

        if (widget != null) {
            add(theme.horizontalSeparator()).expandX();
            Cell<WWidget> cell = add(widget);
            if (widget instanceof WContainer) cell.expandX();
        }
    }

    @Override
    public void tick() {
        super.tick();

        marker.settings.tick(settingsContainer, theme);
    }

    public WWidget getWidget(GuiTheme theme) {
        return null;
    }
}
