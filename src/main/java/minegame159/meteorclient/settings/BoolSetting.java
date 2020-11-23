/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.widgets.WCheckbox;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

public class BoolSetting extends Setting<Boolean> {
    private BoolSetting(String name, String description, Boolean defaultValue, Consumer<Boolean> onChanged, Consumer<Setting<Boolean>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        widget = new WCheckbox(get());
        ((WCheckbox) widget).action = () -> set(((WCheckbox) widget).checked);
    }

    @Override
    protected Boolean parseImpl(String str) {
        if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("1")) return true;
        else if (str.equalsIgnoreCase("false") || str.equalsIgnoreCase("0")) return false;
        else if (str.equalsIgnoreCase("toggle")) return !get();
        return null;
    }

    @Override
    public void resetWidget() {
        ((WCheckbox) widget).checked = get();
    }

    @Override
    protected boolean isValueValid(Boolean value) {
        return true;
    }

    @Override
    protected String generateUsage() {
        return "(highlight)true(default), (highlight)false (default)or (highlight)toggle";
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();
        tag.putBoolean("value", get());
        return tag;
    }

    @Override
    public Boolean fromTag(CompoundTag tag) {
        set(tag.getBoolean("value"));

        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private Boolean defaultValue;
        private Consumer<Boolean> onChanged;
        private Consumer<Setting<Boolean>> onModuleActivated;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(boolean defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<Boolean> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<Boolean>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public BoolSetting build() {
            return new BoolSetting(name, description, defaultValue, onChanged, onModuleActivated);
        }
    }
}
