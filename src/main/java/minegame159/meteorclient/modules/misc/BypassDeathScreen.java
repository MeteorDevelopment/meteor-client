/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

//Created by squidoodly 27/05/2020

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class BypassDeathScreen extends Module {

    public boolean shouldBypass = false;

    public BypassDeathScreen(){
        super(Category.Misc, "Bypass-Death-Screen", "Lets you spy on people after death.");
    }

    @Override
    public void onDeactivate() {
        shouldBypass = false;
        super.onDeactivate();
    }
}
