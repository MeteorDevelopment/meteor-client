/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;

public class CameraClip extends Module {
    public CameraClip() {
        super(Categories.Render, "camera-clip", "Allows your third person camera to clip through blocks.");
    }
}
