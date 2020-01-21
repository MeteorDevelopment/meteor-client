package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;

public class ClearChat extends Command {
    public ClearChat() {
        super("clear-chat", "Clears your chat.");
    }

    @Override
    public void run(String[] args) {
        MC.inGameHud.getChatHud().clear(false);
    }
}
