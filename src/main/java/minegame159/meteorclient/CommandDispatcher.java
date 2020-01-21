package minegame159.meteorclient;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Utils;

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
        Module module = ModuleManager.get(args[0]);
        if (module == null) return;
        args = subArray(args, 1);

        if (args.length <= 0) {
            // Toggle module if nothing is after it's name
            module.toggle();
        } else {
            // Set or get module setting
            Setting setting = module.getSetting(args[0]);
            if (setting == null) {
                Utils.sendMessage("#redModule #blue'%s' #reddoesn't have setting with name #blue'%s'#red.", module.title, args[0]);
                return;
            }
            args = subArray(args, 1);

            if (args.length <= 0) {
                // Send setting's value if nothing is after it's name
                setting.sendValue();
            } else {
                // Parse setting's value and report usage if error
                int parseResult = setting.parse(args);
                if (parseResult > 0) {
                    args = subArray(args, parseResult);
                    module.onSettingChanges();
                }
                else if (parseResult == 0) setting.sendUsage(module.name, true);
                else {
                    args = subArray(args, -parseResult);
                    setting.sendUsage(module.name, true);
                }
            }
        }
    }

    private static String[] subArray(String[] arr, int startIndex) {
        return Arrays.copyOfRange(arr, startIndex, arr.length);
    }
}
