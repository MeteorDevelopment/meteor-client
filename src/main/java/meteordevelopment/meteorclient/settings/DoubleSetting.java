/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.NbtCompound;

import java.util.function.Consumer;

public class DoubleSetting extends Setting<Double> {
    public final Double min, max;
    public final Boolean noSlider;
    private final Double sliderMin, sliderMax;

    public final int decimalPlaces;
    public final boolean onSliderRelease;

    private DoubleSetting(String name, String description, Double defaultValue, Consumer<Double> onChanged, Consumer<Setting<Double>> onModuleActivated, IVisible visible, Double min, Double max, Double sliderMin, Double sliderMax, boolean onSliderRelease, int decimalPlaces, boolean noSlider) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
        this.min = min;
        this.max = max;
        this.sliderMin = sliderMin;
        this.sliderMax = sliderMax;
        this.decimalPlaces = decimalPlaces;
        this.onSliderRelease = onSliderRelease;
        this.noSlider = noSlider;
    }

    @Override
    protected Double parseImpl(String str) {
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    protected boolean isValueValid(Double value) {
        return (min == null || value >= min) && (max == null || value <= max);
    }

    public double getSliderMin() {
        return sliderMin != null ? sliderMin : 0;
    }

    public double getSliderMax() {
        return sliderMax != null ? sliderMax : 10;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = saveGeneral();
        tag.putDouble("value", get());
        return tag;
    }

    @Override
    public Double fromTag(NbtCompound tag) {
        set(tag.getDouble("value"));

        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private Double defaultValue;
        private Consumer<Double> onChanged;
        private Consumer<Setting<Double>> onModuleActivated;
        private IVisible visible;
        private Double min, max;
        private Double sliderMin, sliderMax;
        private boolean onSliderRelease;
        private int decimalPlaces = 3;
        private boolean noSlider = false;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(double defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<Double> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<Double>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public Builder visible(IVisible visible) {
            this.visible = visible;
            return this;
        }

        public Builder min(double min) {
            this.min = min;
            return this;
        }

        public Builder max(double max) {
            this.max = max;
            return this;
        }

        public Builder sliderMin(double min) {
            sliderMin = min;
            return this;
        }

        public Builder sliderMax(double max) {
            sliderMax = max;
            return this;
        }

        public Builder onSliderRelease() {
            onSliderRelease = true;
            return this;
        }

        public Builder decimalPlaces(int decimalPlaces) {
            this.decimalPlaces = decimalPlaces;
            return this;
        }

        public Builder noSlider(boolean b) {
            this.noSlider = b;
            return this;
        }

        public DoubleSetting build() {
            return new DoubleSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, min, max, sliderMin, sliderMax, onSliderRelease, decimalPlaces, noSlider);
        }
    }
}
