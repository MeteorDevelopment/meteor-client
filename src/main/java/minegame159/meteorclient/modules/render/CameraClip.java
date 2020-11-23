/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class CameraClip extends ToggleModule {
    public CameraClip() {
        super(Category.Render, "camera-clip", "Allows your 3rd person camera to move through blocks..");
    }
}
