/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EnumSetting<T extends Enum<?>> extends Setting<T> {
    private final T[] values;

    private final List<String> suggestions;

    public EnumSetting(String name, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        values = (T[]) defaultValue.getDeclaringClass().getEnumConstants();
        suggestions = new ArrayList<>(values.length);
        for (T value : values) suggestions.add(value.toString());
    }

    @Override
    public void buildCommandNode(LiteralArgumentBuilder<CommandSource> builder, Consumer<String> output) {} // todo

    protected T parseImpl(String str) {
        for (T possibleValue : values) {
            if (str.equalsIgnoreCase(possibleValue.toString())) return possibleValue;
        }

        return null;
    }

    @Override
    public List<String> getSuggestions() {
        return suggestions;
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        tag.putString("value", get().toString());

        return tag;
    }

    @Override
    public T load(NbtCompound tag) {
        return parseImpl(tag.getString("value")); // todo
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
