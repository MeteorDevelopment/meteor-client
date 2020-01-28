package minegame159.meteorclient.settings.builders;

import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.StringConverter;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class DoubleSettingBuilder extends SettingBuilder<Double> {
    private Double min, max;

    @Override
    public DoubleSettingBuilder name(String name) {
        return (DoubleSettingBuilder) super.name(name);
    }

    @Override
    public DoubleSettingBuilder description(String description) {
        return (DoubleSettingBuilder) super.description(description);
    }

    @Override
    public DoubleSettingBuilder usage(String usage) {
        return (DoubleSettingBuilder) super.usage(usage);
    }

    @Override
    public DoubleSettingBuilder defaultValue(Double defaultValue) {
        return (DoubleSettingBuilder) super.defaultValue(defaultValue);
    }

    @Override
    public DoubleSettingBuilder restriction(Predicate<Double> restriction) {
        return (DoubleSettingBuilder) super.restriction(restriction);
    }

    @Override
    public DoubleSettingBuilder consumer(BiConsumer<Double, Double> consumer) {
        return (DoubleSettingBuilder) super.consumer(consumer);
    }

    @Override
    public DoubleSettingBuilder converter(StringConverter<Double> converter) {
        return (DoubleSettingBuilder) super.converter(converter);
    }

    public DoubleSettingBuilder min(Double min) {
        this.min = min;
        return this;
    }

    public DoubleSettingBuilder max(Double max) {
        this.max = max;
        return this;
    }

    @Override
    public Setting<Double> build() {
        if (usage == null) {
            if (min == null && max == null) usage = "number";
            else if (min != null && max == null) usage = min + "-inf";
            else if (min == null && max != null) usage = "inf-" + max;
            else if (min != null && max != null) usage = min + "-" + max;
        }

        if (converter == null) {
            converter = string -> {
                try {
                    return Double.parseDouble(string);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            };
        }

        if (restriction == null) {
            restriction = value -> {
                if (min != null && value < min) return false;
                if (max != null && value > max) return false;
                return true;
            };
        }

        return super.build();
    }
}
