package minegame159.meteorclient.utils;

import java.lang.reflect.Field;

public class ReflectionField<T> {
    private Field field;

    public ReflectionField(Class klass, String name) {
        try {
            field = klass.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        field.setAccessible(true);
    }

    public T get(Object instance) {
        try {
            return (T)field.get(instance);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
    public T getStatic() {
        return get(null);
    }

    public void set(Object instance, T value) {
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
