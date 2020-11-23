/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Chat;

import java.util.Arrays;

public class CommandDispatcher {
    public static void run(String msg) {
        String[] args = msg.split(" ");

        // Get command, if found then run it and if not get module
        Command command = CommandManager.get(args[0]);
        if (command != null) {
            args = subArray(args, 1);
            command.run(args);
            return;
        }

        // Get module
        Module module = ModuleManager.INSTANCE.get(args[0]);
        if (module == null) {
            Chat.error("Not a valid command or module. Do (highlight).commands (default)or (highlight).modules (default)for a list of commands/modules.");
            return;
        }
        args = subArray(args, 1);

        if (args.length <= 0) {
            module.doAction();
            if (module instanceof ToggleModule) ((ToggleModule) module).sendToggledMsg();
        } else {
            // Set or get module setting
            Setting<?> setting = module.settings.get(args[0]);
            if (setting == null) {
                Chat.error("Module (highlight)%s (default)doesn't have a setting with name (highlight)%s(default).", module.title, args[0]);
                return;
            }
            args = subArray(args, 1);

            if (args.length <= 0) {
                // Send setting's value if nothing is after it's name
                Chat.info("Value of (highlight)%s (default)for module (highlight)%s (default)is (highlight)%s(default).", setting.title, module.title, setting.get().toString());
            } else {
                // Parse setting's value and report usage if error
                if (!setting.parse(String.join(" ", args))) {
                    Chat.error("Usage of (highlight)%s (default)is (highlight)%s(default).", setting.name, setting.getUsage());
                }
                if (Config.INSTANCE.chatCommandsInfo) Chat.info("Value of (highlight)%s (default)for module (highlight)%s (default)changed to (highlight)%s(default).", setting.title, module.title, setting.get().toString());
            }
        }
    }

    private static String[] subArray(String[] arr, int startIndex) {
        return Arrays.copyOfRange(arr, startIndex, arr.length);
    }
}
