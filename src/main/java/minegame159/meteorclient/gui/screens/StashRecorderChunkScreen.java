package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.modules.misc.StashFinder;
import minegame159.meteorclient.gui.widgets.WHorizontalSeparator;
import minegame159.meteorclient.gui.widgets.WLabel;

public class StashRecorderChunkScreen extends WindowScreen {
    public StashRecorderChunkScreen(StashFinder.Chunk chunk) {
        super("Chunk at " + chunk.x + ", " + chunk.z, true);

        add(new WLabel("Total:"));
        add(new WLabel(chunk.getTotal() + ""));
        row();

        add(new WHorizontalSeparator()).fillX().expandX();
        row();

        add(new WLabel("Chests:"));
        add(new WLabel(chunk.chests + ""));
        row();

        add(new WLabel("Barrels:"));
        add(new WLabel(chunk.barrels + ""));
        row();

        add(new WLabel("Shulkers:"));
        add(new WLabel(chunk.shulkers + ""));
        row();

        add(new WLabel("Ender Chests:"));
        add(new WLabel(chunk.enderChests + ""));
        row();

        add(new WLabel("Furnaces:"));
        add(new WLabel(chunk.furnaces + ""));
        row();

        add(new WLabel("Dispensers and droppers:"));
        add(new WLabel(chunk.dispensersDroppers + ""));
        row();

        add(new WLabel("Hoppers:"));
        add(new WLabel(chunk.hoppers + ""));
    }
}
