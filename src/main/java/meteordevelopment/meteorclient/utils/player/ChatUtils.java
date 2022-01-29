/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import baritone.api.BaritoneAPI;
import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.mixin.ChatHudAccessor;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.Init;
import meteordevelopment.meteorclient.utils.InitStage;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ChatUtils {
    private static final List<Pair<String, Supplier<Text>>> customPrefixes = new ArrayList<>();
    private static String forcedPrefixClassName;

    private static Text PREFIX;

    @Init(stage = InitStage.Post)
    public static void init() {
        PREFIX = new LiteralText("")
            .setStyle(Style.EMPTY.withFormatting(Formatting.GRAY))
            .append("[")
            .append(new LiteralText("Meteor").setStyle(Style.EMPTY.withColor(new TextColor(AddonManager.METEOR.color.getPacked()))))
            .append("] ");
    }

    /** Registers a custom prefix to be used when calling from a class in the specified package. When null is returned from the supplier the default Meteor prefix is used. */
    public static void registerCustomPrefix(String packageName, Supplier<Text> supplier) {
        for (Pair<String, Supplier<Text>> pair : customPrefixes) {
            if (pair.getLeft().equals(packageName)) {
                pair.setRight(supplier);
                return;
            }
        }

        customPrefixes.add(new Pair<>(packageName, supplier));
    }

    public static void forceNextPrefixClass(Class<?> klass) {
        forcedPrefixClassName = klass.getName();
    }

    // Default
    public static void info(String message, Object... args) {
        sendMsg(Formatting.GRAY, message, args);
    }

    public static void info(String prefix, String message, Object... args) {
        sendMsg(0, prefix, Formatting.LIGHT_PURPLE, Formatting.GRAY, message, args);
    }

    // Warning
    public static void warning(String message, Object... args) {
        sendMsg(Formatting.YELLOW, message, args);
    }

    public static void warning(String prefix, String message, Object... args) {
        sendMsg(0, prefix, Formatting.LIGHT_PURPLE, Formatting.YELLOW, message, args);
    }

    // Error
    public static void error(String message, Object... args) {
        sendMsg(Formatting.RED, message, args);
    }

    public static void error(String prefix, String message, Object... args) {
        sendMsg(0, prefix, Formatting.LIGHT_PURPLE, Formatting.RED, message, args);
    }

    // Misc
    public static void sendMsg(Text message) {
        sendMsg(null, message);
    }

    public static void sendMsg(String prefix, Text message) {
        sendMsg(0, prefix, Formatting.LIGHT_PURPLE, message);
    }

    public static void sendMsg(Formatting color, String message, Object... args) {
        sendMsg(0, null, null, color, message, args);
    }

    public static void sendMsg(int id, Formatting color, String message, Object... args) {
        sendMsg(id, null, null, color, message, args);
    }

    public static void sendMsg(int id, @Nullable String prefixTitle, @Nullable Formatting prefixColor, Formatting messageColor, String messageContent, Object... args) {
        sendMsg(id, prefixTitle, prefixColor, formatMsg(messageContent, messageColor, args), messageColor);
    }

    public static void sendMsg(int id, @Nullable String prefixTitle, @Nullable Formatting prefixColor, String messageContent, Formatting messageColor) {
        BaseText message = new LiteralText(messageContent);
        message.setStyle(message.getStyle().withFormatting(messageColor));
        sendMsg(id, prefixTitle, prefixColor, message);
    }

    public static void sendMsg(int id, @Nullable String prefixTitle, @Nullable Formatting prefixColor, Text msg) {
        if (mc.world == null) return;

        BaseText message = new LiteralText("");
        message.append(getPrefix());
        if (prefixTitle != null) message.append(getCustomPrefix(prefixTitle, prefixColor));
        message.append(msg);

        if (!Config.get().deleteChatFeedback.get()) id = 0;

        ((ChatHudAccessor) mc.inGameHud.getChatHud()).add(message, id);
    }

    private static BaseText getCustomPrefix(String prefixTitle, Formatting prefixColor) {
        BaseText prefix = new LiteralText("");
        prefix.setStyle(prefix.getStyle().withFormatting(Formatting.GRAY));

        prefix.append("[");

        BaseText moduleTitle = new LiteralText(prefixTitle);
        moduleTitle.setStyle(moduleTitle.getStyle().withFormatting(prefixColor));
        prefix.append(moduleTitle);

        prefix.append("] ");

        return prefix;
    }

    private static Text getPrefix() {
        if (customPrefixes.isEmpty()) {
            forcedPrefixClassName = null;
            return PREFIX;
        }

        boolean foundChatUtils = false;
        String className = null;

        if (forcedPrefixClassName != null) {
            className = forcedPrefixClassName;
            forcedPrefixClassName = null;
        }
        else {
            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                if (foundChatUtils) {
                    if (!element.getClassName().equals(ChatUtils.class.getName())) {
                        className = element.getClassName();
                        break;
                    }
                } else {
                    if (element.getClassName().equals(ChatUtils.class.getName())) foundChatUtils = true;
                }
            }
        }

        if (className == null) return PREFIX;

        for (Pair<String, Supplier<Text>> pair : customPrefixes) {
            if (className.startsWith(pair.getLeft())) {
                Text prefix = pair.getRight().get();
                return prefix != null ? prefix : PREFIX;
            }
        }

        return PREFIX;
    }

    private static String formatMsg(String format, Formatting defaultColor, Object... args) {
        String msg = String.format(format, args);
        msg = msg.replaceAll("\\(default\\)", defaultColor.toString());
        msg = msg.replaceAll("\\(highlight\\)", Formatting.WHITE.toString());
        msg = msg.replaceAll("\\(underline\\)", Formatting.UNDERLINE.toString());

        return msg;
    }

    public static BaseText formatCoords(Vec3d pos) {
        String coordsString = String.format("(highlight)(underline)%.0f, %.0f, %.0f(default)", pos.x, pos.y, pos.z);
        coordsString = formatMsg(coordsString, Formatting.GRAY);
        BaseText coordsText = new LiteralText(coordsString);
        coordsText.setStyle(coordsText.getStyle()
                .withFormatting(Formatting.BOLD)
                .withClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        String.format("%sgoto %d %d %d", BaritoneAPI.getSettings().prefix.value, (int) pos.x, (int) pos.y, (int) pos.z)
                ))
                .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new LiteralText("Set as Baritone goal")
                ))
        );
        return coordsText;
    }
}
