package minegame159.meteorclient.settings.builders;

import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.StringConverter;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class FloatSettingBuilder extends SettingBuilder<Float> {
    private Float min, max;

    @Override
    public FloatSettingBuilder name(String name) {
        return (FloatSettingBuilder) super.name(name);
    }

    @Override
    public FloatSettingBuilder description(String description) {
        return (FloatSettingBuilder) super.description(description);
    }

    @Override
    public FloatSettingBuilder usage(String usage) {
        return (FloatSettingBuilder) super.usage(usage);
    }

    @Override
    public FloatSettingBuilder defaultValue(Float defaultValue) {
        return (FloatSettingBuilder) super.defaultValue(defaultValue);
    }

    @Override
    public FloatSettingBuilder restriction(Predicate<Float> restriction) {
        return (FloatSettingBuilder) super.restriction(restriction);
    }

    @Override
    public FloatSettingBuilder consumer(BiConsumer<Float, Float> consumer) {
        return (FloatSettingBuilder) super.consumer(consumer);
    }

    @Override
    public FloatSettingBuilder converter(StringConverter<Float> converter) {
        return (FloatSettingBuilder) super.converter(converter);
    }

    public FloatSettingBuilder min(Float min) {
        this.min = min;
        return this;
    }

    public FloatSettingBuilder max(Float max) {
        this.max = max;
        return this;
    }

    @Override
    public Setting<Float> build() {
        if (usage == null) {
            if (min == null && max == null) usage = "number";
            else if (min != null && max == null) usage = min + "-inf";
            else if (min == null && max != null) usage = "inf-" + max;
            else if (min != null && max != null) usage = min + "-" + max;
        }

        if (converter == null) {
            converter = string -> {
                try {
                    return (float) Double.parseDouble(string);
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
