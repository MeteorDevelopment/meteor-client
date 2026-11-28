/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.GuiKeyEvents;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.mixin.CreativeModeInventoryScreenAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTabs;

import static com.mojang.blaze3d.platform.InputConstants.*;

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
            if (isActive() && !aBoolean) mc.options.keyJump.setDown(false);
        })
        .build()
    );

    public final Setting<Boolean> sneak = sgGeneral.add(new BoolSetting.Builder()
        .name("sneak")
        .description("Allows you to sneak while in GUIs.")
        .defaultValue(true)
        .onChanged(aBoolean -> {
            if (isActive() && !aBoolean) mc.options.keyShift.setDown(false);
        })
        .build()
    );

    public final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder()
        .name("sprint")
        .description("Allows you to sprint while in GUIs.")
        .defaultValue(true)
        .onChanged(aBoolean -> {
            if (isActive() && !aBoolean) mc.options.keySprint.setDown(false);
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

    private boolean wasSkipped = true;

    public GUIMove() {
        super(Categories.Movement, "gui-move", "Allows you to perform various actions while in GUIs.");
    }

    @Override
    public void onActivate() {
        wasSkipped = true;
    }

    @Override
    public void onDeactivate() {
        wasSkipped = true;
        unpress();
    }

    private void unpress() {
        if (mc == null || mc.options == null) return;

        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);

        if (jump.get()) mc.options.keyJump.setDown(false);
        if (sneak.get()) mc.options.keyShift.setDown(false);
        if (sprint.get()) mc.options.keySprint.setDown(false);
    }

    public boolean disableSpace() {
        return isActive() && jump.get() && mc.options.keyJump.isDefault() && !skip();
    }

    public boolean disableArrows() {
        return isActive() && arrowsRotate.get() && !skip();
    }

    @EventHandler
    private void onKey(KeyInputEvent event) {
        onInput(event.key(), event.action);
    }

    @EventHandler
    private void onButton(MouseClickEvent event) {
        onInput(event.button(), event.action);
    }

    private void onInput(int key, KeyAction action) {
        if (checkSkip()) return;

        pass(mc.options.keyUp, key, action);
        pass(mc.options.keyDown, key, action);
        pass(mc.options.keyLeft, key, action);
        pass(mc.options.keyRight, key, action);

        if (jump.get()) pass(mc.options.keyJump, key, action);
        if (sneak.get()) pass(mc.options.keyShift, key, action);
        if (sprint.get()) pass(mc.options.keySprint, key, action);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (checkSkip()) return;

        float rotationDelta = Math.min((float) (rotateSpeed.get() * event.frameTime * 20f), 100);

        Freecam freecam = Modules.get().get(Freecam.class);

        if (arrowsRotate.get()) {
            if (!freecam.isActive()) {
                float yaw = mc.player.getYRot();
                float pitch = mc.player.getXRot();

                if (Input.isKeyPressed(KEY_LEFT)) yaw -= rotationDelta;
                if (Input.isKeyPressed(KEY_RIGHT)) yaw += rotationDelta;
                if (Input.isKeyPressed(KEY_UP)) pitch -= rotationDelta;
                if (Input.isKeyPressed(KEY_DOWN)) pitch += rotationDelta;

                pitch = Mth.clamp(pitch, -90, 90);

                mc.player.setYRot(yaw);
                mc.player.setXRot(pitch);
            } else {
                double dy = 0, dx = 0;

                if (Input.isKeyPressed(KEY_LEFT)) dy = -rotationDelta;
                if (Input.isKeyPressed(KEY_RIGHT)) dy = rotationDelta;
                if (Input.isKeyPressed(KEY_UP)) dx = -rotationDelta;
                if (Input.isKeyPressed(KEY_DOWN)) dx = rotationDelta;

                freecam.changeLookDirection(dy, dx);
            }
        }
    }

    private void pass(KeyMapping bind, int key, KeyAction action) {
        if (Input.getKey(bind) != key) return;
        if (action == KeyAction.Press) bind.setDown(true);
        if (action == KeyAction.Release) bind.setDown(false);
    }

    private boolean checkSkip() {
        if (skip()) {
            if (!wasSkipped) {
                wasSkipped = true;
                if (mc != null && mc.gui != null && mc.gui.screen() != null) unpress();
            }
            return true;
        }

        wasSkipped = false;
        return false;
    }

    public boolean skip() {
        return mc == null || mc.gui == null || skip(mc.gui.screen());
    }

    public boolean skip(Screen screen) {
        if (screen == null
            || !GuiKeyEvents.canUseKeys
            || (screen instanceof CreativeModeInventoryScreen && CreativeModeInventoryScreenAccessor.meteor$getSelectedTab() == CreativeModeTabs.searchTab())
            || screen instanceof ChatScreen
            || screen instanceof AbstractSignEditScreen
            || screen instanceof AnvilScreen
            || screen instanceof AbstractCommandBlockEditScreen
            || screen instanceof StructureBlockEditScreen
            || (screen.getFocused() instanceof EditBox editBox && editBox.canConsumeInput())) return true;
        if (screens.get() == Screens.GUI) return !(screen instanceof WidgetScreen);
        if (screens.get() == Screens.Inventory) return !(screen instanceof AbstractContainerScreen<?>);
        return !(screen instanceof WidgetScreen || screen instanceof AbstractContainerScreen<?>);
    }
}
