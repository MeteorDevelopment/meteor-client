package minegame159.meteorclient.settings;

import minegame159.meteorclient.utils.Color;

public class ColorSetting extends Setting<Color> {
    public ColorSetting(String name, String description, Color defaultValue) {
        super(SettingType.Color, name, description, defaultValue);
    }

    @Override
    public int parse(String[] args) {
        if (args.length < 4) return 0;
        int parsed = 0;
        try {
            int r = (int) Double.parseDouble(args[0]);
            parsed++;
            int g = (int) Double.parseDouble(args[1]);
            parsed++;
            int b = (int) Double.parseDouble(args[2]);
            parsed++;
            int a = (int) Double.parseDouble(args[3]);
            parsed++;

            if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255 || a < 0 || a > 255) return -4;
            value = new Color(r, g, b, a);

            return 4;
        } catch (NumberFormatException ignored) {
            return -parsed;
        }
    }

    @Override
    public String getUsage() {
        return "#blue<0-255 0-255 0-255 0-255>";
    }
}
