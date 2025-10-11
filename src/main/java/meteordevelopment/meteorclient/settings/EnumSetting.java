/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import net.minecraft.nbt.NbtCompound;

import java.util.Map;
import java.util.function.Consumer;

public class EnumSetting<T extends Enum<?>> extends Setting<T> {
    private final Map<String, T> nameToValueMap = new Object2ObjectRBTreeMap<>(String.CASE_INSENSITIVE_ORDER);

    @SuppressWarnings("unchecked")
    public EnumSetting(String name, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        for (Object t : defaultValue.getDeclaringClass().getEnumConstants()) {
            nameToValueMap.put(t.toString(), (T) t);
        }
    }

    @Override
    protected T parseImpl(String str) {
        return nameToValueMap.get(str);
    }

    @Override
    protected boolean isValueValid(T value) {
        return true;
    }

    @Override
    public Iterable<String> getSuggestions() {
        return nameToValueMap.keySet();
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        tag.putString("value", get().toString());

        return tag;
    }

    @Override
    public T load(NbtCompound tag) {
        parse(tag.getString("value", ""));

        return get();
    }

    public static class Builder<T extends Enum<?>> extends SettingBuilder<Builder<T>, T, EnumSetting<T>> {
        public Builder() {
            super(null);
        }

        @Override
        public EnumSetting<T> build() {
            return new EnumSetting<>(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
