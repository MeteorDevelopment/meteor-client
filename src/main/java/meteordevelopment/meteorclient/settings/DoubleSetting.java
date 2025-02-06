/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;

import java.util.function.Consumer;

public class DoubleSetting extends Setting<Double> {
    public final double min, max;
    public final double sliderMin, sliderMax;
    public final boolean onSliderRelease;
    public final int decimalPlaces;
    public final boolean noSlider;

    private DoubleSetting(String name, String description, double defaultValue, Consumer<Double> onChanged, Consumer<Setting<Double>> onModuleActivated, IVisible visible, double min, double max, double sliderMin, double sliderMax, boolean onSliderRelease, int decimalPlaces, boolean noSlider) {
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
    public void buildCommandNode(LiteralArgumentBuilder<CommandSource> builder, Consumer<String> output) {
        builder.then(Command.literal("set")
            .then(Command.argument("value", DoubleArgumentType.doubleArg(this.min, this.max))
                .executes(context -> {
                    if (this.set(DoubleArgumentType.getDouble(context, "value"))) {
                        String formatStr = "%." + this.decimalPlaces + "f";
                        output.accept(String.format("Set (highlight)%s(default) to (hightlight)" + formatStr + "(default).", this.title, this.get()));
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }

    @Override
    protected boolean isValueValid(Double value) {
        return value >= min && value <= max;
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        tag.putDouble("value", get());

        return tag;
    }

    @Override
    public Double load(NbtCompound tag) {
        set(tag.getDouble("value"));

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Double, DoubleSetting> {
        public double min = Double.NEGATIVE_INFINITY, max = Double.POSITIVE_INFINITY;
        public double sliderMin = 0, sliderMax = 10;
        public boolean onSliderRelease = false;
        public int decimalPlaces = 3;
        public boolean noSlider = false;

        public Builder() {
            super(0D);
        }

        public Builder defaultValue(double defaultValue) {
            this.defaultValue = defaultValue;
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

        public Builder range(double min, double max) {
            this.min = Math.min(min, max);
            this.max = Math.max(min, max);
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

        public Builder sliderRange(double min, double max) {
            sliderMin = min;
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

        public Builder noSlider() {
            noSlider = true;
            return this;
        }

        public DoubleSetting build() {
            return new DoubleSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, min, max, Math.max(sliderMin, min), Math.min(sliderMax, max), onSliderRelease, decimalPlaces, noSlider);
        }
    }
}
