package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class NoBubbles extends ToggleModule {
    public NoBubbles() {
        super(Category.Render, "no-bubbles", "Disables rendering of bubbles in water.");
    }
}
