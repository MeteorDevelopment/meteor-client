/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import com.mojang.brigadier.StringReader;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.misc.text.MeteorClickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ChatUtils {
    private static final List<Tuple<String, Supplier<Component>>> customPrefixes = new ArrayList<>();
    private static String forcedPrefixClassName;

    private static Component PREFIX;

    private ChatUtils() {
    }

    @PostInit
    public static void init() {
        PREFIX = Component.empty()
            .setStyle(Style.EMPTY.applyFormats(ChatFormatting.GRAY))
            .append("[")
            .append(Component.literal("Meteor").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MeteorClient.ADDON.color.getPacked()))))
            .append("] ");
    }

    public static Component getMeteorPrefix() {
        return PREFIX;
    }

    /**
     * Registers a custom prefix to be used when calling from a class in the specified package. When null is returned from the supplier the default Meteor prefix is used.
     */
    @SuppressWarnings("unused")
    public static void registerCustomPrefix(String packageName, Supplier<Component> supplier) {
        for (Tuple<String, Supplier<Component>> pair : customPrefixes) {
            if (pair.getA().equals(packageName)) {
                pair.setB(supplier);
                return;
            }
        }

        customPrefixes.add(new Tuple<>(packageName, supplier));
    }

    /**
     * The package name must match exactly to the one provided through {@link #registerCustomPrefix(String, Supplier)}.
     */
    @SuppressWarnings("unused")
    public static void unregisterCustomPrefix(String packageName) {
        customPrefixes.removeIf(pair -> pair.getA().equals(packageName));
    }

    public static void forceNextPrefixClass(Class<?> klass) {
        forcedPrefixClassName = klass.getName();
    }

    // Player

    /**
     * Sends the message as if the user typed it into chat and adds it to the chat history.
     */
    public static void sendPlayerMsg(String message) {
        sendPlayerMsg(message, true);
    }

    /**
     * Sends the message as if the user typed it into chat.
     */
    public static void sendPlayerMsg(String message, boolean addToHistory) {
        if (addToHistory) mc.gui.getChat().addRecentChat(message);

        if (message.startsWith("/")) mc.player.connection.sendCommand(message.substring(1));
        else mc.player.connection.sendChat(message);
    }

    // Default

    public static void info(String message, Object... args) {
        sendMsg(ChatFormatting.GRAY, message, args);
    }

    public static void infoPrefix(String prefix, String message, Object... args) {
        sendMsg(0, prefix, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GRAY, message, args);
    }

    // Warning

    public static void warning(String message, Object... args) {
        sendMsg(ChatFormatting.YELLOW, message, args);
    }

    public static void warningPrefix(String prefix, String message, Object... args) {
        sendMsg(0, prefix, ChatFormatting.LIGHT_PURPLE, ChatFormatting.YELLOW, message, args);
    }

    // Error

    public static void error(String message, Object... args) {
        sendMsg(ChatFormatting.RED, message, args);
    }

    public static void errorPrefix(String prefix, String message, Object... args) {
        sendMsg(0, prefix, ChatFormatting.LIGHT_PURPLE, ChatFormatting.RED, message, args);
    }

    // Misc

    public static void sendMsg(Component message) {
        sendMsg(null, message);
    }

    public static void sendMsg(String prefix, Component message) {
        sendMsg(0, prefix, ChatFormatting.LIGHT_PURPLE, message);
    }

    public static void sendMsg(ChatFormatting color, String message, Object... args) {
        sendMsg(0, null, null, color, message, args);
    }

    public static void sendMsg(int id, ChatFormatting color, String message, Object... args) {
        sendMsg(id, null, null, color, message, args);
    }

    public static void sendMsg(int id, @Nullable String prefixTitle, @Nullable ChatFormatting prefixColor, ChatFormatting messageColor, String messageContent, Object... args) {
        MutableComponent message = formatMsg(String.format(messageContent, args), messageColor);
        sendMsg(id, prefixTitle, prefixColor, message);
    }

    public static void sendMsg(int id, @Nullable String prefixTitle, @Nullable ChatFormatting prefixColor, String messageContent, ChatFormatting messageColor) {
        MutableComponent message = formatMsg(messageContent, messageColor);
        sendMsg(id, prefixTitle, prefixColor, message);
    }

    public static void sendMsg(int id, @Nullable String prefixTitle, @Nullable ChatFormatting prefixColor, Component msg) {
        if (mc.level == null) return;

        MutableComponent message = Component.empty();
        message.append(getPrefix());
        if (prefixTitle != null) message.append(getCustomPrefix(prefixTitle, prefixColor));
        message.append(msg);

        if (!Config.get().deleteChatFeedback.get()) id = 0;

        final int finalId = id; // Intellij copes about using non-final args in lambdas
        mc.execute(() -> ((IChatHud) mc.gui.getChat()).meteor$add(message, finalId));
    }

    private static MutableComponent getCustomPrefix(String prefixTitle, ChatFormatting prefixColor) {
        MutableComponent prefix = Component.empty();
        prefix.setStyle(prefix.getStyle().applyFormats(ChatFormatting.GRAY));

        prefix.append("[");

        MutableComponent moduleTitle = Component.literal(prefixTitle);
        moduleTitle.setStyle(moduleTitle.getStyle().applyFormats(prefixColor));
        prefix.append(moduleTitle);

        prefix.append("] ");

        return prefix;
    }

    private static Component getPrefix() {
        if (customPrefixes.isEmpty()) {
            forcedPrefixClassName = null;
            return PREFIX;
        }

        boolean foundChatUtils = false;
        String className = null;

        if (forcedPrefixClassName != null) {
            className = forcedPrefixClassName;
            forcedPrefixClassName = null;
        } else {
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

        for (Tuple<String, Supplier<Component>> pair : customPrefixes) {
            if (className.startsWith(pair.getA())) {
                Component prefix = pair.getB().get();
                return prefix != null ? prefix : PREFIX;
            }
        }

        return PREFIX;
    }

    private static MutableComponent formatMsg(String message, ChatFormatting defaultColor) {
        StringReader reader = new StringReader(message);
        MutableComponent text = Component.empty();
        Style style = Style.EMPTY.applyFormats(defaultColor);
        StringBuilder result = new StringBuilder();
        boolean formatting = false;
        while (reader.canRead()) {
            char c = reader.read();
            if (c == '(') {
                text.append(Component.literal(result.toString()).setStyle(style));
                result.setLength(0);
                result.append(c);
                formatting = true;
            } else {
                result.append(c);

                if (formatting && c == ')') {
                    switch (result.toString()) {
                        case "(default)" -> {
                            style = style.applyFormats(defaultColor);
                            result.setLength(0);
                        }
                        case "(highlight)" -> {
                            style = style.applyFormats(ChatFormatting.WHITE);
                            result.setLength(0);
                        }
                        case "(underline)" -> {
                            style = style.applyFormats(ChatFormatting.UNDERLINE);
                            result.setLength(0);
                        }
                        case "(bold)" -> {
                            style = style.applyFormats(ChatFormatting.BOLD);
                            result.setLength(0);
                        }
                    }
                    formatting = false;
                }
            }
        }

        if (!result.isEmpty()) text.append(Component.literal(result.toString()).setStyle(style));

        return text;
    }

    public static MutableComponent formatCoords(Vec3 pos) {
        String coordsString = String.format("(highlight)(underline)%.0f, %.0f, %.0f(default)", pos.x, pos.y, pos.z);
        MutableComponent coordsText = formatMsg(coordsString, ChatFormatting.GRAY);

        if (BaritoneUtils.IS_AVAILABLE) {
            Style style = coordsText.getStyle().applyFormats(ChatFormatting.BOLD)
                .withHoverEvent(new HoverEvent.ShowText(
                    Component.literal("Set as Baritone goal")
                ))
                .withClickEvent(new MeteorClickEvent(
                    String.format("%sgoto %d %d %d", BaritoneUtils.getPrefix(), (int) pos.x, (int) pos.y, (int) pos.z)
                ));

            coordsText.setStyle(style);
        }

        return coordsText;
    }
}
