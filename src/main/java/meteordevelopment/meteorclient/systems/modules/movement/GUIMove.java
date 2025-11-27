/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.mixin.CreativeInventoryScreenAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemGroups;
import net.minecraft.util.math.MathHelper;

import static org.lwjgl.glfw.GLFW.*;

public class GUIMove extends Module {
    public enum Screens {
        GUI,
        Inventory,
        Both
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Screens> screens = sgGeneral.add(new EnumSetting.Builder<Screens>()
        .name("guis")
        .description("Which GUIs to move in.")
        .defaultValue(Screens.Inventory)
        .build()
    );

    public final Setting<Boolean> jump = sgGeneral.add(new BoolSetting.Builder()
        .name("jump")
        .description("Allows you to jump while in GUIs.")
        .defaultValue(true)
        .onChanged(aBoolean -> {
            if (isActive() && !aBoolean) mc.options.jumpKey.setPressed(false);
        })
        .build()
    );

    public final Setting<Boolean> sneak = sgGeneral.add(new BoolSetting.Builder()
        .name("sneak")
        .description("Allows you to sneak while in GUIs.")
        .defaultValue(true)
        .onChanged(aBoolean -> {
            if (isActive() && !aBoolean) mc.options.sneakKey.setPressed(false);
        })
        .build()
    );

    public final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder()
        .name("sprint")
        .description("Allows you to sprint while in GUIs.")
        .defaultValue(true)
        .onChanged(aBoolean -> {
            if (isActive() && !aBoolean) mc.options.sprintKey.setPressed(false);
        })
        .build()
    );

    private final Setting<Boolean> arrowsRotate = sgGeneral.add(new BoolSetting.Builder()
        .name("arrows-rotate")
        .description("Allows you to use your arrow keys to rotate while in GUIs.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> rotateSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("rotate-speed")
        .description("Rotation speed while in GUIs.")
        .defaultValue(4)
        .min(0)
        .build()
    );

    public GUIMove() {
        super(Categories.Movement, "gui-move", "Allows you to perform various actions while in GUIs.");
    }

    @Override
    public void onDeactivate() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);

        if (jump.get()) mc.options.jumpKey.setPressed(false);
        if (sneak.get()) mc.options.sneakKey.setPressed(false);
        if (sprint.get()) mc.options.sprintKey.setPressed(false);
    }

    public boolean disableSpace() {
        return isActive() && jump.get() && mc.options.jumpKey.isDefault();
    }
    public boolean disableArrows() {
        return isActive() && arrowsRotate.get();
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        onInput(event.key(), event.action);
    }

    @EventHandler
    private void onButton(MouseClickEvent event) {
        onInput(event.button(), event.action);
    }

    private void onInput(int key, KeyAction action) {
        if (skip()) return;

        pass(mc.options.forwardKey, key, action);
        pass(mc.options.backKey, key, action);
        pass(mc.options.leftKey, key, action);
        pass(mc.options.rightKey, key, action);

        if (jump.get()) pass(mc.options.jumpKey, key, action);
        if (sneak.get()) pass(mc.options.sneakKey, key, action);
        if (sprint.get()) pass(mc.options.sprintKey, key, action);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (skip()) return;

        float rotationDelta = Math.min((float) (rotateSpeed.get() * event.frameTime * 20f), 100);

        Freecam freecam = Modules.get().get(Freecam.class);

        if (arrowsRotate.get()) {
            if (!freecam.isActive()) {
                float yaw = mc.player.getYaw();
                float pitch = mc.player.getPitch();

                if (Input.isKeyPressed(GLFW_KEY_LEFT)) yaw -= rotationDelta;
                if (Input.isKeyPressed(GLFW_KEY_RIGHT)) yaw += rotationDelta;
                if (Input.isKeyPressed(GLFW_KEY_UP)) pitch -= rotationDelta;
                if (Input.isKeyPressed(GLFW_KEY_DOWN)) pitch += rotationDelta;

                pitch = MathHelper.clamp(pitch, -90, 90);

                mc.player.setYaw(yaw);
                mc.player.setPitch(pitch);
            } else {
                double dy = 0, dx = 0;

                if (Input.isKeyPressed(GLFW_KEY_LEFT)) dy = -rotationDelta;
                if (Input.isKeyPressed(GLFW_KEY_RIGHT)) dy = rotationDelta;
                if (Input.isKeyPressed(GLFW_KEY_UP)) dx = -rotationDelta;
                if (Input.isKeyPressed(GLFW_KEY_DOWN)) dx = rotationDelta;

                freecam.changeLookDirection(dy, dx);
            }
        }
    }

    private void pass(KeyBinding bind, int key, KeyAction action) {
        if (Input.getKey(bind) != key) return;
        if (action == KeyAction.Press) bind.setPressed(true);
        if (action == KeyAction.Release) bind.setPressed(false);
    }

    public boolean skip() {
        if (mc.currentScreen == null ||
            (mc.currentScreen instanceof CreativeInventoryScreen && CreativeInventoryScreenAccessor.meteor$getSelectedTab() == ItemGroups.getSearchGroup())
            || mc.currentScreen instanceof ChatScreen
            || mc.currentScreen instanceof SignEditScreen
            || mc.currentScreen instanceof AnvilScreen
            || mc.currentScreen instanceof AbstractCommandBlockScreen
            || mc.currentScreen instanceof StructureBlockScreen) return true;
        if (screens.get() == Screens.GUI && !(mc.currentScreen instanceof WidgetScreen)) return true;
        return screens.get() == Screens.Inventory && mc.currentScreen instanceof WidgetScreen;
    }
}
