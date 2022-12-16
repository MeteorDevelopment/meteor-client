/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class IgnoreBorder extends Module {
    public IgnoreBorder() {
        super(Categories.World, "Ignore Border", "Disables worldborder restrictions");
    }
}
