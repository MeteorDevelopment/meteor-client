package minegame159.meteorclient.settings.builders;

import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.StringConverter;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class ColorSettingBuilder extends SettingBuilder<Color> {
    @Override
    public ColorSettingBuilder name(String name) {
        return (ColorSettingBuilder) super.name(name);
    }

    @Override
    public ColorSettingBuilder description(String description) {
        return (ColorSettingBuilder) super.description(description);
    }

    @Override
    public ColorSettingBuilder usage(String usage) {
        return (ColorSettingBuilder) super.usage(usage);
    }

    @Override
    public ColorSettingBuilder defaultValue(Color defaultValue) {
        return (ColorSettingBuilder) super.defaultValue(defaultValue);
    }

    @Override
    public ColorSettingBuilder restriction(Predicate<Color> restriction) {
        return (ColorSettingBuilder) super.restriction(restriction);
    }

    @Override
    public ColorSettingBuilder consumer(BiConsumer<Color, Color> consumer) {
        return (ColorSettingBuilder) super.consumer(consumer);
    }

    @Override
    public ColorSettingBuilder converter(StringConverter<Color> converter) {
        return (ColorSettingBuilder) super.converter(converter);
    }

    @Override
    public Setting<Color> build() {
        if (usage == null) usage = "0-255 0-255 0-255 0-255";

        if (converter == null) {
            converter = string -> {
                try {
                    String[] a = string.split(" ");
                    return new Color(Integer.parseInt(a[0].trim()), Integer.parseInt(a[1].trim()), Integer.parseInt(a[2].trim()), Integer.parseInt(a[3].trim()));
                } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
                    return null;
                }
            };
        }

        return super.build();
    }
}
