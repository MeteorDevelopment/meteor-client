/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;

public class NoBreakDelay extends Module {
    public NoBreakDelay() {
        super(Categories.Player, "no-break-delay", "Completely removes the delay between breaking blocks.");
    }
}