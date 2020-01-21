package minegame159.meteorclient.settings.builders;

import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.StringConverter;

import java.lang.reflect.InvocationTargetException;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class EnumSettingBuilder<T extends Enum> extends SettingBuilder<T> {
    private Class<? extends Enum> clazz;

    @Override
    public EnumSettingBuilder<T> name(String name) {
        return (EnumSettingBuilder<T>) super.name(name);
    }

    @Override
    public EnumSettingBuilder<T> description(String description) {
        return (EnumSettingBuilder<T>) super.description(description);
    }

    @Override
    public EnumSettingBuilder<T> usage(String usage) {
        return (EnumSettingBuilder<T>) super.usage(usage);
    }

    @Override
    public EnumSettingBuilder<T> defaultValue(T defaultValue) {
        clazz = defaultValue.getClass();
        return (EnumSettingBuilder<T>) super.defaultValue(defaultValue);
    }

    @Override
    public EnumSettingBuilder<T> restriction(Predicate<T> restriction) {
        return (EnumSettingBuilder<T>) super.restriction(restriction);
    }

    @Override
    public EnumSettingBuilder<T> consumer(BiConsumer<T, T> consumer) {
        return (EnumSettingBuilder<T>) super.consumer(consumer);
    }

    @Override
    public EnumSettingBuilder<T> converter(StringConverter<T> converter) {
        return (EnumSettingBuilder<T>) super.converter(converter);
    }

    @Override
    public Setting<T> build() {
        try {
            if (usage == null) {
                StringBuilder sb = new StringBuilder();
                int i = 0;
                for (Enum a : (Enum[]) defaultValue.getClass().getMethod("values").invoke(null)) {
                    if (i > 0) sb.append(" or ");
                    sb.append(a.toString());
                    i++;
                }
                usage = sb.toString();
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        if (converter == null) {
            converter = string -> {
                try {
                    for (Enum a : (Enum[]) defaultValue.getClass().getMethod("values").invoke(null)) {
                        if (a.toString().equalsIgnoreCase(string)) return (T) a;
                    }
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
                return null;
            };
        }

        return super.build();
    }
}
