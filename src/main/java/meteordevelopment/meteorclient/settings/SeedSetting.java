/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.gen.GeneratorOptions;

import java.util.OptionalLong;
import java.util.function.Consumer;

public class SeedSetting extends Setting<Long> {

    private SeedSetting(String name, String description, Long defaultValue, Consumer<Long> onChanged, Consumer<Setting<Long>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    protected Long parseImpl(String str) {
        OptionalLong optionalLong = GeneratorOptions.parseSeed(str);
        if (optionalLong.isPresent()) {
            return optionalLong.getAsLong();
        }
        return null;
    }

    @Override
    protected boolean isValueValid(Long value) {
        return value != null;
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        tag.putLong("seed", get());

        return tag;
    }

    @Override
    public Long load(NbtCompound tag) {
        set(tag.getLong("seed", 0));

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Long, SeedSetting> {

        public Builder() {
            super(null);
        }

        @Override
        public SeedSetting build() {
            return new SeedSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
