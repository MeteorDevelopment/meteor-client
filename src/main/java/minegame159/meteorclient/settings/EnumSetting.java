package minegame159.meteorclient.settings;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

public class EnumSetting<T extends Enum<?>> extends Setting<T> {
    private T[] values;

    private EnumSetting(String name, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        try {
            values = (T[]) defaultValue.getClass().getMethod("values").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected T parseImpl(String str) {
        for (T possibleValue : values) {
            if (str.equalsIgnoreCase(possibleValue.toString())) return possibleValue;
        }

        return null;
    }

    @Override
    protected boolean isValueValid(T value) {
        return true;
    }

    @Override
    protected String generateUsage() {
        String usage = "";

        for (int i = 0; i < values.length; i++) {
            if (i > 0) usage += " #grayor ";
            usage += "#blue" + values[i];
        }

        return usage;
    }

    public static class Builder<T extends Enum<?>> {
        private String name = "undefined", description = "";
        private T defaultValue;
        private Consumer<T> onChanged;
        private Consumer<Setting<T>> onModuleActivated;

        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T> description(String description) {
            this.description = description;
            return this;
        }

        public Builder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder<T> onChanged(Consumer<T> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<T>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public EnumSetting<T> build() {
            return new EnumSetting<>(name, description, defaultValue, onChanged, onModuleActivated);
        }
    }
}
