/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Objects;
import java.util.function.Consumer;

public class CustomStatSetting extends Setting<Stat<Identifier>> {


    public CustomStatSetting(String name, String description, Stat<Identifier> defaultValue, Consumer<Stat<Identifier>> onChanged, Consumer<Setting<Stat<Identifier>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    protected Stat<Identifier> parseImpl(String str) {
        return Stats.CUSTOM.getOrCreateStat(parseId(Stats.CUSTOM.getRegistry(), str));
    }

    @Override
    protected boolean isValueValid(Stat<Identifier> value) {
        return Stats.CUSTOM.getRegistry().containsId(value.getValue());
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Stats.CUSTOM.getRegistry().getIds();
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        tag.putString("value", Objects.requireNonNull(Stats.CUSTOM.getRegistry().getId(get().getValue())).toString());
        return tag;
    }

    @Override
    protected Stat<Identifier> load(NbtCompound tag) {
        value = Stats.CUSTOM.getOrCreateStat(new Identifier(tag.getString("value")));
        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Stat<Identifier>, CustomStatSetting> {
        public Builder() {
            super(null);
        }

        @Override
        public CustomStatSetting build() {
            return new CustomStatSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
