package minegame159.meteorclient.utils;

import minegame159.meteorclient.modules.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

public class Chat {
    public static void info(Module module, String format, Object... args) {
        sendMsg(module, formatMsg(format, Formatting.GRAY, args), Formatting.GRAY);
    }
    public static void info(String format, Object... args) {
        info(null, format, args);
    }

    public static void warning(Module module, String format, Object... args) {
        sendMsg(module, formatMsg(format, Formatting.YELLOW, args), Formatting.YELLOW);
    }
    public static void warning(String format, Object... args) {
        warning(null, format, args);
    }

    public static void error(Module module, String format, Object... args) {
        sendMsg(module, formatMsg(format, Formatting.RED, args), Formatting.RED);
    }
    public static void error(String format, Object... args) {
        error(null, format, args);
    }

    private static void sendMsg(Module module, String msg, Formatting color) {
        if (MinecraftClient.getInstance().world == null) return;
        if (module != null) {
            MinecraftClient.getInstance().player.sendMessage(new LiteralText(String.format("%s[Meteor] %s[%s]: %s%s", Formatting.BLUE, Formatting.AQUA, module.title, color, msg)));
        } else {
            MinecraftClient.getInstance().player.sendMessage(new LiteralText(String.format("%s[Meteor]: %s%s", Formatting.BLUE, color, msg)));
        }
    }

    private static String formatMsg(String format, Formatting defaultColor, Object... args) {
        String msg = String.format(format, args);
        msg = msg.replaceAll("\\(default\\)", defaultColor.toString());
        msg = msg.replaceAll("\\(highlight\\)", Formatting.WHITE.toString());

        return msg;
    }
}
