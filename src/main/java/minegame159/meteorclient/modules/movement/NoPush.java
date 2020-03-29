package minegame159.meteorclient.modules.movement;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class NoPush extends ToggleModule {
    public NoPush() {
        super(Category.Movement, "no-push", "Prevents u from getting pushed by mobs, taking damage, etc.");
    }
}
