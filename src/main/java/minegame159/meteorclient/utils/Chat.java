/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.utils;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.mixininterface.IChatHud;
import minegame159.meteorclient.modules.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

public class Chat {
    private static final int INFO_ID = 1591;
    private static final int WARNING_ID = 1592;
    private static final int ERROR_ID = 1593;

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void info(Module module, String format, Object... args) {
        sendMsg(INFO_ID, module, formatMsg(format, Formatting.GRAY, args), Formatting.GRAY);
    }
    public static void info(String format, Object... args) {
        info(null, format, args);
    }

    public static void warning(Module module, String format, Object... args) {
        sendMsg(WARNING_ID, module, formatMsg(format, Formatting.YELLOW, args), Formatting.YELLOW);
    }
    public static void warning(String format, Object... args) {
        warning(null, format, args);
    }

    public static void error(Module module, String format, Object... args) {
        sendMsg(ERROR_ID, module, formatMsg(format, Formatting.RED, args), Formatting.RED);
    }
    public static void error(String format, Object... args) {
        error(null, format, args);
    }

    private static void sendMsg(int id, Module module, String msg, Formatting color) {
        if (mc.world == null) return;

        if (!Config.INSTANCE.deleteChatCommandsInfo) id = 0;

        if (module != null) {
            ((IChatHud) mc.inGameHud.getChatHud()).add(new LiteralText(String.format("%s[%sMeteor%s] %s[%s] %s%s", Formatting.GRAY, Formatting.BLUE,Formatting.GRAY, Formatting.AQUA, module.title, color, msg)), id);
        } else {
            ((IChatHud) mc.inGameHud.getChatHud()).add(new LiteralText(String.format("%s[%sMeteor%s] %s%s", Formatting.GRAY, Formatting.BLUE, Formatting.GRAY, color, msg)), id);
        }
    }

    private static String formatMsg(String format, Formatting defaultColor, Object... args) {
        String msg = String.format(format, args);
        msg = msg.replaceAll("\\(default\\)", defaultColor.toString());
        msg = msg.replaceAll("\\(highlight\\)", Formatting.WHITE.toString());

        return msg;
    }
}
