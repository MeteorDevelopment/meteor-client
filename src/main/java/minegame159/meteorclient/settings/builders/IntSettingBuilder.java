package minegame159.meteorclient.settings.builders;

import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.StringConverter;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class IntSettingBuilder extends SettingBuilder<Integer> {
    private Integer min, max;

    @Override
    public IntSettingBuilder name(String name) {
        return (IntSettingBuilder) super.name(name);
    }

    @Override
    public IntSettingBuilder description(String description) {
        return (IntSettingBuilder) super.description(description);
    }

    @Override
    public IntSettingBuilder usage(String usage) {
        return (IntSettingBuilder) super.usage(usage);
    }

    @Override
    public IntSettingBuilder defaultValue(Integer defaultValue) {
        return (IntSettingBuilder) super.defaultValue(defaultValue);
    }

    @Override
    public IntSettingBuilder restriction(Predicate<Integer> restriction) {
        return (IntSettingBuilder) super.restriction(restriction);
    }

    @Override
    public IntSettingBuilder consumer(BiConsumer<Integer, Integer> consumer) {
        return (IntSettingBuilder) super.consumer(consumer);
    }

    @Override
    public IntSettingBuilder converter(StringConverter<Integer> converter) {
        return (IntSettingBuilder) super.converter(converter);
    }

    public IntSettingBuilder min(Integer min) {
        this.min = min;
        return this;
    }

    public IntSettingBuilder max(Integer max) {
        this.max = max;
        return this;
    }

    @Override
    public Setting<Integer> build() {
        if (usage == null) {
            if (min == null && max == null) usage = "number";
            else if (min != null && max == null) usage = min + "-inf";
            else if (min == null && max != null) usage = "inf-" + max;
            else if (min != null && max != null) usage = min + "-" + max;
        }

        if (converter == null) {
            converter = string -> {
                try {
                    return (int) Math.round(Double.parseDouble(string));
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
