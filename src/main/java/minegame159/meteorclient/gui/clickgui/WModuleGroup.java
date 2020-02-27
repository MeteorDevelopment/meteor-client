package minegame159.meteorclient.gui.clickgui;

import minegame159.meteorclient.gui.Alignment;
import minegame159.meteorclient.gui.widgets.WHorizontalSeparator;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WPanel;
import minegame159.meteorclient.gui.widgets.WVerticalList;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;

import java.util.List;

public class WModuleGroup extends WPanel {
    Category category;

    private WVerticalList list;

    public WModuleGroup(Category category) {
        boundingBox.setMargin(6, 1);
        boundingBox.marginTop = 6;

        this.category = category;

        list = add(new WVerticalList(0));

        // Name
        WLabel name = list.add(new WLabel(category.toString(), true));
        name.boundingBox.alignment.x = Alignment.X.Center;
        name.boundingBox.marginBottom = 4;

        // Modules
        List<Module> group = ModuleManager.getGroup(category);
        for (int i = 0; i < group.size(); i++) {
            Module module = group.get(i);

            list.add(new WHorizontalSeparator());

            WModule wModule = list.add(new WModule(module));
            wModule.boundingBox.fullWidth = true;
            wModule.boundingBox.setMargin(4);
        }
    }

    @Override
    public void onWindowResized(int width, int height) {
        list.maxHeight = height - 32;
        list.calculateSize();
        list.calculatePosition();
    }
}
