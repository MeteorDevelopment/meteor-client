/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.misc.text.MessageBuilderImpl;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ChatUtils {
    private static final List<Pair<String, Supplier<Text>>> customPrefixes = new ArrayList<>();

    private static Text PREFIX;

    private ChatUtils() {
    }

    @PostInit
    public static void init() {
        PREFIX = Text.empty()
            .setStyle(Style.EMPTY.withFormatting(Formatting.GRAY))
            .append("[")
            .append(Text.literal("Meteor").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MeteorClient.ADDON.color.getPacked()))))
            .append("] ");
    }

    /**
     * @deprecated use {@link GuiTheme#getChatPrefix()}
     */
    @Deprecated
    public static Text getMeteorPrefix() {
        return PREFIX;
    }

    /**
     * Registers a custom prefix to be used when calling from a class in the specified package. When null is returned from the supplier the default Meteor prefix is used.
     */
    @SuppressWarnings("unused")
    public static void registerCustomPrefix(String packageName, Supplier<Text> supplier) {
        for (Pair<String, Supplier<Text>> pair : customPrefixes) {
            if (pair.getLeft().equals(packageName)) {
                pair.setRight(supplier);
                return;
            }
        }

        customPrefixes.add(new Pair<>(packageName, supplier));
    }

    /**
     * The package name must match exactly to the one provided through {@link #registerCustomPrefix(String, Supplier)}.
     */
    @SuppressWarnings("unused")
    public static void unregisterCustomPrefix(String packageName) {
        customPrefixes.removeIf(pair -> pair.getLeft().equals(packageName));
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
        if (addToHistory) mc.inGameHud.getChatHud().addToMessageHistory(message);

        if (message.startsWith("/")) mc.player.networkHandler.sendChatCommand(message.substring(1));
        else mc.player.networkHandler.sendChatMessage(message);
    }

    public static void sendMsg(int id, Text message) {
        if (mc.world == null) return;

        if (mc.isOnThread()) {
            ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(message, id);
        } else {
            mc.execute(() -> ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(message, id));
        }
    }

    public static Text getPrefix(@Nullable Object source, GuiTheme theme) {
        if (customPrefixes.isEmpty()) {
            return theme.getChatPrefix();
        }

        String className = null;
        if (source != null) {
            className = source.getClass().getName();
        } else {
            boolean foundClass = false;
            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                if (foundClass) {
                    if (!element.getClassName().equals(MessageBuilderImpl.class.getName())) {
                        className = element.getClassName();
                        break;
                    }
                } else {
                    if (element.getClassName().equals(MessageBuilderImpl.class.getName())) {
                        foundClass = true;
                    }
                }
            }
        }

        if (className == null) {
            return theme.getChatPrefix();
        }

        for (Pair<String, Supplier<Text>> pair : customPrefixes) {
            if (className.startsWith(pair.getLeft())) {
                @Nullable Text prefix = pair.getRight().get();
                return prefix != null ? prefix : theme.getChatPrefix();
            }
        }

        return theme.getChatPrefix();
    }
}
