package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.utils.Utils;

public class ResetBind extends Command {
    public ResetBind() {
        super("reset-bind", "Resets modules bind.");
    }

    @Override
    public void run(String[] args) {
        Module module = Utils.tryToGetModule(args);
        if (module == null) return;

        Utils.sendMessage("#yellowBind has been reset.");
        module.setKey(-1);
    }
}
