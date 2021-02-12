/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.widgets.WDropbox;
import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

public class EnumSetting<T extends Enum<?>> extends Setting<T> {
    private T[] values;

    public EnumSetting(String name, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        try {
            values = (T[]) defaultValue.getClass().getMethod("values").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        widget = new WDropbox<>(get());
        ((WDropbox<T>) widget).action = () -> set(((WDropbox<T>) widget).getValue());
    }

    @Override
    protected T parseImpl(String str) {
        for (T possibleValue : values) {
            if (str.equalsIgnoreCase(possibleValue.toString())) return possibleValue;
        }

        return null;
    }

    @Override
    public void resetWidget() {
        ((WDropbox<T>) widget).setValue(get());
    }

    @Override
    protected boolean isValueValid(T value) {
        return true;
    }

    @Override
    protected String generateUsage() {
        String usage = "";

        for (int i = 0; i < values.length; i++) {
            if (i > 0) usage += " (default)or ";
            usage += "(highlight)" + values[i];
        }

        return usage;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();
        tag.putString("value", get().toString());
        return tag;
    }

    @Override
    public T fromTag(CompoundTag tag) {
        parse(tag.getString("value"));

        return get();
    }

    public static class Builder<T extends Enum<?>> {
        protected String name = "undefined", description = "";
        protected T defaultValue;
        protected Consumer<T> onChanged;
        protected Consumer<Setting<T>> onModuleActivated;

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

        public EnumSetting<T> build() {
            return new EnumSetting<>(name, description, defaultValue, onChanged, onModuleActivated);
        }
    }
}
