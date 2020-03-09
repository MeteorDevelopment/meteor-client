package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Utils;

public class Settings extends Command {
    public Settings() {
        super("settings", "Displays all settings of specified module.");
    }

    @Override
    public void run(String[] args) {
        Module module = Utils.tryToGetModule(args);
        if (module == null) return;

        Utils.sendMessage("#pink%s:", module.title);
        for (Setting setting : module.settings) {
            Utils.sendMessage("  #yellowUsage of #blue'%s' #gray(%s) #yellowis #gray%s#yellow.", setting.name, setting.get().toString(), setting.getUsage());
        }
    }
}
