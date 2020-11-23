/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

//Created by squidoodly 27/05/2020

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class BypassDeathScreen extends ToggleModule {

    public boolean shouldBypass = false;

    public BypassDeathScreen(){
        super(Category.Misc, "bypass-death-screen", "Let's you spy on people");
    }

    @Override
    public void onDeactivate() {
        shouldBypass = false;
        super.onDeactivate();
    }
}
