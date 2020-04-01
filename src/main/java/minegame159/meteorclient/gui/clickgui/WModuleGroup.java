package minegame159.meteorclient.gui.clickgui;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.gui.widgets.WWindow;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.Vector2;

public class WModuleGroup extends WWindow {
    public final Category category;

    public WModuleGroup(Category category) {
        super(category.toString(), 0, 0);

        this.category = category;

        onDragged = panel -> {
            Vector2 pos = Config.INSTANCE.getGuiPositionNotNull(category);
            pos.x = panel.boundingBox.x;
            pos.y = panel.boundingBox.y;
        };

        for (Module module : ModuleManager.INSTANCE.getGroup(category)) {
            add(new WModule(module));
        }
    }
}
