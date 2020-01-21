package minegame159.meteorclient.settings;

import java.lang.reflect.InvocationTargetException;

public class EnumSetting<T extends Enum> extends Setting<T> {
    private T[] values;

    public EnumSetting(String name, String description, T defaultValue) {
        super(SettingType.Enum, name, description, defaultValue);
        try {
            values = (T[]) defaultValue.getDeclaringClass().getMethod("values").invoke(null);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int parse(String[] args) {
        if (args.length < 1) return 0;

        for (int i = 0; i < values.length; i++) {
            T a = values[i];

            if (a.toString().equalsIgnoreCase(args[0])) {
                value = a;
                return 1;
            }
        }

        return -1;
    }

    @Override
    public String getUsage() {
        String usage = "#blue<";
        for (int i = 0; i < values.length; i++) {
            if (i == 0) usage += values[i];
            else usage += " #grayor#blue " + values[i];
        }
        return usage + "#blue>";
    }
}
