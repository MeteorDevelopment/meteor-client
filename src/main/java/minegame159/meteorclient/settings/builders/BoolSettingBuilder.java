package minegame159.meteorclient.settings.builders;

import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.StringConverter;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class BoolSettingBuilder extends SettingBuilder<Boolean> {
    @Override
    public BoolSettingBuilder name(String name) {
        return (BoolSettingBuilder) super.name(name);
    }

    @Override
    public BoolSettingBuilder description(String description) {
        return (BoolSettingBuilder) super.description(description);
    }

    @Override
    public BoolSettingBuilder usage(String usage) {
        return (BoolSettingBuilder) super.usage(usage);
    }

    @Override
    public BoolSettingBuilder defaultValue(Boolean defaultValue) {
        return (BoolSettingBuilder) super.defaultValue(defaultValue);
    }

    @Override
    public BoolSettingBuilder restriction(Predicate<Boolean> restriction) {
        return (BoolSettingBuilder) super.restriction(restriction);
    }

    @Override
    public BoolSettingBuilder consumer(BiConsumer<Boolean, Boolean> consumer) {
        return (BoolSettingBuilder) super.consumer(consumer);
    }

    @Override
    public BoolSettingBuilder converter(StringConverter<Boolean> converter) {
        return (BoolSettingBuilder) super.converter(converter);
    }

    @Override
    public Setting<Boolean> build() {
        if (usage == null) usage = "true or false";

        if (converter == null) {
            converter = string -> string.equalsIgnoreCase("true");
        }

        return super.build();
    }
}
