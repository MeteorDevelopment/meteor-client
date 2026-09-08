/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

public class LongSetting extends Setting<Long> {
    private LongSetting(String name, String description, long defaultValue, Consumer<Long> onChanged, Consumer<Setting<Long>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    protected Long parseImpl(String str) {
        try {
            return Long.parseLong(str.trim());
        } catch (NumberFormatException _) {
            return null;
        }
    }

    @Override
    protected boolean isValueValid(Long value) {
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("value", get());

        return tag;
    }

    @Override
    public Long load(CompoundTag tag) {
        set(tag.getLongOr("value", 0));

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Long, LongSetting> {
        public Builder() {
            super(0L);
        }

        @Override
        public LongSetting build() {
            return new LongSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
