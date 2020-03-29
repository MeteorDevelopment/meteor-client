package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class NoHurtCam extends ToggleModule {
    public NoHurtCam() {
        super(Category.Render, "no-hurt-cam", "Disables hurt camera effect.");
    }
}
