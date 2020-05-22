package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class AntiSpam extends ToggleModule {
    public AntiSpam() {
        super(Category.Misc, "anti-spam", "Repeated messages not shown.");
    }
}
