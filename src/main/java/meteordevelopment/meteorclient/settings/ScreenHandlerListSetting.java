/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ScreenHandlerListSetting extends Setting<List<ScreenHandlerType<?>>> {
    public ScreenHandlerListSetting(String name, String description, List<ScreenHandlerType<?>> defaultValue, Consumer<List<ScreenHandlerType<?>>> onChanged, Consumer<Setting<List<ScreenHandlerType<?>>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    protected List<ScreenHandlerType<?>> parseImpl(String str) {
        String[] values = str.split(",");
        List<ScreenHandlerType<?>> handlers = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                ScreenHandlerType<?> handler = parseId(Registries.SCREEN_HANDLER, value);
                if (handler != null) handlers.add(handler);
            }
        } catch (Exception ignored) {
        }

        return handlers;
    }

    @Override
    protected boolean isValueValid(List<ScreenHandlerType<?>> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registries.SCREEN_HANDLER.getIds();
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (ScreenHandlerType<?> type : get()) {
            Identifier id = Registries.SCREEN_HANDLER.getId(type);
            if (id != null) valueTag.add(NbtString.of(id.toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<ScreenHandlerType<?>> load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", 8);
        for (NbtElement tagI : valueTag) {
            ScreenHandlerType<?> type = Registries.SCREEN_HANDLER.get(Identifier.of(tagI.asString()));
            if (type != null) get().add(type);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<ScreenHandlerType<?>>, ScreenHandlerListSetting> {
        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(ScreenHandlerType<?>... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        @Override
        public ScreenHandlerListSetting build() {
            return new ScreenHandlerListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
