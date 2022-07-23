/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.game.ChangePerspectiveEvent;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.Perspective;

public class CameraTweaks extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScrolling = settings.createGroup("Scrolling");

    // General

    private final Setting<Boolean> clip = sgGeneral.add(new BoolSetting.Builder()
            .name("clip")
            .description("Allows the camera to clip through blocks.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> cameraDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("camera-distance")
            .description("The distance the third person camera is from the player.")
            .defaultValue(4)
            .min(0)
            .onChanged(value -> distance = value)
            .build()
    );

    // Scrolling

    private final Setting<Boolean> scrollingEnabled = sgScrolling.add(new BoolSetting.Builder()
        .name("scrolling-enabled")
        .description("Allows you to scroll to change camera distance.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> scrollSensitivity = sgScrolling.add(new DoubleSetting.Builder()
            .name("scroll-sensitivity")
            .description("Scroll sensitivity when changing the cameras distance. 0 to disable.")
            .visible(scrollingEnabled::get)
            .defaultValue(1)
            .min(0)
            .build()
    );

    private final Setting<Keybind> scrollKeybind = sgScrolling.add(new KeybindSetting.Builder()
        .name("scroll-keybind")
        .description("Makes it so a keybind needs to be pressed for scrolling to work.")
        .visible(scrollingEnabled::get)
        .defaultValue(Keybind.none())
        .build()
    );

    public double distance;

    public CameraTweaks() {
        super(Categories.Render, "camera-tweaks", "Allows modification of the third person camera.");
    }

    @Override
    public void onActivate() {
        distance = cameraDistance.get();
    }

    @EventHandler
    private void onPerspectiveChanged(ChangePerspectiveEvent event) {
        distance = cameraDistance.get();
    }

    @EventHandler
    private void onMouseScroll(MouseScrollEvent event) {
        if (mc.options.getPerspective() == Perspective.FIRST_PERSON || mc.currentScreen != null || !scrollingEnabled.get() || (scrollKeybind.get().isValid() && !scrollKeybind.get().isPressed())) return;

        if (scrollSensitivity.get() > 0) {
            distance -= event.value * 0.25 * (scrollSensitivity.get() * distance);

            event.cancel();
        }
    }

    public boolean clip() {
        return isActive() && clip.get();
    }

    public double getDistance() {
        return isActive() ? distance : 4;
    }
}
