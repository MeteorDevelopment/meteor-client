/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.input.Input;
import net.minecraft.client.options.Perspective;
import org.lwjgl.glfw.GLFW;

public class FreeLook extends Module {
    public enum Mode {
        Player,
        Camera
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgArrows = settings.createGroup("Arrows");

    // General

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Which entity to rotate.")
            .defaultValue(Mode.Player)
            .build()
    );

    public final Setting<Boolean> togglePerpective = sgGeneral.add(new BoolSetting.Builder()
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

    private Perspective prePers;

    public FreeLook() {
        super(Categories.Render, "free-look", "Allows more rotation options in third person.");
    }

    @Override
    public void onActivate() {
        cameraYaw = mc.player.yaw;
        cameraPitch = mc.player.pitch;
        prePers = mc.options.getPerspective();

        if (prePers != Perspective.THIRD_PERSON_BACK &&  togglePerpective.get()) mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    @Override
    public void onDeactivate() {
        if (mc.options.getPerspective() != prePers && togglePerpective.get()) mc.options.setPerspective(prePers);
    }

    public boolean playerMode() {
        return isActive() && mc.options.getPerspective() == Perspective.THIRD_PERSON_BACK && mode.get() == Mode.Player;
    }

    public boolean cameraMode() {
        return isActive() && mc.options.getPerspective() == Perspective.THIRD_PERSON_BACK && mode.get() == Mode.Camera;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (arrows.get()) {
            for (int i = 0; i < (arrowSpeed.get() * 2); i++) {
                switch (mode.get()) {
                    case Player:
                        if (Input.isKeyPressed(GLFW.GLFW_KEY_LEFT)) cameraYaw -= 0.5;
                        if (Input.isKeyPressed(GLFW.GLFW_KEY_RIGHT)) cameraYaw += 0.5;
                        if (Input.isKeyPressed(GLFW.GLFW_KEY_UP)) cameraPitch -= 0.5;
                        if (Input.isKeyPressed(GLFW.GLFW_KEY_DOWN)) cameraPitch += 0.5;
                        break;
                    case Camera:
                        if (Input.isKeyPressed(GLFW.GLFW_KEY_LEFT)) mc.player.yaw -= 0.5;
                        if (Input.isKeyPressed(GLFW.GLFW_KEY_RIGHT)) mc.player.yaw += 0.5;
                        if (Input.isKeyPressed(GLFW.GLFW_KEY_UP)) mc.player.pitch -= 0.5;
                        if (Input.isKeyPressed(GLFW.GLFW_KEY_DOWN)) mc.player.pitch += 0.5;
                        break;
                }
            }
        }

        mc.player.pitch = Utils.clamp(mc.player.pitch, -90, 90);
        cameraPitch = Utils.clamp(cameraPitch, -90, 90);
    }
}