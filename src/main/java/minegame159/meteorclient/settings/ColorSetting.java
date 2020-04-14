package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.widgets.WColorEdit;
import minegame159.meteorclient.utils.Color;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

public class ColorSetting extends Setting<Color> {
    public ColorSetting(String name, String description, String group, Color defaultValue, Consumer<Color> onChanged, Consumer<Setting<Color>> onModuleActivated, boolean visible) {
        super(name, description, group, defaultValue, onChanged, onModuleActivated, visible);

        widget = new WColorEdit(get());
        ((WColorEdit) widget).action = wColorEdit -> {
            set(wColorEdit.color);
            wColorEdit.set(get());
        };
    }

    @Override
    protected Color parseImpl(String str) {
        try {
            String[] strs = str.split(" ");
            return new Color(Integer.parseInt(strs[0]), Integer.parseInt(strs[1]), Integer.parseInt(strs[2]), Integer.parseInt(strs[3]));
        } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public void reset(boolean callbacks) {
        value = new Color(defaultValue);
        if (callbacks) {
            resetWidget();
            changed();
        }
    }

    @Override
    protected void resetWidget() {
        ((WColorEdit) widget).set(get());
    }

    @Override
    protected boolean isValueValid(Color value) {
        value.validate();
        return true;
    }

    @Override
    protected String generateUsage() {
        return "#blue0-255 0-255 0-255 0-255";
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();
        tag.put("value", get().toTag());
        return tag;
    }

    @Override
    public Color fromTag(CompoundTag tag) {
        get().fromTag(tag.getCompound("value"));

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private String group;
        private Color defaultValue;
        private Consumer<Color> onChanged;
        private Consumer<Setting<Color>> onModuleActivated;
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

        public Builder defaultValue(Color defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<Color> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<Color>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public Builder visible(boolean visible) {
            this.visible = visible;
            return this;
        }

        public ColorSetting build() {
            return new ColorSetting(name, description, group, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
