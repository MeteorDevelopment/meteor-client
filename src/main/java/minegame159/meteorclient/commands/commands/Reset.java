package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Utils;

public class Reset extends Command {
    public Reset() {
        super("reset", "Resets module's oldsettings.");
    }

    @Override
    public void run(String[] args) {
        Module module = Utils.tryToGetModule(args);
        if (module == null) return;

        for (Setting setting : module.settings) setting.reset();
    }
}
