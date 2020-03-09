package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.utils.Utils;

public class Commands extends Command {
    public Commands() {
        super("commands", "Lists all commands.");
    }

    @Override
    public void run(String[] args) {
        Utils.sendMessage("#pinkAll #gray%d #yellowcommands:", CommandManager.getCount());
        CommandManager.forEach(command -> {
            Utils.sendMessage("  #yellow%s%s #gray- #yellow%s", Config.INSTANCE.prefix, command.name, command.description);
        });
    }
}
