/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.ArmRenderEvent;
import meteordevelopment.meteorclient.events.render.HeldItemRendererEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

public class HandView extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMainHand = settings.createGroup("Main Hand");
    private final SettingGroup sgOffHand = settings.createGroup("Off Hand");
    private final SettingGroup sgArm = settings.createGroup("Arm");
    private final SettingGroup sgSwing = settings.createGroup("Swing");

    //general
    private final Setting<Boolean> followRotations = sgGeneral.add(new BoolSetting.Builder()
        .name("follow-rotations")
        .description("Makes your hands follow your serverside rotations.")
        .defaultValue(false)
        .build()
    );

    // Scale
    private final Setting<Double> scaleXMain = sgMainHand.add(new DoubleSetting.Builder().name("mainhand-scale-x").description("The X scale of your main hand.").defaultValue(1).sliderMax(5).build());
    private final Setting<Double> scaleYMain = sgMainHand.add(new DoubleSetting.Builder().name("mainhand-scale-y").description("The Y scale of your main hand.").defaultValue(1).sliderMax(5).build());
    private final Setting<Double> scaleZMain = sgMainHand.add(new DoubleSetting.Builder().name("mainhand-scale-z").description("The Z scale of your main hand.").defaultValue(1).sliderMax(5).build());
    private final Setting<Double> scaleXOff = sgOffHand.add(new DoubleSetting.Builder().name("offhand-scale-x").description("The X scale of your offhand.").defaultValue(1).sliderMax(5).build());
    private final Setting<Double> scaleYOff = sgOffHand.add(new DoubleSetting.Builder().name("offhand-scale-y").description("The Y scale of your offhand.").defaultValue(1).sliderMax(5).build());
    private final Setting<Double> scaleZOff = sgOffHand.add(new DoubleSetting.Builder().name("offhand-scale-z").description("The Z scale of your offhand.").defaultValue(1).sliderMax(5).build());
    private final Setting<Double> scaleXArm = sgArm.add(new DoubleSetting.Builder().name("arm-scale-x").description("The X scale of your arm.").defaultValue(1).sliderMax(5).build());
    private final Setting<Double> scaleYArm = sgArm.add(new DoubleSetting.Builder().name("arm-scale-y").description("The Y scale of your arm.").defaultValue(1).sliderMax(5).build());
    private final Setting<Double> scaleZArm = sgArm.add(new DoubleSetting.Builder().name("arm-scale-z").description("The Z scale of your arm.").defaultValue(1).sliderMax(5).build());

    // Position
    private final Setting<Double> posXMain = sgMainHand.add(new DoubleSetting.Builder().name("mainhand-position-x").description("The X position offset of your main hand.").defaultValue(0).sliderRange(-3, 3).build());
    private final Setting<Double> posYMain = sgMainHand.add(new DoubleSetting.Builder().name("mainhand-position-y").description("The Y position offset of your main hand.").defaultValue(0).sliderRange(-3, 3).build());
    private final Setting<Double> posZMain = sgMainHand.add(new DoubleSetting.Builder().name("mainhand-position-z").description("The Z position offset of your main hand.").defaultValue(0).sliderRange(-3, 3).build());
    private final Setting<Double> posXOff = sgOffHand.add(new DoubleSetting.Builder().name("offhand-position-x").description("The X position offset of your offhand.").defaultValue(0).sliderRange(-3, 3).build());
    private final Setting<Double> posYOff = sgOffHand.add(new DoubleSetting.Builder().name("offhand-position-y").description("The Y position offset of your offhand.").defaultValue(0).sliderRange(-3, 3).build());
    private final Setting<Double> posZOff = sgOffHand.add(new DoubleSetting.Builder().name("offhand-position-z").description("The Z position offset of your offhand.").defaultValue(0).sliderRange(-3, 3).build());
    private final Setting<Double> posXArm = sgArm.add(new DoubleSetting.Builder().name("arm-position-x").description("The X position offset of your arm.").defaultValue(0).sliderRange(-3, 3).build());
    private final Setting<Double> posYArm = sgArm.add(new DoubleSetting.Builder().name("arm-position-y").description("The Y position offset of your arm.").defaultValue(0).sliderRange(-3, 3).build());
    private final Setting<Double> posZArm = sgArm.add(new DoubleSetting.Builder().name("arm-position-z").description("The Z position offset of your arm.").defaultValue(0).sliderRange(-3, 3).build());

    // Rotation
    private final Setting<Double> rotationXMain = sgMainHand.add(new DoubleSetting.Builder().name("mainhand-rotation-x").description("The X orientation of your main hand.").defaultValue(0).sliderRange(-180, 180).build());
    private final Setting<Double> rotationYMain = sgMainHand.add(new DoubleSetting.Builder().name("mainhand-rotation-y").description("The Y orientation of your main hand.").defaultValue(0).sliderRange(-180, 180).build());
    private final Setting<Double> rotationZMain = sgMainHand.add(new DoubleSetting.Builder().name("mainhand-rotation-z").description("The Z orientation of your main hand.").defaultValue(0).sliderRange(-180, 180).build());
    private final Setting<Double> rotationXOff = sgOffHand.add(new DoubleSetting.Builder().name("offhand-rotation-x").description("The X orientation of your offhand.").defaultValue(0).sliderRange(-180, 180).build());
    private final Setting<Double> rotationYOff = sgOffHand.add(new DoubleSetting.Builder().name("offhand-rotation-y").description("The Y orientation of your offhand.").defaultValue(0).sliderRange(-180, 180).build());
    private final Setting<Double> rotationZOff = sgOffHand.add(new DoubleSetting.Builder().name("offhand-rotation-z").description("The Z orientation of your offhand.").defaultValue(0).sliderRange(-180, 180).build());
    private final Setting<Double> rotationXArm = sgArm.add(new DoubleSetting.Builder().name("arm-rotation-x").description("The X orientation of your arm.").defaultValue(0).sliderRange(-180, 180).build());
    private final Setting<Double> rotationYArm = sgArm.add(new DoubleSetting.Builder().name("arm-rotation-y").description("The Y orientation of your arm.").defaultValue(0).sliderRange(-180, 180).build());
    private final Setting<Double> rotationZArm = sgArm.add(new DoubleSetting.Builder().name("arm-rotation-z").description("The Z orientation of your arm.").defaultValue(0).sliderRange(-180, 180).build());

    // Swing
    public final Setting<SwingMode> swingMode = sgSwing.add(new EnumSetting.Builder<SwingMode>().name("mode").description("Modifies your client & server hand swinging.").defaultValue(SwingMode.None).build());
    public final Setting<Double> mainSwing = sgSwing.add(new DoubleSetting.Builder().name("main-progress").description("The swing progress of your main hand.").defaultValue(0).range(0, 1).sliderMax(1).build());
    public final Setting<Double> offSwing = sgSwing.add(new DoubleSetting.Builder().name("offhand-progress").description("The swing progress of your offhand.").defaultValue(0).range(0, 1).sliderMax(1).build());
    public final Setting<Integer> swingSpeed = sgSwing.add(new IntSetting.Builder().name("swing-speed").description("The swing speed of your hands. (higher = slower swing)").defaultValue(6).range(0, 20).sliderMax(20).build());


    public HandView() {
        super(Categories.Render, "hand-view", "Alters the way items are rendered in your hands.");
    }

    @EventHandler
    private void onHeldItemRender(HeldItemRendererEvent event) {
        if (!isActive()) return;
        if (event.hand == Hand.MAIN_HAND) {
            event.matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotationXMain.get().floatValue()));
            event.matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationYMain.get().floatValue()));
            event.matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationZMain.get().floatValue()));
            event.matrix.scale(scaleXMain.get().floatValue(), scaleYMain.get().floatValue(), scaleZMain.get().floatValue());
            event.matrix.translate(posXMain.get().floatValue(), posYMain.get().floatValue(), posZMain.get().floatValue());
        } else {
            event.matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotationXOff.get().floatValue()));
            event.matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationYOff.get().floatValue()));
            event.matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationZOff.get().floatValue()));
            event.matrix.scale(scaleXOff.get().floatValue(), scaleYOff.get().floatValue(), scaleZOff.get().floatValue());
            event.matrix.translate(posXOff.get().floatValue(), posYOff.get().floatValue(), posZOff.get().floatValue());
        }
        if (Rotations.rotating && followRotations.get()) {
            applyServerRotations(event.matrix);
        }
    }

    @EventHandler
    private void onRenderArm(ArmRenderEvent event) {
        if (!isActive()) return;

        event.matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotationXArm.get().floatValue()));
        event.matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationYArm.get().floatValue()));
        event.matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationZArm.get().floatValue()));
        event.matrix.scale(scaleXArm.get().floatValue(), scaleYArm.get().floatValue(), scaleZArm.get().floatValue());
        event.matrix.translate(posXArm.get().floatValue(), posYArm.get().floatValue(), posZArm.get().floatValue());
    }

    private void applyServerRotations(MatrixStack matrix) {
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.player.getPitch() - Rotations.serverPitch));
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(mc.player.getYaw() - Rotations.serverYaw));
    }

    public enum SwingMode {
        Offhand,
        Mainhand,
        None
    }
}
