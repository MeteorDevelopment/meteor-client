package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.Utils;

public class Modules extends Command {
    public Modules() {
        super("modules", "Lists all modules.");
    }

    @Override
    public void run(String[] args) {
        Utils.sendMessage("#yellowAll #gray%d #yellowmodules:", ModuleManager.getCount());

        // Combat
        Utils.sendMessage("  #pinkPlayer #gray(%d)#pink:", ModuleManager.combatCount());
        ModuleManager.combatForEach(module -> {
            Utils.sendMessage("    #yellow%s%s #gray- #yellow%s", Config.instance.prefix, module.name, module.description);
        });

        // Player
        Utils.sendMessage("  #pinkPlayer #gray(%d)#pink:", ModuleManager.playerCount());
        ModuleManager.playerForEach(module -> {
            Utils.sendMessage("    #yellow%s%s #gray- #yellow%s", Config.instance.prefix, module.name, module.description);
        });

        // Movement
        Utils.sendMessage("  #pinkMovement #gray(%d)#pink:", ModuleManager.movementCount());
        ModuleManager.movementForEach(module -> {
            Utils.sendMessage("    #yellow%s%s #gray- #yellow%s", Config.instance.prefix, module.name, module.description);
        });

        // Render
        Utils.sendMessage("  #pinkRender #gray(%d)#pink:", ModuleManager.renderCount());
        ModuleManager.renderForEach(module -> {
            Utils.sendMessage("    #yellow%s%s #gray- #yellow%s", Config.instance.prefix, module.name, module.description);
        });

        // Misc
        Utils.sendMessage("  #pinkMisc #gray(%d)#pink:", ModuleManager.miscCount());
        ModuleManager.miscForEach(module -> {
            Utils.sendMessage("    #yellow%s%s #gray- #yellow%s", Config.instance.prefix, module.name, module.description);
        });
    }
}
