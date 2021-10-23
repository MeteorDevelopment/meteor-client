/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.NbtCompound;

import java.util.function.Consumer;

public class IntSetting extends Setting<Integer> {
    public final Integer min, max;
    public final Boolean noSlider;
    private final Integer sliderMin, sliderMax;

    private IntSetting(String name, String description, Integer defaultValue, Consumer<Integer> onChanged, Consumer<Setting<Integer>> onModuleActivated, IVisible visible, Integer min, Integer max, Integer sliderMin, Integer sliderMax, Boolean noSlider) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
        this.min = min;
        this.max = max;
        this.sliderMin = sliderMin;
        this.sliderMax = sliderMax;
        this.noSlider = noSlider;
    }

    @Override
    protected Integer parseImpl(String str) {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    protected boolean isValueValid(Integer value) {
        return (min == null || value >= min) && (max == null || value <= max);
    }

    public int getSliderMin() {
        return sliderMin != null ? sliderMin : 0;
    }

    public int getSliderMax() {
        return sliderMax != null ? sliderMax : 10;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = saveGeneral();
        tag.putInt("value", get());
        return tag;
    }

    @Override
    public Integer fromTag(NbtCompound tag) {
        set(tag.getInt("value"));

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Integer, IntSetting> {
        private Integer min, max;
        private Integer sliderMin, sliderMax;
        private Boolean noSlider = false;

        public Builder() {
            super(0);
        }

        public Builder visible(IVisible visible) {
            this.visible = visible;
            return this;
        }

        public Builder min(int min) {
            this.min = min;
            return this;
        }

        public Builder max(int max) {
            this.max = max;
            return this;
        }

        public Builder sliderMin(int min) {
            sliderMin = min;
            return this;
        }

        public Builder sliderMax(int max) {
            sliderMax = max;
            return this;
        }

        public Builder noSlider() {
            noSlider = true;
            return this;
        }

        @Override
        public IntSetting build() {
            return new IntSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, min, max, sliderMin, sliderMax, noSlider);
        }
    }
}
