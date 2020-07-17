package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.utils.Chat;

public class Commands extends Command {
    public Commands() {
        super("commands", "Lists all commands.");
    }

    @Override
    public void run(String[] args) {
        Chat.info("All (highlight)%d (default)commands:", CommandManager.getCount());
        CommandManager.forEach(command -> {
            Chat.info("- (highlight)%s%s (default)- %s", Config.INSTANCE.getPrefix(), command.name, command.description);
        });
    }
}
