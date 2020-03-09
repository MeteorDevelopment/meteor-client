package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.Utils;

public class Bind extends Command {
    public Bind() {
        super("bind", "Binds module to a key.");
    }

    @Override
    public void run(String[] args) {
        Module module = Utils.tryToGetModule(args);
        if (module == null) return;

        Utils.sendMessage("#yellowPress some key.");
        ModuleManager.INSTANCE.setModuleToBind(module);
    }
}
