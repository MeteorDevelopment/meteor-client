/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ScreenHandlerListSetting extends Setting<List<MenuType<?>>> {
    public ScreenHandlerListSetting(String name, String description, List<MenuType<?>> defaultValue, Consumer<List<MenuType<?>>> onChanged, Consumer<Setting<List<MenuType<?>>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    protected List<MenuType<?>> parseImpl(String str) {
        String[] values = str.split(",");
        List<MenuType<?>> handlers = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                MenuType<?> handler = parseId(BuiltInRegistries.MENU, value);
                if (handler != null) handlers.add(handler);
            }
        } catch (Exception ignored) {
        }

        return handlers;
    }

    @Override
    protected boolean isValueValid(List<MenuType<?>> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return BuiltInRegistries.MENU.keySet();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag valueTag = new ListTag();
        for (MenuType<?> type : get()) {
            Identifier id = BuiltInRegistries.MENU.getKey(type);
            if (id != null) valueTag.add(StringTag.valueOf(id.toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<MenuType<?>> load(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getListOrEmpty("value");
        for (Tag tagI : valueTag) {
            MenuType<?> type = BuiltInRegistries.MENU.getValue(Identifier.parse(tagI.asString().orElse("")));
            if (type != null) get().add(type);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<MenuType<?>>, ScreenHandlerListSetting> {
        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(MenuType<?>... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        @Override
        public ScreenHandlerListSetting build() {
            return new ScreenHandlerListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
