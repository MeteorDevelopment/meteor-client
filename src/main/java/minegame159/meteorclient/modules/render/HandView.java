/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class HandView extends ToggleModule {

    private final SettingGroup sgDefault = settings.getDefaultGroup();

    private final Setting<Double> rotationX = sgDefault.add(new DoubleSetting.Builder()
            .name("rotation-x")
            .description("The x rotation of your hands.")
            .defaultValue(0.00)
            .sliderMin(-0.2)
            .sliderMax(0.2)
            .build()
    );

    private final Setting<Double> rotationY = sgDefault.add(new DoubleSetting.Builder()
            .name("rotation-y")
            .description("The y rotation of your hands.")
            .defaultValue(0.00)
            .sliderMin(-0.2)
            .sliderMax(0.2)
            .build()
    );

    private final Setting<Double> rotationZ = sgDefault.add(new DoubleSetting.Builder()
            .name("rotation-z")
            .description("The z rotation of your hands.")
            .defaultValue(0.00)
            .sliderMin(-0.25)
            .sliderMax(0.25)
            .build()
    );

    private final Setting<Double> ScaleX = sgDefault.add(new DoubleSetting.Builder()
            .name("scale-x")
            .description("The x scale of the items rendered in your hands.")
            .defaultValue(0.75)
            .sliderMin(0)
            .sliderMax(1.5)
            .build()
    );

    private final Setting<Double> ScaleY = sgDefault.add(new DoubleSetting.Builder()
            .name("scale-y")
            .description("The y scale of the items rendered in your hands.")
            .defaultValue(0.60)
            .sliderMin(0)
            .sliderMax(2)
            .build()
    );

    private final Setting<Double> ScaleZ = sgDefault.add(new DoubleSetting.Builder()
            .name("scale-z")
            .description("The z scale of the items rendered in your hands.")
            .defaultValue(1.00)
            .sliderMin(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Double> PosX = sgDefault.add(new DoubleSetting.Builder()
            .name("pos-x")
            .description("The x offset of your hands.")
            .defaultValue(0.00)
            .sliderMin(-3)
            .sliderMax(3)
            .build()
    );

    private final Setting<Double> PosY = sgDefault.add(new DoubleSetting.Builder()
            .name("pos-y")
            .description("The y offset of your hands.")
            .defaultValue(0.00)
            .sliderMin(-3)
            .sliderMax(3)
            .build()
    );

    private final Setting<Double> PosZ = sgDefault.add(new DoubleSetting.Builder()
            .name("pos-z")
            .description("The z offset of your hands.")
            .defaultValue(-0.10)
            .sliderMin(-3)
            .sliderMax(3)
            .build()
    );


    public HandView() {
        super(Category.Render, "hand-view", "Changes the way items are rendered in your hands.");
    }

    public float rotationX() {
        return rotationX.get().floatValue();
    }
    public float rotationY() {
        return rotationY.get().floatValue();
    }
    public float rotationZ() {
        return rotationZ.get().floatValue();
    }

    public float scaleX() {
        return ScaleX.get().floatValue();
    }
    public float scaleY() {
        return ScaleY.get().floatValue();
    }
    public float scaleZ() {
        return ScaleZ.get().floatValue();
    }

    public float posX() {
        return PosX.get().floatValue();
    }
    public float posY() {
        return PosY.get().floatValue();
    }
    public float posZ() {
        return PosZ.get().floatValue();
    }
}