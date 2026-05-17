/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class FreeLook extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgArrows = settings.createGroup("Arrows");

    // General

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Which entity to rotate.")
        .defaultValue(Mode.Player)
        .build()
    );

    public final Setting<Boolean> togglePerspective = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-perspective")
        .description("Changes your perspective on toggle.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> sensitivity = sgGeneral.add(new DoubleSetting.Builder()
        .name("camera-sensitivity")
        .description("How fast the camera moves in camera mode.")
        .defaultValue(8)
        .min(0)
        .sliderMax(10)
        .build()
    );

    // Arrows

    public final Setting<Boolean> arrows = sgArrows.add(new BoolSetting.Builder()
        .name("arrows-control-opposite")
        .description("Allows you to control the other entities rotation with the arrow keys.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> arrowSpeed = sgArrows.add(new DoubleSetting.Builder()
        .name("arrow-speed")
        .description("Rotation speed with arrow keys.")
        .defaultValue(4)
        .min(0)
        .build()
    );

    public float cameraYaw;
    public float cameraPitch;

    private CameraType prePers;

    public FreeLook() {
        super(Categories.Render, "free-look", "Allows more rotation options in third person.");
    }

    @Override
    public void onActivate() {
        cameraYaw = mc.player.getYRot();
        cameraPitch = mc.player.getXRot();
        prePers = mc.options.getCameraType();

        if (prePers != CameraType.THIRD_PERSON_BACK && togglePerspective.get())
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    @Override
    public void onDeactivate() {
        if (mc.options.getCameraType() != prePers && togglePerspective.get()) mc.options.setCameraType(prePers);
    }

    public boolean playerMode() {
        return isActive() && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK && mode.get() == Mode.Player;
    }

    public boolean cameraMode() {
        return isActive() && mode.get() == Mode.Camera;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (arrows.get()) {
            for (int i = 0; i < (arrowSpeed.get() * 2); i++) {
                switch (mode.get()) {
                    case Player -> {
                        if (Input.isKeyPressed(GLFW.GLFW_KEY_LEFT)) cameraYaw -= 0.5;
                        if (Input.isKeyPressed(GLFW.GLFW_KEY_RIGHT)) cameraYaw += 0.5;
                        if (Input.isKeyPressed(GLFW.GLFW_KEY_UP)) cameraPitch -= 0.5;
                        if (Input.isKeyPressed(GLFW.GLFW_KEY_DOWN)) cameraPitch += 0.5;
                    }
                    case Camera -> {
                        float yaw = mc.player.getYRot();
                        float pitch = mc.player.getXRot();

                        if (Input.isKeyPressed(GLFW.GLFW_KEY_LEFT)) yaw -= 0.5;
                        if (Input.isKeyPressed(GLFW.GLFW_KEY_RIGHT)) yaw += 0.5;
                        if (Input.isKeyPressed(GLFW.GLFW_KEY_UP)) pitch -= 0.5;
                        if (Input.isKeyPressed(GLFW.GLFW_KEY_DOWN)) pitch += 0.5;

                        mc.player.setYRot(yaw);
                        mc.player.setXRot(pitch);
                    }
                }
            }
        }

        mc.player.setXRot(Mth.clamp(mc.player.getXRot(), -90, 90));
        cameraPitch = Mth.clamp(cameraPitch, -90, 90);
    }

    public enum Mode {
        Player,
        Camera
    }
}
