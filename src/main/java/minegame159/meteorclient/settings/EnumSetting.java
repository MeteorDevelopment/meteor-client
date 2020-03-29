package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.widgets.WEnumButton;
import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

public class EnumSetting<T extends Enum<?>> extends Setting<T> {
    private T[] values;

    public EnumSetting(String name, String description, String group, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated) {
        super(name, description, group, defaultValue, onChanged, onModuleActivated);

        try {
            values = (T[]) defaultValue.getClass().getMethod("values").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        widget = new WEnumButton<>(get());
        ((WEnumButton<T>) widget).action = twEnumButton -> set(twEnumButton.value);
    }

    @Override
    protected T parseImpl(String str) {
        for (T possibleValue : values) {
            if (str.equalsIgnoreCase(possibleValue.toString())) return possibleValue;
        }

        return null;
    }

    @Override
    protected void resetWidget() {
        ((WEnumButton<T>) widget).setValue(get());
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

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();
        tag.putString("value", get().toString());
        return tag;
    }

    @Override
    public T fromTag(CompoundTag tag) {
        parse(tag.getString("value"));

        return get();
    }

    public static class Builder<T extends Enum<?>> {
        protected String name = "undefined", description = "";
        protected String group;
        protected T defaultValue;
        protected Consumer<T> onChanged;
        protected Consumer<Setting<T>> onModuleActivated;

        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T> description(String description) {
            this.description = description;
            return this;
        }

        public Builder<T> group(String group) {
            this.group = group;
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
            return new EnumSetting<>(name, description, group, defaultValue, onChanged, onModuleActivated);
        }
    }
}
