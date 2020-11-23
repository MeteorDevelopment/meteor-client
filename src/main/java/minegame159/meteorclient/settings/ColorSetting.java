/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.screens.settings.ColorSettingScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WQuad;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.utils.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

public class ColorSetting extends Setting<Color> {
    private final WQuad quad;

    public ColorSetting(String name, String description, Color defaultValue, Consumer<Color> onChanged, Consumer<Setting<Color>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        widget = new WTable();
        quad = widget.add(new WQuad(get())).getWidget();

        WButton button = widget.add(new WButton(WButton.ButtonRegion.Edit)).getWidget();
        button.action = () -> {
            ColorSettingScreen colorSettingScreen = new ColorSettingScreen(this);
            colorSettingScreen.action = () -> quad.color.set(get());
            MinecraftClient.getInstance().openScreen(colorSettingScreen);
        };
    }

    @Override
    protected Color parseImpl(String str) {
        try {
            String[] strs = str.split(" ");
            return new Color(Integer.parseInt(strs[0]), Integer.parseInt(strs[1]), Integer.parseInt(strs[2]), Integer.parseInt(strs[3]));
        } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public void reset(boolean callbacks) {
        value = new Color(defaultValue);
        if (callbacks) {
            resetWidget();
            changed();
        }
    }

    @Override
    public void resetWidget() {
        quad.color.set(get());
    }

    @Override
    protected boolean isValueValid(Color value) {
        value.validate();
        return true;
    }

    @Override
    protected String generateUsage() {
        return "(highlight)0-255 0-255 0-255 0-255";
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();
        tag.put("value", get().toTag());
        return tag;
    }

    @Override
    public Color fromTag(CompoundTag tag) {
        get().fromTag(tag.getCompound("value"));

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private Color defaultValue;
        private Consumer<Color> onChanged;
        private Consumer<Setting<Color>> onModuleActivated;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(Color defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<Color> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<Color>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public ColorSetting build() {
            return new ColorSetting(name, description, defaultValue, onChanged, onModuleActivated);
        }
    }
}
