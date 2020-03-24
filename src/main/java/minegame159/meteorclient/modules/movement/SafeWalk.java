package minegame159.meteorclient.modules.movement;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class SafeWalk extends Module {
    public static SafeWalk INSTANCE;

    public SafeWalk() {
        super(Category.Movement, "safe-walk", "Don't fall of blocks like if you were sneaking.");
    }
}
