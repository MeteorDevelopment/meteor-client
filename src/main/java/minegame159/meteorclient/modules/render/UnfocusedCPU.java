/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class UnfocusedCPU extends Module {
    public UnfocusedCPU() {
        super(Category.Render, "Unfocused-CPU", "Will not render anything when your Minecraft window is not focused.");
    }
}