package minegame159.meteorclient.gui.screens.topbar;

import minegame159.meteorclient.gui.TopBarType;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.gui.widgets.WTopBar;

public class TopBarScreen extends WidgetScreen {
    public final TopBarType type;

    public TopBarScreen(TopBarType type) {
        super(type.toString());
        this.type = type;

        add(new WTopBar()).centerX();
    }
}
