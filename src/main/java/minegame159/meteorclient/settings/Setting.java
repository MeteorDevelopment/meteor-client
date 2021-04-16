/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.misc.IGetter;
import minegame159.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class Setting<T> implements IGetter<T>, ISerializable<T> {
    private static final List<String> NO_SUGGESTIONS = new ArrayList<>(0);

    public final String name, title, description;

    protected final T defaultValue;
    protected T value;

    private final Consumer<T> onChanged;
    public final Consumer<Setting<T>> onModuleActivated;

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

    @Override
    public T get() {
        return value;
    }

    public boolean set(T value) {
        if (!isValueValid(value)) return false;
        this.value = value;
        changed();
        return true;
    }

    public void reset(boolean callbacks) {
        value = defaultValue;
        if (callbacks) changed();
    }
    public void reset() {
        reset(true);
    }

    public boolean parse(String str) {
        T newValue = parseImpl(str);

        if (newValue != null) {
            if (isValueValid(newValue)) {
                value = newValue;
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

    protected abstract boolean isValueValid(T value);

    public Iterable<Identifier> getIdentifierSuggestions() {
        return null;
    }

    public List<String> getSuggestions() {
        return NO_SUGGESTIONS;
    }

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

    public static <T> T parseId(Registry<T> registry, String name) {
        name = name.trim();

        Identifier id;
        if (name.contains(":")) id = new Identifier(name);
        else id = new Identifier("minecraft", name);
        if (registry.containsId(id)) return registry.get(id);

        return null;
    }
}
