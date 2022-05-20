/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.*;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CustomStatListSetting extends Setting<List<Identifier>> {

    public CustomStatListSetting(String name, String description, List<Identifier> defaultValue, Consumer<List<Identifier>> onChanged, Consumer<Setting<List<Identifier>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    protected List<Identifier> parseImpl(String str) {
        String[] values = str.split(",");
        List<Identifier> list = new ArrayList<>(values.length);
        for (String value : values) {
            Identifier stat = parseId(Stats.CUSTOM.getRegistry(), value);
            list.add(stat);
        }
        return list;
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    protected boolean isValueValid(List<Identifier> value) {
        return true;
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Stats.CUSTOM.getRegistry().getIds();
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        NbtList l = new NbtList();
        for (Identifier i : value) {
            l.add(NbtString.of(i.toString()));
        }
        tag.put("value", l);
        return tag;
    }

    @Override
    protected List<Identifier> load(NbtCompound tag) {
        value.clear();
        NbtList l = tag.getList("value", NbtElement.STRING_TYPE);
        for (NbtElement e : l) {
            value.add(new Identifier(e.asString()));
        }
        return value;
    }

    public static class Builder extends SettingBuilder<Builder, List<Identifier>, CustomStatListSetting> {
        public Builder() {
            super(null);
        }

        @Override
        public CustomStatListSetting build() {
            return new CustomStatListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
