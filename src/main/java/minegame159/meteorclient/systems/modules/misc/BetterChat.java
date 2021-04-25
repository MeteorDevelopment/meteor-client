/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.misc;

import it.unimi.dsi.fastutil.chars.Char2CharArrayMap;
import it.unimi.dsi.fastutil.chars.Char2CharMap;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.player.SendMessageEvent;
import minegame159.meteorclient.mixin.ChatHudLineAccessor;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.commands.Commands;
import minegame159.meteorclient.systems.commands.commands.SayCommand;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.*;
import net.minecraft.util.ChatUtil;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BetterChat extends Module {
    private final SettingGroup sgAnnoy = settings.createGroup("Annoy");
    private final SettingGroup sgAntiSpam = settings.createGroup("Anti Spam");
    private final SettingGroup sgChatProtect = settings.createGroup("Chat Protect");
    private final SettingGroup sgFancyChat = settings.createGroup("Fancy Chat");
    private final SettingGroup sgLongerChat = settings.createGroup("Longer Chat");
    private final SettingGroup sgPrefix = settings.createGroup("Prefix");
    private final SettingGroup sgSuffix = settings.createGroup("Suffix");

    // Annoy

    private final Setting<Boolean> annoyEnabled = sgAnnoy.add(new BoolSetting.Builder()
            .name("annoy-enabled")
            .description("Makes your messages aNnOyInG.")
            .defaultValue(false)
            .build()
    );

    // Anti Spam

    private final Setting<Boolean> antiSpamEnabled = sgAntiSpam.add(new BoolSetting.Builder()
            .name("anti-spam-enabled")
            .description("Enables the anti-spam.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> antiSpamDepth = sgAntiSpam.add(new IntSetting.Builder()
            .name("depth")
            .description("How many chat messages to check for duplicate messages.")
            .defaultValue(4)
            .min(1)
            .sliderMin(1)
            .build()
    );

    private final Setting<Boolean> antiSpamMoveToBottom = sgAntiSpam.add(new BoolSetting.Builder()
            .name("move-to-bottom")
            .description("Moves any duplicate messages to the bottom of the chat.")
            .defaultValue(true)
            .build()
    );

    // Chat Protect

    private final Setting<Boolean> coordsProtectionEnabled = sgChatProtect.add(new BoolSetting.Builder()
            .name("coords-protection-enabled")
            .description("Prevents you from sending messages in chat that may contain coordinates.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableAllMessages = sgChatProtect.add(new BoolSetting.Builder()
            .name("disable-all-messages")
            .description("Prevents you from essentially being able to send messages in chat.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> disableButton = sgChatProtect.add(new BoolSetting.Builder()
            .name("disable-button")
            .description("Adds a button to the warning to send a message to the chat in any way.")
            .defaultValue(true)
            .build()
    );

    // Fancy Chat

    private final Setting<Boolean> fancyEnabled = sgFancyChat.add(new BoolSetting.Builder()
            .name("fancy-chat-enabled")
            .description("Makes your messages fancy!")
            .defaultValue(false)
            .build()
    );

    // Longer Chat

    private final Setting<Boolean> infiniteChatBox = sgLongerChat.add(new BoolSetting.Builder()
            .name("infinite-chat-box")
            .description("Lets you type infinitely long messages.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> longerChatEnabled = sgLongerChat.add(new BoolSetting.Builder()
            .name("longer-chat-enabled")
            .description("Extends chat length.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> longerChatLines = sgLongerChat.add(new IntSetting.Builder()
            .name("extra-lines")
            .description("The amount of extra chat lines.")
            .defaultValue(1000)
            .min(100)
            .sliderMax(1000)
            .build()
    );

    // Prefix

    private final Setting<Boolean> prefixEnabled = sgPrefix.add(new BoolSetting.Builder()
            .name("prefix-enabled")
            .description("Enables a prefix.")
            .defaultValue(false)
            .build()
    );

    private final Setting<String> prefixText = sgPrefix.add(new StringSetting.Builder()
            .name("text")
            .description("The text to add as your prefix.")
            .defaultValue("> ")
            .build()
    );

    private final Setting<Boolean> prefixSmallCaps = sgPrefix.add(new BoolSetting.Builder()
            .name("small-caps")
            .description("Uses a small font.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> prefixRandom = sgPrefix.add(new BoolSetting.Builder()
            .name("random-number")
            .description("Example: <msg> (538)")
            .defaultValue(false)
            .build()
    );

    // Suffix

    private final Setting<Boolean> suffixEnabled = sgSuffix.add(new BoolSetting.Builder()
            .name("suffix-enabled")
            .description("Enables a suffix.")
            .defaultValue(false)
            .build()
    );


    private final Setting<String> suffixText = sgSuffix.add(new StringSetting.Builder()
            .name("text")
            .description("The text to add as your suffix.")
            .defaultValue(" | Meteor on Crack!")
            .build()
    );

    private final Setting<Boolean> suffixSmallCaps = sgSuffix.add(new BoolSetting.Builder()
            .name("small-caps")
            .description("Uses a small font.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> suffixRandom = sgSuffix.add(new BoolSetting.Builder()
            .name("random")
            .description("Example: <msg> (538)")
            .defaultValue(false)
            .build()
    );

    private boolean skipMessage;

    private static final Char2CharMap SMALL_CAPS = new Char2CharArrayMap();

    static {
        String[] a = "abcdefghijklmnopqrstuvwxyz".split("");
        String[] b = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴩqʀꜱᴛᴜᴠᴡxyᴢ".split("");
        for (int i = 0; i < a.length; i++) SMALL_CAPS.put(a[i].charAt(0), b[i].charAt(0));
    }

    private final StringBuilder sb = new StringBuilder();


    public BetterChat() {
        super(Categories.Misc, "better-chat", "Improves your chat experience in various ways.");
    }

    public boolean onMsg(String message, int messageId, int timestamp, List<ChatHudLine<Text>> messages, List<ChatHudLine<OrderedText>> visibleMessages) {
        if (!isActive() || skipMessage) return false;
        return antiSpamEnabled.get() && antiSpamOnMsg(message, messageId, timestamp, messages, visibleMessages);
    }

    @EventHandler
    private void onSendMessage(SendMessageEvent event) {
        String message = event.msg;

        if (annoyEnabled.get())
            message = applyAnnoy(message);

        if (fancyEnabled.get())
            message = applyFancy(message);

        message = getPrefix() + message + getSuffix();

        if (disableAllMessages.get()) {
            sendWarningMessage(message,
                    "You are trying to send a message to the global chat! ",
                    "Send your message to the global chat:");
            event.cancel();
            return;
        }

        if (coordsProtectionEnabled.get() && containsCoordinates(message)) {
            sendWarningMessage(message,
                    "It looks like there are coordinates in your message! ",
                    "Send your message to the global chat even if there are coordinates:");
            event.cancel();
            return;
        }

        event.msg = message;
    }

    // ANTI SPAM

    private boolean antiSpamOnMsg(String message, int messageId, int timestamp, List<ChatHudLine<Text>> messages, List<ChatHudLine<OrderedText>> visibleMessages) {
        message = ChatUtil.stripTextFormat(message);

        for (int i = 0; i < antiSpamDepth.get(); i++) {
            if (antiSpamCheckMsg(visibleMessages, message, timestamp, messageId, i)) {
                if (antiSpamMoveToBottom.get() && i != 0) {
                    ChatHudLine msg = visibleMessages.remove(i);
                    visibleMessages.add(0, msg);
                    messages.add(0, msg);
                }

                return true;
            }
        }

        return false;
    }

    private boolean antiSpamCheckMsg(List<ChatHudLine<OrderedText>> visibleMessages, String newMsg, int newTimestamp, int newId, int msgI) {
        ChatHudLine<OrderedText> msg = visibleMessages.size() > msgI ? visibleMessages.get(msgI) : null;
        if (msg == null) return false;
        String msgString = msg.getText().toString();

        if (ChatUtil.stripTextFormat(msgString).equals(newMsg)) {
            msgString += Formatting.GRAY + " (2)";

            ((ChatHudLineAccessor<Text>) msg).setText(new LiteralText(msgString));
            ((ChatHudLineAccessor<Text>) msg).setTimestamp(newTimestamp);
            ((ChatHudLineAccessor<Text>) msg).setId(newId);

            return true;
        } else {
            Matcher matcher = Pattern.compile(".*(\\([0-9]+\\)$)").matcher(msgString);

            if (matcher.matches()) {
                String group = matcher.group(1);
                int number = Integer.parseInt(group.substring(1, group.length() - 1));

                int i = msgString.lastIndexOf(group);
                msgString = msgString.substring(0, i - Formatting.GRAY.toString().length() - 1);

                if (ChatUtil.stripTextFormat(msgString).equals(newMsg)) {
                    msgString += Formatting.GRAY + " (" + (number + 1) + ")";

                    ((ChatHudLineAccessor) msg).setText(new LiteralText(msgString));
                    ((ChatHudLineAccessor) msg).setTimestamp(newTimestamp);
                    ((ChatHudLineAccessor) msg).setId(newId);

                    return true;
                }
            }

            return false;
        }
    }

    // LONGER CHAT

    public boolean isInfiniteChatBox() {
        return isActive() && infiniteChatBox.get();
    }

    public boolean isLongerChat() {
        return longerChatEnabled.get();
    }

    public int getChatLength() {
        return longerChatLines.get();
    }

    // ANNOY

    private String applyAnnoy(String message) {
        StringBuilder sb = new StringBuilder(message.length());
        boolean upperCase = true;
        for (int cp : message.codePoints().toArray()) {
            if (upperCase) sb.appendCodePoint(Character.toUpperCase(cp));
            else sb.appendCodePoint(Character.toLowerCase(cp));
            upperCase = !upperCase;
        }
        message = sb.toString();
        return message;
    }

    // FANCY CHAT

    private String applyFancy(String changeFrom) {
        String output = changeFrom;
        sb.setLength(0);

        for (char ch : output.toCharArray()) {
            if (SMALL_CAPS.containsKey(ch)) sb.append(SMALL_CAPS.get(ch));
            else sb.append(ch);
        }

        output = sb.toString();

        return output;
    }

    // PREFIX/SUFFIX

    private String getAffix(Setting<Boolean> affixEnabled, Setting<Boolean> affixRandom, String affixRandomFormat, Setting<String> affixText, Setting<Boolean> affixSmallCaps) {
        String text;

        if (affixEnabled.get()) {
            if (affixRandom.get()) {
                text = String.format(affixRandomFormat, Utils.random(0, 1000));
            } else {
                text = affixText.get();

                if (affixSmallCaps.get()) {
                    sb.setLength(0);

                    for (char ch : text.toCharArray()) {
                        if (SMALL_CAPS.containsKey(ch)) sb.append(SMALL_CAPS.get(ch));
                        else sb.append(ch);
                    }

                    text = sb.toString();
                }
            }
        } else text = "";

        return text;
    }

    private String getPrefix() {
        return getAffix(prefixEnabled, prefixRandom, "(%03d) ", prefixText, prefixSmallCaps);
    }

    private String getSuffix() {
        return getAffix(suffixEnabled, suffixRandom, " (%03d)", suffixText, suffixSmallCaps);
    }

    // PROTECTION

    private boolean containsCoordinates(String message) {
        return message.matches(".*(?<x>-?\\d{3,}(?:\\.\\d*)?)(?:\\s+(?<y>\\d{1,3}(?:\\.\\d*)?))?\\s+(?<z>-?\\d{3,}(?:\\.\\d*)?).*");
    }

    private BaseText getSendButton(String message, String hint) {
        BaseText sendButton = new LiteralText("[SEND ANYWAY]");
        BaseText hintBaseText = new LiteralText("");

        BaseText hintMsg = new LiteralText(hint);
        hintMsg.setStyle(hintBaseText.getStyle().withFormatting(Formatting.GRAY));
        hintBaseText.append(hintMsg);

        hintBaseText.append(new LiteralText('\n' + message));

        sendButton.setStyle(sendButton.getStyle()
                .withFormatting(Formatting.DARK_RED)
                .withClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        Commands.get().get(SayCommand.class).toString(message)
                ))
                .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        hintBaseText
                )));
        return sendButton;
    }

    private void sendWarningMessage(String message, String title, String hint) {
        BaseText warningMessage = new LiteralText(title);

        if (disableButton.get()) {
            BaseText sendButton = getSendButton(message, hint);
            warningMessage.append(sendButton);
        }

        ChatUtils.info("Warning", warningMessage);
    }
}