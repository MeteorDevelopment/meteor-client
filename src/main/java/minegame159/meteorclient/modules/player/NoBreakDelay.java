/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class NoBreakDelay extends Module {
    public NoBreakDelay() {
        super(Category.Player, "No-Break-Delay", "Completely removes the delay between breaking blocks.");
    }
}