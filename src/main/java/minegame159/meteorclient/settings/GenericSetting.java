/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.utils.IScreenFactory;
import minegame159.meteorclient.utils.misc.ICopyable;
import minegame159.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

public class GenericSetting<T extends ICopyable<T> & ISerializable<T> & IScreenFactory> extends Setting<T> {
    public GenericSetting(String name, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);
    }

    @Override
    public void reset(boolean callbacks) {
        if (value == null) value = defaultValue.copy();
        value.set(defaultValue);

        if (callbacks) changed();
    }

    @Override
    protected T parseImpl(String str) {
        return defaultValue.copy();
    }

    @Override
    protected boolean isValueValid(T value) {
        return true;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();

        tag.put("value", get().toTag());

        return tag;
    }

    @Override
    public T fromTag(CompoundTag tag) {
        get().fromTag(tag.getCompound("value"));

        return get();
    }

    public static class Builder<T extends ICopyable<T> & ISerializable<T> & IScreenFactory> {
        private String name = "undefined", description = "";
        private T defaultValue;
        private Consumer<T> onChanged;
        private Consumer<Setting<T>> onModuleActivated;

        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T> description(String description) {
            this.description = description;
            return this;
        }

        public Builder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder<T> onChanged(Consumer<T> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder<T> onModuleActivated(Consumer<Setting<T>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public GenericSetting<T> build() {
            return new GenericSetting<>(name, description, defaultValue, onChanged, onModuleActivated);
        }
    }
}
