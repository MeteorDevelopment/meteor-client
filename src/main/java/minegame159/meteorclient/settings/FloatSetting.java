package minegame159.meteorclient.settings;

public class FloatSetting extends Setting<Float> {
    private final Float min, max;

    public FloatSetting(String name, String description, Float defaultValue, Float min, Float max) {
        super(SettingType.Float, name, description, defaultValue);
        this.min = min;
        this.max = max;
    }

    @Override
    public int parse(String[] args) {
        if (args.length < 1) return 0;
        try {
            float parsed = (float) Double.parseDouble(args[0]);
            if ((min != null && parsed < min) || (max != null && parsed > max)) return -1;
            value = parsed;
            return 1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    @Override
    public String getUsage() {
        String usage = "#blue<";
        usage += "number#gray";
        if (min != null) usage += " " + min.toString() + "-";
        else if (max != null) usage += " inf-";
        if (max != null) usage += max.toString();
        else if (min != null) usage += "inf";
        return usage + "#blue>";
    }
}
