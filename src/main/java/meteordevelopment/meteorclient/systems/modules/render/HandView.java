/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.HeldItemRendererEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;

public class HandView extends Module {
    private final SettingGroup mainHand = settings.createGroup("Main Hand");
    private final SettingGroup offHand = settings.createGroup("Off Hand");
    private final SettingGroup sgSwing = settings.createGroup("Swing");

    // Scale

    private final Setting<Double> scaleXMain = mainHand.add(new DoubleSetting.Builder()
        .name("Scale X")
        .description("The X scale of your main hand.")
        .defaultValue(1)
        .sliderMax(5)
        .build()
    );

    private final Setting<Double> scaleYMain = mainHand.add(new DoubleSetting.Builder()
        .name("Scale Y")
        .description("The Y scale of your main hand.")
        .defaultValue(1)
        .sliderMax(5)
        .build()
    );

    private final Setting<Double> scaleZMain = mainHand.add(new DoubleSetting.Builder()
        .name("Scale Z")
        .description("The Z scale of your main hand.")
        .defaultValue(1)
        .sliderMax(5)
        .build()
    );

    private final Setting<Double> scaleXOff = offHand.add(new DoubleSetting.Builder()
        .name("Scale X")
        .description("The X scale of your offhand.")
        .defaultValue(1)
        .sliderMax(5)
        .build()
    );

    private final Setting<Double> scaleYOff = offHand.add(new DoubleSetting.Builder()
        .name("Scale Y")
        .description("The Y scale of your offhand.")
        .defaultValue(1)
        .sliderMax(5)
        .build()
    );

    private final Setting<Double> scaleZOff = offHand.add(new DoubleSetting.Builder()
        .name("Scale Z")
        .description("The Z scale of your offhand.")
        .defaultValue(1)
        .sliderMax(5)
        .build()
    );

    // Position

    private final Setting<Double> posXMain = mainHand.add(new DoubleSetting.Builder()
        .name("Position X")
        .description("The X position offset of your main hand.")
        .defaultValue(0)
        .sliderRange(-3, 3)
        .build()
    );

    private final Setting<Double> posYMain = mainHand.add(new DoubleSetting.Builder()
        .name("Position Y")
        .description("The Y position offset of your main hand.")
        .defaultValue(0)
        .sliderRange(-3, 3)
        .build()
    );

    private final Setting<Double> posZMain = mainHand.add(new DoubleSetting.Builder()
        .name("Position Z")
        .description("The Z position offset of your main hand.")
        .defaultValue(0)
        .sliderRange(-3, 3)
        .build()
    );

    private final Setting<Double> posXOff = offHand.add(new DoubleSetting.Builder()
        .name("Position X")
        .description("The X position offset of your offhand.")
        .defaultValue(0)
        .sliderRange(-3, 3)
        .build()
    );

    private final Setting<Double> posYOff = offHand.add(new DoubleSetting.Builder()
        .name("Position Y")
        .description("The Y position offset of your offhand.")
        .defaultValue(0)
        .sliderRange(-3, 3)
        .build()
    );

    private final Setting<Double> posZOff = offHand.add(new DoubleSetting.Builder()
        .name("Position Z")
        .description("The Z position offset of your offhand.")
        .defaultValue(0)
        .sliderRange(-3, 3)
        .build()
    );

    // Rotation

    private final Setting<Double> rotationXMain = mainHand.add(new DoubleSetting.Builder()
        .name("Rotation X")
        .description("The X orientation of your main hand.")
        .defaultValue(0)
        .sliderRange(-180, 180)
        .build()
    );

    private final Setting<Double> rotationYMain = mainHand.add(new DoubleSetting.Builder()
        .name("Rotation Y")
        .description("The Y orientation of your main hand.")
        .defaultValue(0)
        .sliderRange(-180, 180)
        .build()
    );

    private final Setting<Double> rotationZMain = mainHand.add(new DoubleSetting.Builder()
        .name("Rotation Z")
        .description("The Z orientation of your main hand.")
        .defaultValue(0)
        .sliderRange(-180, 180)
        .build()
    );

    private final Setting<Double> rotationXOff = offHand.add(new DoubleSetting.Builder()
        .name("Rotation X")
        .description("The X orientation of your offhand.")
        .defaultValue(0)
        .sliderRange(-180, 180)
        .build()
    );

    private final Setting<Double> rotationYOff = offHand.add(new DoubleSetting.Builder()
        .name("Rotation Y")
        .description("The Y orientation of your offhand.")
        .defaultValue(0)
        .sliderRange(-180, 180)
        .build()
    );

    private final Setting<Double> rotationZOff = offHand.add(new DoubleSetting.Builder()
        .name("Rotation Z")
        .description("The Z orientation of your offhand.")
        .defaultValue(0)
        .sliderRange(-180, 180)
        .build()
    );

    // Swing

    public final Setting<SwingMode> swingMode = sgSwing.add(new EnumSetting.Builder<SwingMode>()
        .name("mode")
        .description("Modifies your client & server hand swinging.")
        .defaultValue(SwingMode.None)
        .build()
    );

    public final Setting<Double> mainSwing = sgSwing.add(new DoubleSetting.Builder()
        .name("main-progress")
        .description("The swing progress of your main hand.")
        .defaultValue(0)
        .range(0, 1)
        .sliderMax(1)
        .build()
    );

    public final Setting<Double> offSwing = sgSwing.add(new DoubleSetting.Builder()
        .name("offhand-progress")
        .description("The swing progress of your offhand.")
        .defaultValue(0)
        .range(0, 1)
        .sliderMax(1)
        .build()
    );

    public final Setting<Integer> swingSpeed = sgSwing.add(new IntSetting.Builder()
        .name("swing-speed")
        .description("The swing speed of your hands. (higher = slower swing)")
        .defaultValue(6)
        .range(0, 20)
        .sliderMax(20)
        .build()
    );

    public HandView() {
        super(Categories.Render, "hand-view", "Alters the way items are rendered in your hands.");
    }

    @EventHandler
    private void onHeldItemRender(HeldItemRendererEvent event) {
        if (!isActive()) return;
        if (event.hand == Hand.MAIN_HAND) {
            event.matrix.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(rotationXMain.get().floatValue()));
            event.matrix.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(rotationYMain.get().floatValue()));
            event.matrix.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(rotationZMain.get().floatValue()));
            event.matrix.scale(scaleXMain.get().floatValue(), scaleYMain.get().floatValue(), scaleZMain.get().floatValue());
            event.matrix.translate(posXMain.get().floatValue(), posYMain.get().floatValue(), posZMain.get().floatValue());
        } else {
            event.matrix.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(rotationXOff.get().floatValue()));
            event.matrix.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(rotationYOff.get().floatValue()));
            event.matrix.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(rotationZOff.get().floatValue()));
            event.matrix.scale(scaleXOff.get().floatValue(), scaleYOff.get().floatValue(), scaleZOff.get().floatValue());
            event.matrix.translate(posXOff.get().floatValue(), posYOff.get().floatValue(), posZOff.get().floatValue());
        }
    }

    public enum SwingMode {
        Offhand,
        Mainhand,
        None
    }
}
