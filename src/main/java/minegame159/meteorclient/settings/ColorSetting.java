package minegame159.meteorclient.settings;

import minegame159.meteorclient.utils.Color;

import java.util.function.Consumer;

public class ColorSetting extends Setting<Color> {
    public ColorSetting(String name, String description, Color defaultValue, Consumer<Color> onChanged, Consumer<Setting<Color>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);
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
    protected boolean isValueValid(Color value) {
        value.validate();
        return true;
    }

    @Override
    protected String generateUsage() {
        return "#blue0-255 0-255 0-255 0-255";
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private Color defaultValue;
        private Consumer<Color> onChanged;
        private Consumer<Setting<Color>> onModuleActivated;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
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

        public ColorSetting build() {
            return new ColorSetting(name, description, defaultValue, onChanged, onModuleActivated);
        }
    }
}
