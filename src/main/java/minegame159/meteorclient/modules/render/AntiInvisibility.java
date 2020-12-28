/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class AntiInvisibility extends ToggleModule {
    public AntiInvisibility() {
        super(Category.Render, "anti-invisibility", "Renders invisible entities");
    }
}
