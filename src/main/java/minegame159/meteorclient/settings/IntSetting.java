package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.widgets.WIntTextBox;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

public class IntSetting extends Setting<Integer> {
    private final Integer min, max;

    private IntSetting(String name, String description, String group, Integer defaultValue, Consumer<Integer> onChanged, Consumer<Setting<Integer>> onModuleActivated, Integer min, Integer max, boolean visible) {
        super(name, description, group, defaultValue, onChanged, onModuleActivated, visible);
        this.min = min;
        this.max = max;

        widget = new WIntTextBox(get(), 70);
        ((WIntTextBox) widget).action = wIntTextBox -> {
            if (!set(wIntTextBox.value)) wIntTextBox.setValue(get());
        };
    }

    @Override
    protected Integer parseImpl(String str) {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    protected void resetWidget() {
        ((WIntTextBox) widget).setValue(get());
    }

    @Override
    protected boolean isValueValid(Integer value) {
        return (min == null || value >= min) && (max == null || value <= max);
    }

    @Override
    protected String generateUsage() {
        String usage = "#blue";

        if (min == null) usage += "inf";
        else usage += min;

        usage += " #gray- #blue";

        if (max == null) usage += "inf";
        else usage += max;

        return usage;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();
        tag.putInt("value", get());
        return tag;
    }

    @Override
    public Integer fromTag(CompoundTag tag) {
        set(tag.getInt("value"));

        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private String group;
        private Integer defaultValue;
        private Consumer<Integer> onChanged;
        private Consumer<Setting<Integer>> onModuleActivated;
        private Integer min, max;
        private boolean visible = true;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder defaultValue(int defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<Integer> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<Integer>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public Builder min(int min) {
            this.min = min;
            return this;
        }

        public Builder max(int max) {
            this.max = max;
            return this;
        }

        public Builder visible(boolean visible) {
            this.visible = visible;
            return this;
        }

        public IntSetting build() {
            return new IntSetting(name, description, group, defaultValue, onChanged, onModuleActivated, min, max, visible);
        }
    }
}
