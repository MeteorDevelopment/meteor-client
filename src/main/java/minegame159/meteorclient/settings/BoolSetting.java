package minegame159.meteorclient.settings;

public class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, String description, Boolean defaultValue) {
        super(SettingType.Bool, name, description, defaultValue);
    }

    @Override
    public int parse(String[] args) {
        if (args.length < 1) return 0;
        value = Boolean.parseBoolean(args[0].trim());
        return 1;
    }

    @Override
    public String getUsage() {
        return "#blue<true #grayor #bluefalse>";
    }
}
