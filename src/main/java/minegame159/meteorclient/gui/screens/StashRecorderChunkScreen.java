package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.widgets.WGrid;
import minegame159.meteorclient.gui.widgets.WHorizontalList;
import minegame159.meteorclient.gui.widgets.WHorizontalSeparator;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.modules.misc.StashFinder;

public class StashRecorderChunkScreen extends WindowScreen {
    public StashRecorderChunkScreen(StashFinder.Chunk chunk) {
        super("Chunk at " + chunk.x + ", " + chunk.z);

        WHorizontalList total = add(new WHorizontalList(4));
        total.add(new WLabel("Total:"));
        total.add(new WLabel(chunk.getTotal() + ""));
        add(new WHorizontalSeparator());

        WGrid grid = add(new WGrid(4, 4, 2));
        grid.addRow(new WLabel("Chests:"), new WLabel(chunk.chests + ""));
        grid.addRow(new WLabel("Barrels:"), new WLabel(chunk.barrels + ""));
        grid.addRow(new WLabel("Shulkers:"), new WLabel(chunk.shulkers + ""));
        grid.addRow(new WLabel("Ender Chests:"), new WLabel(chunk.enderChests + ""));
        grid.addRow(new WLabel("Furnaces:"), new WLabel(chunk.furnaces + ""));
        grid.addRow(new WLabel("Dispensers and droppers:"), new WLabel(chunk.dispensersDroppers + ""));
        grid.addRow(new WLabel("Hoppers:"), new WLabel(chunk.hoppers + ""));

        layout();
    }
}
