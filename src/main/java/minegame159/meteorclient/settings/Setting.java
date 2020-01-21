package minegame159.meteorclient.settings;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.utils.Utils;

public abstract class Setting<T> {
    public final SettingType type;
    public final String name, description;
    public T value;
    private final T defaultValue;

    public Setting(SettingType type, String name, String description, T defaultValue) {
        this.type = type;
        this.name = name;
        this.description = description;
        value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public void reset() {
        value = defaultValue;
    }

    public abstract int parse(String[] args);

    public abstract String getUsage();

    public void sendUsage(String moduleName, boolean usage) {
        if (usage) Utils.sendMessage("#redUsage#white:");
        Utils.sendMessage("  #yellow%s %s %s #gray(%s) #yellow- %s", Config.instance.prefix + moduleName, name, getUsage(), value.toString(), description);
    }
    public void sendValue() {
        Utils.sendMessage("#yellowValue of #blue'%s'#yellow is #blue'%s'#yellow.", name, value.toString());
    }
}
