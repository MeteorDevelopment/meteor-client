package minegame159.meteorclient.commands.commands;

import baritone.api.BaritoneAPI;
import minegame159.meteorclient.commands.Command;

public class Baritone extends Command {
    public Baritone() {
        super("b", "Baritone.");
    }

    @Override
    public void run(String[] args) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(String.join(" ", args));
    }
}
