/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class BetterBeacons extends Module {
    public BetterBeacons() {
        super(Categories.Misc, "better-beacons", "Select effects unaffected by beacon level.");
    }
}
