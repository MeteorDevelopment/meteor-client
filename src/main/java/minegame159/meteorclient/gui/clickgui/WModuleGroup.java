package minegame159.meteorclient.gui.clickgui;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.gui.widgets.WWindow;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;

public class WModuleGroup extends WWindow {
    public final Category category;

    public WModuleGroup(Category category) {
        super(category.toString(), Config.WindowType.valueOf(category.toString()), 0, 0, false);

        this.category = category;

        onDragged = panel -> {
            Config.WindowConfig winConfig = Config.INSTANCE.getWindowConfig(getType(), false);
            winConfig.setPos(panel.boundingBox.x, panel.boundingBox.y);
        };

        for (Module module : ModuleManager.INSTANCE.getGroup(category)) {
            add(new WModule(module));
        }
    }
}
