package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class UnfocusedCPU extends ToggleModule {
    public UnfocusedCPU() {
        super(Category.Misc, "unfocused-CPU", "Doesn't render anything when window is not focused.");
    }
}
