package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;

import java.util.ArrayList;
import java.util.List;

public class Panic extends Command {
    public Panic() {
        super("panic", "Disables all modules.");
    }

    @Override
    public void run(String[] args) {
        List<ToggleModule> active = new ArrayList<>(ModuleManager.INSTANCE.getActive());
        for (ToggleModule module : active) module.toggle();
    }
}
