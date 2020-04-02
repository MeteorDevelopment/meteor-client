package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.gui.widgets.WWindow;

public class WindowScreen extends WidgetScreen {
    private WWindow window;

    private final double horizontalMargin, spacing;

    public WindowScreen(String title, double horizontalMargin, double spacing, boolean expanded) {
        super(title);

        this.horizontalMargin = horizontalMargin;
        this.spacing = spacing;

        window = super.add(new WWindow(title, null, horizontalMargin, spacing, expanded));
    }

    public WindowScreen(String title) {
        this(title, 4, 4, true);
    }

    @Override
    public void clear() {
        super.clear();
        window = super.add(new WWindow(title.asString(), null, horizontalMargin, spacing, window.isExpanded()));
    }

    @Override
    public <T extends WWidget> T add(T widget) {
        return window.add(widget);
    }
}
