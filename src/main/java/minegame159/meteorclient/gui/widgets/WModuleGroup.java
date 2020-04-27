package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;

public class WModuleGroup extends WWindow {
    public static boolean MOVED = false;

    public final Category category;

    public WModuleGroup(Category category) {
        super(category.toString(), false, 0, 0, GuiConfig.WindowType.valueOf(category.toString()));
        this.category = category;

        onDragged = window -> {
            GuiConfig.WindowConfig winConfig = GuiConfig.INSTANCE.getWindowConfig(type, false);
            winConfig.setPos(x, y);
            MOVED = true;
        };

        for (Module module : ModuleManager.INSTANCE.getGroup(category)) {
            add(new WModule(module)).fillX().expandX();
            row();
        }
    }
}
