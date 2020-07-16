package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.gui.widgets.Cell;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.gui.widgets.WWindow;

public class WindowScreen extends WidgetScreen {
    protected WWindow window;

    public WindowScreen(String title, boolean expanded) {
        super(title);

        initWidgets(expanded);
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

    private void initWidgets(boolean expanded) {
        window = super.add(new WWindow(title, expanded)).centerXY().getWidget();
    }
}
