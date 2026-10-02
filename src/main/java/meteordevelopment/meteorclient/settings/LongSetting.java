/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

public class LongSetting extends Setting<Long> {
    public final long min, max;

    private LongSetting(String name, String description, long defaultValue, Consumer<Long> onChanged, Consumer<Setting<Long>> onModuleActivated, IVisible visible, long min, long max) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.min = min;
        this.max = max;
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
        return value >= min && value <= max;
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
        private long min = Long.MIN_VALUE, max = Long.MAX_VALUE;

        public Builder() {
            super(0L);
        }

        public Builder min(long min) {
            this.min = min;
            return this;
        }

        public Builder max(long max) {
            this.max = max;
            return this;
        }

        public Builder range(long min, long max) {
            this.min = Math.min(min, max);
            this.max = Math.max(min, max);
            return this;
        }

        @Override
        public LongSetting build() {
            return new LongSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, min, max);
        }
    }
}
