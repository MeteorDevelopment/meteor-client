/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.misc;

import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;

public class MapPreview extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public MapPreview() {
        super(Categories.Misc, "map-preview", "View your map art from your inventory.");
    }

    private final Setting<Integer> scale = sgGeneral.add(new IntSetting.Builder()
        .name("scale")
        .description("The scale of the map.")
        .defaultValue(100)
        .min(1)
        .sliderMax(500)
        .build()
    );

    public int getScale() {
        return scale.get();
    }
}
