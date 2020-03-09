package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.settings.Setting;

public class ResetAll extends Command {
    public ResetAll() {
        super("reset-all", "Resets all modules oldsettings.");
    }

    @Override
    public void run(String[] args) {
        for (Module module : ModuleManager.INSTANCE.getAll()) {
            for (Setting setting : module.settings) setting.reset();
        }
    }
}
