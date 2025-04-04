/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.settings;

import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.events.motor.KeyEvent;
import motordevelopment.motorclient.events.motor.MouseButtonEvent;
import motordevelopment.motorclient.gui.widgets.WKeybind;
import motordevelopment.motorclient.utils.misc.Keybind;
import motordevelopment.motorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.nbt.NbtCompound;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class KeybindSetting extends Setting<Keybind> {
    private final Runnable action;
    public WKeybind widget;

    public KeybindSetting(String name, String description, Keybind defaultValue, Consumer<Keybind> onChanged, Consumer<Setting<Keybind>> onModuleActivated, IVisible visible, Runnable action) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.action = action;
        MotorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onKeyBinding(KeyEvent event) {
        if (widget == null) return;
        if (event.action == KeyAction.Press && event.key == GLFW.GLFW_KEY_ESCAPE && widget.onClear()) event.cancel();
        else if (event.action == KeyAction.Release && widget.onAction(true, event.key, event.modifiers)) event.cancel();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onMouseButtonBinding(MouseButtonEvent event) {
        if (event.action == KeyAction.Press && widget != null && widget.onAction(false, event.button, 0)) event.cancel();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onKey(KeyEvent event) {
        if (event.action == KeyAction.Release && get().matches(true, event.key, event.modifiers) && (module == null || module.isActive()) && action != null) {
            action.run();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action == KeyAction.Release && get().matches(false, event.button, 0) && (module == null || module.isActive()) && action != null) {
            action.run();
        }
    }

    @Override
    public void resetImpl() {
        if (value == null) value = defaultValue.copy();
        else value.set(defaultValue);

        if (widget != null) widget.reset();
    }

    @Override
    protected Keybind parseImpl(String str) {
        try {
            return Keybind.fromKey(Integer.parseInt(str.trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    protected boolean isValueValid(Keybind value) {
        return true;
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        tag.put("value", get().toTag());

        return tag;
    }

    @Override
    public Keybind load(NbtCompound tag) {
        get().fromTag(tag.getCompound("value"));

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Keybind, KeybindSetting> {
        private Runnable action;

        public Builder() {
            super(Keybind.none());
        }

        public Builder action(Runnable action) {
            this.action = action;
            return this;
        }

        @Override
        public KeybindSetting build() {
            return new KeybindSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, action);
        }
    }
}
