/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.Utils;

public class Bind extends Command {
    public Bind() {
        super("bind", "Binds module to a key.");
    }

    @Override
    public void run(String[] args) {
        Module module = Utils.tryToGetModule(args);
        if (module == null) return;

        Chat.info("Press some key.");
        ModuleManager.INSTANCE.setModuleToBind(module);
    }
}
