package minegame159.meteorclient.gui.clickgui;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.gui.GuiThings;
import minegame159.meteorclient.gui.widgets.WHorizontalSeparator;
import minegame159.meteorclient.gui.widgets.WTextBox;
import minegame159.meteorclient.gui.widgets.WWindow;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;

import java.util.List;

public class WSearch extends WWindow {
    private WTextBox filter;

    public WSearch() {
        super("Search", Config.WindowType.Search, 4, 4, false);

        onDragged = panel -> {
            Config.WindowConfig winConfig = Config.INSTANCE.getWindowConfig(getType(), false);
            winConfig.setPos(panel.boundingBox.x, panel.boundingBox.y);
        };

        initWidgets();
    }

    private void initWidgets() {
        boolean focused = filter != null && filter.isFocused();
        if (focused) GuiThings.setPostKeyEvents(false);
        filter = add(new WTextBox(filter != null ? filter.text : "", 70));
        filter.setFocused(focused);
        filter.boundingBox.fullWidth = true;
        filter.action = textBox -> {
            clear();
            initWidgets();
            layout();
        };

        if (!filter.text.isEmpty()) {
            // Titles
            List<Module> modules = ModuleManager.INSTANCE.searchTitles(filter.text);
            if (modules.size() > 0) add(new WHorizontalSeparator("Modules"));
            for (Module module : modules) add(new WModule(module));

            // Setting titles
            modules = ModuleManager.INSTANCE.searchSettingTitles(filter.text);
            add(new WHorizontalSeparator("Settings"));
            for (Module module : modules) add(new WModule(module));
        }
    }
}
