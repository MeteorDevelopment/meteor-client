/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.Chat;

import java.util.List;

public class Modules extends Command {
    public Modules() {
        super("modules", "Lists all modules.");
    }

    @Override
    public void run(String[] args) {
        Chat.info("All (highlight)%d (default)modules:", ModuleManager.INSTANCE.getAll().size());

        for (Category category : ModuleManager.CATEGORIES) {
            List<Module> group = ModuleManager.INSTANCE.getGroup(category);
            Chat.info("- (highlight)%s (default)(%d):", category.toString(), group.size());

            for (Module module : group) {
                Chat.info("  - (highlight)%s%s (default)- %s", Config.INSTANCE.getPrefix(), module.name, module.description);
            }
        }
    }
}
