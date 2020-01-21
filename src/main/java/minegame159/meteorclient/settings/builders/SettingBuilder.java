package minegame159.meteorclient.settings.builders;

import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.StringConverter;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class SettingBuilder<T> {
    protected String name;
    protected String description;
    protected String usage;

    protected T defaultValue;

    protected Predicate<T> restriction;
    protected BiConsumer<T, T> consumer;
    protected StringConverter<T> converter;

    public SettingBuilder<T> name(String name) {
        this.name = name;
        return this;
    }

    public SettingBuilder<T> description(String description) {
        this.description = description;
        return this;
    }

    public SettingBuilder<T> usage(String usage) {
        this.usage = usage;
        return this;
    }

    public SettingBuilder<T> defaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public SettingBuilder<T> restriction(Predicate<T> restriction) {
        this.restriction = restriction;
        return this;
    }

    public SettingBuilder<T> consumer(BiConsumer<T, T> consumer) {
        this.consumer = consumer;
        return this;
    }

    public SettingBuilder<T> converter(StringConverter<T> converter) {
        this.converter = converter;
        return this;
    }

    public Setting<T> build() {
        return new Setting<>(name, description, usage, defaultValue, restriction, consumer, converter);
    }
}
