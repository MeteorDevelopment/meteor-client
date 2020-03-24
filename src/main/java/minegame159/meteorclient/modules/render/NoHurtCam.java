package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class NoHurtCam extends Module {
    public static NoHurtCam INSTANCE;

    public NoHurtCam() {
        super(Category.Render, "no-hurt-cam", "Disables hurt camera effect.");
    }
}
