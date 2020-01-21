package minegame159.meteorclient.settings;

import minegame159.meteorclient.utils.StringConverter;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class Setting<T> {
    public String name;
    public String description;
    public String usage;

    private T value;
    private T defaultValue;

    private Predicate<T> restriction;
    private BiConsumer<T, T> consumer;
    private StringConverter<T> converter;

    public Setting(String name, String description, String usage, T defaultValue, Predicate<T> restriction, BiConsumer<T, T> consumer, StringConverter<T> converter) {
        this.name = name;
        this.description = description;
        this.usage = usage;
        value = defaultValue;
        this.defaultValue = defaultValue;
        this.restriction = restriction;
        this.consumer = consumer;
        this.converter = converter;
    }

    public T value() {
        return value;
    }

    public boolean value(T value) {
        T old = this.value;
        if (restriction != null && !restriction.test(value)) return false;
        this.value = value;
        if (consumer != null) consumer.accept(old, value);
        return true;
    }

    public void reset() {
        value = defaultValue;
    }

    public boolean setFromString(String string) {
        T newValue = converter.convert(string);
        if (newValue == null) return false;
        value = newValue;
        return true;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
