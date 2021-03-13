/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.world;

import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;

public class AntiCactus extends Module {
    public AntiCactus() {
        super(Categories.World, "anti-cactus", "Prevents you from taking damage from cacti.");
    }
}
