package minegame159.meteorclient.modules.setting;

import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.macros.MacrosScreen;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class Macros extends Module {
    public Macros() {
        super(Category.Setting, "macros", "Macro list.", true, true, false);
    }

    @Override
    public WidgetScreen getCustomScreen() {
        return new MacrosScreen();
    }
}
