package minegame159.meteorclient.gui;

import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.utils.Box;
import minegame159.meteorclient.utils.Vector2;

public abstract class WidgetLayout {
    public abstract void reset(WWidget widget);

    public abstract Vector2 calculateAutoSize(WWidget widget);

    public abstract Box layoutWidget(WWidget widget, WWidget child);
}
