/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.settings;

import motordevelopment.motorclient.gui.utils.IScreenFactory;
import motordevelopment.motorclient.utils.misc.ICopyable;
import motordevelopment.motorclient.utils.misc.ISerializable;
import net.minecraft.nbt.NbtCompound;

import java.util.function.Consumer;

public class GenericSetting<T extends ICopyable<T> & ISerializable<T> & IScreenFactory> extends Setting<T> {
    public GenericSetting(String name, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void resetImpl() {
        if (value == null) value = defaultValue.copy();
        value.set(defaultValue);
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
    public NbtCompound save(NbtCompound tag) {
        tag.put("value", get().toTag());

        return tag;
    }

    @Override
    public T load(NbtCompound tag) {
        get().fromTag(tag.getCompound("value"));

        return get();
    }

    public static class Builder<T extends ICopyable<T> & ISerializable<T> & IScreenFactory> extends SettingBuilder<Builder<T>, T, GenericSetting<T>> {
        public Builder() {
            super(null);
        }

        @Override
        public GenericSetting<T> build() {
            return new GenericSetting<>(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
