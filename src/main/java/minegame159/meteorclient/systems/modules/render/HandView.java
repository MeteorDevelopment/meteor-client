/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;

public class HandView extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    private final SettingGroup sgSwing = settings.createGroup("Swing");

    public final Setting<Double> rotationX = sgDefault.add(new DoubleSetting.Builder()
            .name("rotation-x")
            .description("The X rotation of your hands.")
            .defaultValue(0.00)
            .sliderMin(-0.2)
            .sliderMax(0.2)
            .build()
    );

    public final Setting<Double> rotationY = sgDefault.add(new DoubleSetting.Builder()
            .name("rotation-y")
            .description("The Y rotation of your hands.")
            .defaultValue(0.00)
            .sliderMin(-0.2)
            .sliderMax(0.2)
            .build()
    );

    public final Setting<Double> rotationZ = sgDefault.add(new DoubleSetting.Builder()
            .name("rotation-z")
            .description("The Z rotation of your hands.")
            .defaultValue(0.00)
            .sliderMin(-0.25)
            .sliderMax(0.25)
            .build()
    );

    public final Setting<Double> scaleX = sgDefault.add(new DoubleSetting.Builder()
            .name("scale-x")
            .description("The X scale of the items rendered in your hands.")
            .defaultValue(0.75)
            .sliderMin(0)
            .sliderMax(1.5)
            .build()
    );

    public final Setting<Double> scaleY = sgDefault.add(new DoubleSetting.Builder()
            .name("scale-y")
            .description("The Y scale of the items rendered in your hands.")
            .defaultValue(0.60)
            .sliderMin(0)
            .sliderMax(2)
            .build()
    );

    public final Setting<Double> scaleZ = sgDefault.add(new DoubleSetting.Builder()
            .name("scale-z")
            .description("The Z scale of the items rendered in your hands.")
            .defaultValue(1.00)
            .sliderMin(0)
            .sliderMax(5)
            .build()
    );

    public final Setting<Double> posX = sgDefault.add(new DoubleSetting.Builder()
            .name("pos-x")
            .description("The X offset of your hands.")
            .defaultValue(0.00)
            .sliderMin(-3)
            .sliderMax(3)
            .build()
    );

    public final Setting<Double> posY = sgDefault.add(new DoubleSetting.Builder()
            .name("pos-y")
            .description("The Y offset of your hands.")
            .defaultValue(0.00)
            .sliderMin(-3)
            .sliderMax(3)
            .build()
    );

    public final Setting<Double> posZ = sgDefault.add(new DoubleSetting.Builder()
            .name("pos-z")
            .description("The Z offset of your hands.")
            .defaultValue(-0.10)
            .sliderMin(-3)
            .sliderMax(3)
            .build()
    );

    public final Setting<Double> mainSwing = sgSwing.add(new DoubleSetting.Builder()
            .name("main-swing-progress")
            .description("The swing progress of your mainhand.")
            .defaultValue(0)
            .sliderMin(0)
            .sliderMax(1)
            .build()
    );

    public final Setting<Double> offSwing = sgSwing.add(new DoubleSetting.Builder()
            .name("off-swing-progress")
            .description("The swing progress of your offhand.")
            .defaultValue(0)
            .sliderMin(0)
            .sliderMax(1)
            .build()
    );

    public final Setting<Boolean> offhandSwing = sgSwing.add(new BoolSetting.Builder()
            .name("offhand-swing")
            .description("Makes you swing with your off-hand instead of your main-hand.")
            .defaultValue(false)
            .build()
    );

    public HandView() {
        super(Categories.Render, "hand-view", "Alters the way items are rendered in your hands.");
    }
}