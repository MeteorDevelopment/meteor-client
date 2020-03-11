package minegame159.meteorclient.settings;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class Setting<T> {
    public final String name, title, description;
    private String usage;

    private final T defaultValue;
    private T value;

    private final Consumer<T> onChanged;
    public final Consumer<Setting<T>> onModuleActivated;

    public Setting(String name, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated) {
        this.name = name;
        this.title = Arrays.stream(name.split("-")).map(StringUtils::capitalize).collect(Collectors.joining(" "));;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.onChanged = onChanged;
        this.onModuleActivated = onModuleActivated;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        if (!isValueValid(value)) return;
        this.value = value;
        if (onChanged != null) onChanged.accept(value);
    }

    public void reset() {
        value = defaultValue;
        if (onChanged != null) onChanged.accept(value);
    }

    public boolean parse(String str) {
        T newValue = parseImpl(str);

        if (newValue != null) {
            if (isValueValid(newValue)) {
                value = newValue;
                if (onChanged != null) onChanged.accept(value);
            }
        }

        return newValue != null;
    }

    protected abstract T parseImpl(String str);

    protected abstract boolean isValueValid(T value);

    public String getUsage() {
        if (usage == null) usage = generateUsage();
        return usage;
    }

    protected abstract String generateUsage();

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Setting<?> setting = (Setting<?>) o;
        return Objects.equals(name, setting.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
