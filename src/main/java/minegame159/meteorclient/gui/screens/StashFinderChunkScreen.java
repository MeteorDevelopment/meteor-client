/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.widgets.WHorizontalSeparator;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.modules.misc.StashFinder;

public class StashFinderChunkScreen extends WindowScreen {
    public StashFinderChunkScreen(StashFinder.Chunk chunk) {
        super("Chunk at " + chunk.x + ", " + chunk.z, true);

        // Total
        add(new WLabel("Total:"));
        add(new WLabel(chunk.getTotal() + ""));
        row();

        add(new WHorizontalSeparator());
        row();

        // Separate
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
