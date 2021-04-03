/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */
package minegame159.meteorclient.systems.modules.render;

import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;

public class EChestPreview extends Module {
    public EChestPreview() {
        super(Categories.Render, "EChest-preview", "Stores what's inside your Ender Chest and displays when you hover over it.");
    }
}