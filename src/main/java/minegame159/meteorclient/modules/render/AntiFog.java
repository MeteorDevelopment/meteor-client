package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class AntiFog extends Module {
    public static AntiFog INSTANCE;

    public AntiFog() {
        super(Category.Render, "anti-fog", "Disables fog.");
    }
}
