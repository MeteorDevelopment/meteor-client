/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.WorldOptions;

import java.util.OptionalLong;
import java.util.function.Consumer;

public class SeedSetting extends Setting<Long> {

    private SeedSetting(String name, String description, Long defaultValue, Consumer<Long> onChanged, Consumer<Setting<Long>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    protected Long parseImpl(String str) {
        OptionalLong optionalLong = WorldOptions.parseSeed(str);
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
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("seed", get());

        return tag;
    }

    @Override
    public Long load(CompoundTag tag) {
        set(tag.getLongOr("seed", 0));

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
