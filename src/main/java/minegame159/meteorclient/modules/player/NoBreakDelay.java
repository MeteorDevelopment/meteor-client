package minegame159.meteorclient.modules.player;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class NoBreakDelay extends ToggleModule {
    public NoBreakDelay() {
        super(Category.Player, "no-break-delay", "Removes the delay between breaking blocks.");
    }
}