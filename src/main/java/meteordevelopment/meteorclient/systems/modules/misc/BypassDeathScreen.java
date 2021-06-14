/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

//Created by squidoodly 27/05/2020

import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class BypassDeathScreen extends Module {
    public BypassDeathScreen() {
        super(Categories.Misc, "bypass-death-screen", "Lets you spy on people after death.");
    }
}
