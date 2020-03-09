package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.Utils;

import java.util.List;

public class Modules extends Command {
    public Modules() {
        super("modules", "Lists all modules.");
    }

    @Override
    public void run(String[] args) {
        Utils.sendMessage("#yellowAll #gray%d #yellowmodules:", ModuleManager.INSTANCE.getAll().size());

        for (Category category : ModuleManager.CATEGORIES) {
            List<Module> group = ModuleManager.INSTANCE.getGroup(category);
            Utils.sendMessage("  #pink%s #gray(%d)#pink:", category.toString(), group.size());

            for (Module module : group) {
                Utils.sendMessage("    #yellow%s%s #gray- #yellow%s", Config.INSTANCE.prefix, module.name, module.description);
            }
        }
    }
}
