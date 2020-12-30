/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.utils;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.mixininterface.IChatHud;
import minegame159.meteorclient.modules.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class Chat {
    public static final int INFO_ID = 1591;
    public static final int WARNING_ID = 1592;
    public static final int ERROR_ID = 1593;

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static BaseText getPrefix(Module module) {
        BaseText meteor = new LiteralText("Meteor");
        meteor.setStyle(meteor.getStyle().withFormatting(Formatting.BLUE));

        BaseText prefix = new LiteralText("");
        prefix.setStyle(prefix.getStyle().withFormatting(Formatting.GRAY));
        prefix.append("[");
        prefix.append(meteor);
        prefix.append("] ");

        if (module != null) {
            BaseText moduleTitle = new LiteralText(module.title);
            moduleTitle.setStyle(moduleTitle.getStyle().withFormatting(Formatting.AQUA));
            prefix.append("[");
            prefix.append(moduleTitle);
            prefix.append("] ");
        }

        return prefix;
    }

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

    public static void sendMag(int id, Module module, Text msg) {
        if (mc.world == null) return;

        BaseText message = new LiteralText("");
        message.append(getPrefix(module));
        message.append(msg);
        ((IChatHud) mc.inGameHud.getChatHud()).add(message, id);
    }

    public static void sendMsg(int id, Module module, String msg, Formatting color) {
        if (!Config.INSTANCE.deleteChatCommandsInfo) id = 0;

        BaseText message = new LiteralText(msg);
        message.setStyle(message.getStyle().withFormatting(color));

        sendMag(id, module, message);
    }

    private static String formatMsg(String format, Formatting defaultColor, Object... args) {
        String msg = String.format(format, args);
        msg = msg.replaceAll("\\(default\\)", defaultColor.toString());
        msg = msg.replaceAll("\\(highlight\\)", Formatting.WHITE.toString());

        return msg;
    }
}
