/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Quaternion;

public class HandView extends Module {
    public enum SwingMode {
        Offhand,
        Mainhand,
        None
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSwing = settings.createGroup("Swing");

    private final Setting<Double> rotationX = sgGeneral.add(new DoubleSetting.Builder()
            .name("rotation-x")
            .description("The X rotation of your hands.")
            .defaultValue(0.00)
            .sliderMin(-1)
            .sliderMax(1)
            .build()
    );

    private final Setting<Double> rotationY = sgGeneral.add(new DoubleSetting.Builder()
            .name("rotation-y")
            .description("The Y rotation of your hands.")
            .defaultValue(0.00)
            .sliderMin(-1)
            .sliderMax(1)
            .build()
    );

    private final Setting<Double> rotationZ = sgGeneral.add(new DoubleSetting.Builder()
            .name("rotation-z")
            .description("The Z rotation of your hands.")
            .defaultValue(0.00)
            .sliderMin(-1)
            .sliderMax(1)
            .build()
    );

    private final Setting<Double> scaleX = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale-x")
            .description("The X scale of the items rendered in your hands.")
            .defaultValue(0.75)
            .sliderMin(0)
            .sliderMax(1.5)
            .build()
    );

    private final Setting<Double> scaleY = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale-y")
            .description("The Y scale of the items rendered in your hands.")
            .defaultValue(0.60)
            .sliderMin(0)
            .sliderMax(2)
            .build()
    );

    private final Setting<Double> scaleZ = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale-z")
            .description("The Z scale of the items rendered in your hands.")
            .defaultValue(1.00)
            .sliderMin(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Double> posX = sgGeneral.add(new DoubleSetting.Builder()
            .name("pos-x")
            .description("The X offset of your hands.")
            .defaultValue(0.00)
            .sliderMin(-3)
            .sliderMax(3)
            .build()
    );

    private final Setting<Double> posY = sgGeneral.add(new DoubleSetting.Builder()
            .name("pos-y")
            .description("The Y offset of your hands.")
            .defaultValue(0.00)
            .sliderMin(-3)
            .sliderMax(3)
            .build()
    );

    private final Setting<Double> posZ = sgGeneral.add(new DoubleSetting.Builder()
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

    public final Setting<SwingMode> swingMode = sgSwing.add(new EnumSetting.Builder<SwingMode>()
            .name("swing-mode")
            .description("Modifies your client & server hand swinging.")
            .defaultValue(SwingMode.None)
            .build()
    );

    public HandView() {
        super(Categories.Render, "hand-view", "Alters the way items are rendered in your hands.");
    }

    public void transform(MatrixStack matrices) {
        if (!isActive()) return;

        matrices.scale(scaleX.get().floatValue(), scaleY.get().floatValue(), scaleZ.get().floatValue());
        matrices.translate(posX.get(), posY.get(), posZ.get());
        matrices.multiply(Quaternion.method_35825(rotationX.get().floatValue(), rotationY.get().floatValue(), rotationZ.get().floatValue()));
    }
}
