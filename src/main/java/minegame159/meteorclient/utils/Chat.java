/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

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
            MinecraftClient.getInstance().player.sendMessage(new LiteralText(String.format("%s[%sMeteor%s] %s[%s] %s%s", Formatting.GRAY, Formatting.BLUE,Formatting.GRAY, Formatting.AQUA, module.title, color, msg)), false);
        } else {
            MinecraftClient.getInstance().player.sendMessage(new LiteralText(String.format("%s[%sMeteor%s] %s%s", Formatting.GRAY, Formatting.BLUE, Formatting.GRAY, color, msg)), false);
        }
    }

    private static String formatMsg(String format, Formatting defaultColor, Object... args) {
        String msg = String.format(format, args);
        msg = msg.replaceAll("\\(default\\)", defaultColor.toString());
        msg = msg.replaceAll("\\(highlight\\)", Formatting.WHITE.toString());

        return msg;
    }
}
