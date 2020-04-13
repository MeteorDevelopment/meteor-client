package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class NoClip extends ToggleModule {
    public NoClip() {
        super(Category.Render, "no-clip", "Allows your 3rd person camera to move through blocks..");
    }
}
