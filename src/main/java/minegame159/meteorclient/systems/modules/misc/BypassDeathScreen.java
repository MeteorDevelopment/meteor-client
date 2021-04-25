/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.misc;

//Created by squidoodly 27/05/2020

import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;

public class BypassDeathScreen extends Module {
    public boolean shouldBypass = false;

    public BypassDeathScreen(){
        super(Categories.Misc, "bypass-death-screen", "Lets you spy on people after death.");
    }

    @Override
    public void onDeactivate() {
        shouldBypass = false;
        super.onDeactivate();
    }
}
