/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class AntiPacketKick extends ToggleModule {
    public AntiPacketKick() {
        super(Category.Misc, "anti-chunk-ban", "Prevents you from getting kicked by huge packets.");
    }
}
