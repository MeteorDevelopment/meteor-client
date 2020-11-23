/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class LiquidInteract extends ToggleModule {
    public LiquidInteract() {
        super(Category.Player, "liquid-interact", "Allows you to interact with liquids.");
    }
}
