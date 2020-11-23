/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class EChestPreview extends ToggleModule {
    public EChestPreview() {
        super(Category.Misc, "EChest-preview", "Stores what inside your echest and displays when you hover over it.");
    }
}
