/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class Setting<T> implements ISerializable<T> {
    public final String name, title, description;
    private String usage;

    protected final T defaultValue;
    protected T value;

    private final Consumer<T> onChanged;
    public final Consumer<Setting<T>> onModuleActivated;
    public WWidget widget;

    public Module module;

    public Setting(String name, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated) {
        this.name = name;
        this.title = Arrays.stream(name.split("-")).map(StringUtils::capitalize).collect(Collectors.joining(" "));
        this.description = description;
        this.defaultValue = defaultValue;
        reset(false);
        this.onChanged = onChanged;
        this.onModuleActivated = onModuleActivated;
    }

    public T get() {
        return value;
    }

    public boolean set(T value) {
        if (!isValueValid(value)) return false;
        this.value = value;
        resetWidget();
        changed();
        return true;
    }

    public void reset(boolean callbacks) {
        value = defaultValue;
        if (callbacks) {
            resetWidget();
            changed();
        }
    }
    public void reset() {
        reset(true);
    }

    public boolean parse(String str) {
        T newValue = parseImpl(str);

        if (newValue != null) {
            if (isValueValid(newValue)) {
                value = newValue;
                resetWidget();
                changed();
            }
        }

        return newValue != null;
    }

    public void changed() {
        if (onChanged != null) onChanged.accept(value);
    }

    public void onActivated() {
        if (onModuleActivated != null) onModuleActivated.accept(this);
    }

    protected abstract T parseImpl(String str);

    public abstract void resetWidget();

    protected abstract boolean isValueValid(T value);

    public String getUsage() {
        if (usage == null) usage = generateUsage();
        return usage;
    }

    protected abstract String generateUsage();

    protected CompoundTag saveGeneral() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        return tag;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Setting<?> setting = (Setting<?>) o;
        return Objects.equals(name, setting.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
