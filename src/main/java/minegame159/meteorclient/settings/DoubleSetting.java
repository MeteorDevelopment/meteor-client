package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.widgets.WDoubleEdit;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

public class DoubleSetting extends Setting<Double> {
    private final Double min, max;

    private DoubleSetting(String name, String description, Double defaultValue, Consumer<Double> onChanged, Consumer<Setting<Double>> onModuleActivated, Double min, Double max, Double sliderMin, Double sliderMax, boolean noSlider) {
        super(name, description, defaultValue, onChanged, onModuleActivated);
        this.min = min;
        this.max = max;

        widget = new WDoubleEdit(get(), sliderMin != null ? sliderMin : 0, sliderMax != null ? sliderMax : 10, noSlider);
        ((WDoubleEdit) widget).action = () -> {
            if (!set(((WDoubleEdit) widget).get())) ((WDoubleEdit) widget).set(get());
        };
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
    public void resetWidget() {
        ((WDoubleEdit) widget).set(get());
    }

    @Override
    protected boolean isValueValid(Double value) {
        return (min == null || value >= min) && (max == null || value <= max);
    }

    @Override
    protected String generateUsage() {
        String usage = "(highlight)";

        if (min == null) usage += "inf";
        else usage += min;

        usage += " (default)- (highlight)";

        if (max == null) usage += "inf";
        else usage += max;

        return usage;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();
        tag.putDouble("value", get());
        return tag;
    }

    @Override
    public Double fromTag(CompoundTag tag) {
        set(tag.getDouble("value"));

        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private Double defaultValue;
        private Consumer<Double> onChanged;
        private Consumer<Setting<Double>> onModuleActivated;
        private Double min, max;
        private Double sliderMin, sliderMax;
        private boolean noSlider;

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

        public Builder noSlider() {
            noSlider = true;
            return this;
        }

        public DoubleSetting build() {
            return new DoubleSetting(name, description, defaultValue, onChanged, onModuleActivated, min, max, sliderMin, sliderMax, noSlider);
        }
    }
}
