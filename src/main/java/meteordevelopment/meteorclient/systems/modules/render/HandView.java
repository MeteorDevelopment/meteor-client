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
import org.joml.Vector3d;

public class HandView extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMainHand = settings.createGroup("Main Hand");
    private final SettingGroup sgOffHand = settings.createGroup("Off Hand");
    private final SettingGroup sgArm = settings.createGroup("Arm");

    // General

    private final Setting<Boolean> followRotations = sgGeneral.add(new BoolSetting.Builder()
        .name("server-rotations")
        .description("Makes your hands follow your serverside rotations.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> oldAnimations = sgGeneral.add(new BoolSetting.Builder()
        .name("old-animations")
        .description("Changes hit animations to those like 1.8")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> showSwapping = sgGeneral.add(new BoolSetting.Builder()
        .name("show-swapping")
        .description("Whether or not to show the item swapping animation")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableFoodAnimation = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-eating-animation")
        .description("Disables the eating animation. Potentially desirable if it goes offscreen.")
        .defaultValue(false)
        .build()
    );

    public final Setting<SwingMode> swingMode = sgGeneral.add(new EnumSetting.Builder<SwingMode>()
        .name("swing-mode")
        .description("Modifies your client & server hand swinging.")
        .defaultValue(SwingMode.None)
        .build()
    );

    public final Setting<Integer> swingSpeed = sgGeneral.add(new IntSetting.Builder()
        .name("swing-speed")
        .description("The swing speed of your hands.")
        .defaultValue(6)
        .range(0, 20)
        .sliderMax(20)
        .build()
    );

    public final Setting<Double> mainSwing = sgGeneral.add(new DoubleSetting.Builder()
        .name("main-hand-progress")
        .description("The swing progress of your main hand.")
        .defaultValue(0)
        .range(0, 1)
        .sliderMax(1)
        .build()
    );

    public final Setting<Double> offSwing = sgGeneral.add(new DoubleSetting.Builder()
        .name("off-hand-progress")
        .description("The swing progress of your off hand.")
        .defaultValue(0)
        .range(0, 1)
        .sliderMax(1)
        .build()
    );

    // Main Hand

    private final Setting<Vector3d> scaleMain = sgMainHand.add(new Vector3dSetting.Builder()
        .name("scale")
        .description("The scale of your main hand.")
        .defaultValue(1, 1, 1)
        .sliderMax(5)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Vector3d> posMain = sgMainHand.add(new Vector3dSetting.Builder()
        .name("position")
        .description("The position of your main hand.")
        .defaultValue(0, 0, 0)
        .sliderRange(-3, 3)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Vector3d> rotMain = sgMainHand.add(new Vector3dSetting.Builder()
        .name("rotation")
        .description("The rotation of your main hand.")
        .defaultValue(0, 0, 0)
        .sliderRange(-180, 180)
        .decimalPlaces(0)
        .build()
    );

    // Offhand

    private final Setting<Vector3d> scaleOff = sgOffHand.add(new Vector3dSetting.Builder()
        .name("scale")
        .description("The scale of your off hand.")
        .defaultValue(1, 1, 1)
        .sliderMax(5)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Vector3d> posOff = sgOffHand.add(new Vector3dSetting.Builder()
        .name("position")
        .description("The position of your off hand.")
        .defaultValue(0, 0, 0)
        .sliderRange(-3, 3)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Vector3d> rotOff = sgOffHand.add(new Vector3dSetting.Builder()
        .name("rotation")
        .description("The rotation of your off hand.")
        .defaultValue(0, 0, 0)
        .sliderRange(-180, 180)
        .decimalPlaces(0)
        .build()
    );

    // Arm

    private final Setting<Vector3d> scaleArm = sgArm.add(new Vector3dSetting.Builder()
        .name("scale")
        .defaultValue(1, 1, 1)
        .sliderMax(5)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Vector3d> posArm = sgArm.add(new Vector3dSetting.Builder()
        .name("position")
        .defaultValue(0, 0, 0)
        .sliderRange(-3, 3)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Vector3d> rotArm = sgArm.add(new Vector3dSetting.Builder()
        .name("rotation")
        .defaultValue(0, 0, 0)
        .sliderRange(-180, 180)
        .decimalPlaces(0)
        .build()
    );

    public HandView() {
        super(Categories.Render, "hand-view", "Alters the way items are rendered in your hands.");
    }

    @EventHandler
    private void onHeldItemRender(HeldItemRendererEvent event) {
        if (Rotations.rotating && followRotations.get()) {
            applyServerRotations(event.matrix);
        }

        if (event.hand == Hand.MAIN_HAND) {
            rotate(event.matrix, rotMain.get());
            scale(event.matrix, scaleMain.get());
            translate(event.matrix, posMain.get());
        }
        else {
            rotate(event.matrix, rotOff.get());
            scale(event.matrix, scaleOff.get());
            translate(event.matrix, posOff.get());
        }
    }

    @EventHandler
    private void onRenderArm(ArmRenderEvent event) {
        rotate(event.matrix, rotArm.get());
        scale(event.matrix, scaleArm.get());
        translate(event.matrix, posArm.get());
    }

    private void rotate(MatrixStack matrix, Vector3d rotation) {
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rotation.x));
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) rotation.y));
        matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rotation.z));
    }

    private void scale(MatrixStack matrix, Vector3d scale) {
        matrix.scale((float) scale.x, (float) scale.y, (float) scale.z);
    }

    private void translate(MatrixStack matrix, Vector3d translation) {
        matrix.translate((float) translation.x, (float) translation.y, (float) translation.z);
    }

    private void applyServerRotations(MatrixStack matrix) {
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.player.getPitch() - Rotations.serverPitch));
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(mc.player.getYaw() - Rotations.serverYaw));
    }

    public boolean oldAnimations() {
        return isActive() && oldAnimations.get();
    }

    public boolean showSwapping() {
        return isActive() && showSwapping.get();
    }

    public boolean disableFoodAnimation() {
        return isActive() && disableFoodAnimation.get();
    }

    public enum SwingMode {
        Offhand,
        Mainhand,
        None
    }
}
