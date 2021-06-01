/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.misc;

import it.unimi.dsi.fastutil.chars.Char2CharArrayMap;
import it.unimi.dsi.fastutil.chars.Char2CharMap;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.game.ReceiveMessageEvent;
import minegame159.meteorclient.events.game.SendMessageEvent;
import minegame159.meteorclient.mixin.ChatHudAccessor;
import minegame159.meteorclient.mixininterface.IChatHud;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.commands.Commands;
import minegame159.meteorclient.systems.commands.commands.SayCommand;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BetterChat extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPrefix = settings.createGroup("Prefix");
    private final SettingGroup sgSuffix = settings.createGroup("Suffix");

    private final Setting<Boolean> annoy = sgGeneral.add(new BoolSetting.Builder()
            .name("annoy")
            .description("Makes your messages aNnOyInG.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> fancy = sgGeneral.add(new BoolSetting.Builder()
            .name("fancy-chat")
            .description("Makes your messages ғᴀɴᴄʏ!")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> timestamps = sgGeneral.add(new BoolSetting.Builder()
            .name("timestamps")
            .description("Adds client side time stamps to the beginning of chat messages.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> antiSpam = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-spam")
            .description("Blocks duplicate messages from filling your chat.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> antiSpamDepth = sgGeneral.add(new IntSetting.Builder()
            .name("depth")
            .description("How many messages to check for duplicate messages.")
            .defaultValue(20)
            .min(1)
            .sliderMin(1)
            .visible(antiSpam::get)
            .build()
    );

    private final Setting<Boolean> coordsProtection = sgGeneral.add(new BoolSetting.Builder()
            .name("coords-protection")
            .description("Prevents you from sending messages in chat that may contain coordinates.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> infiniteChatBox = sgGeneral.add(new BoolSetting.Builder()
            .name("infinite-chat-box")
            .description("Lets you type infinitely long messages.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> longerChatHistory = sgGeneral.add(new BoolSetting.Builder()
            .name("longer-chat-history")
            .description("Extends chat length.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> longerChatLines = sgGeneral.add(new IntSetting.Builder()
            .name("extra-lines")
            .description("The amount of extra chat lines.")
            .defaultValue(1000)
            .min(100)
            .sliderMax(1000)
            .visible(longerChatHistory::get)
            .build()
    );

    // Prefix

    private final Setting<Boolean> prefix = sgPrefix.add(new BoolSetting.Builder()
            .name("prefix")
            .description("Adds a prefix to your chat messages.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> prefixRandom = sgPrefix.add(new BoolSetting.Builder()
            .name("random")
            .description("Uses a random number as your prefix.")
            .defaultValue(false)
            .build()
    );

    private final Setting<String> prefixText = sgPrefix.add(new StringSetting.Builder()
            .name("text")
            .description("The text to add as your prefix.")
            .defaultValue("> ")
            .visible(() -> !prefixRandom.get())
            .build()
    );

    private final Setting<Boolean> prefixSmallCaps = sgPrefix.add(new BoolSetting.Builder()
            .name("small-caps")
            .description("Uses small caps in the prefix.")
            .defaultValue(false)
            .visible(() -> !prefixRandom.get())
            .build()
    );

    // Suffix

    private final Setting<Boolean> suffix = sgSuffix.add(new BoolSetting.Builder()
            .name("suffix")
            .description("Adds a suffix to your chat messages.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> suffixRandom = sgSuffix.add(new BoolSetting.Builder()
            .name("random")
            .description("Uses a random number as your suffix.")
            .defaultValue(false)
            .build()
    );

    private final Setting<String> suffixText = sgSuffix.add(new StringSetting.Builder()
            .name("text")
            .description("The text to add as your suffix.")
            .defaultValue(" | meteor on crack!")
            .visible(() -> !suffixRandom.get())
            .build()
    );

    private final Setting<Boolean> suffixSmallCaps = sgSuffix.add(new BoolSetting.Builder()
            .name("small-caps")
            .description("Uses small caps in the suffix.")
            .defaultValue(true)
            .visible(() -> !suffixRandom.get())
            .build()
    );

    private final Char2CharMap SMALL_CAPS = new Char2CharArrayMap();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");

    public BetterChat() {
        super(Categories.Misc, "better-chat", "Improves your chat experience in various ways.");

        String[] a = "abcdefghijklmnopqrstuvwxyz".split("");
        String[] b = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴩqʀꜱᴛᴜᴠᴡxyᴢ".split("");
        for (int i = 0; i < a.length; i++) SMALL_CAPS.put(a[i].charAt(0), b[i].charAt(0));
    }

    @EventHandler
    private void onMessageRecieve(ReceiveMessageEvent event) {
        ((ChatHudAccessor) mc.inGameHud.getChatHud()).getVisibleMessages().removeIf((message) -> message.getId() == event.id && event.id != 0);
        ((ChatHudAccessor) mc.inGameHud.getChatHud()).getMessages().removeIf((message) -> message.getId() == event.id && event.id != 0);

        Text message = event.message;

        if (timestamps.get()) {
            Matcher matcher = Pattern.compile("^(<[0-9]{2}:[0-9]{2}>\\s)").matcher(message.getString());
            if (matcher.matches()) message.getSiblings().subList(0, 8).clear();

            Text timestamp = new LiteralText("<" + dateFormat.format(new Date()) + "> ").formatted(Formatting.GRAY);

            message = new LiteralText("").append(timestamp).append(message);
        }

        if (antiSpam.get()) {
            for (int i = 0; i < antiSpamDepth.get(); i++) {
                Text antiSpammed = appendAntiSpam(message, i);
                if (antiSpammed != null) {
                    message = antiSpammed;
                    ((ChatHudAccessor) mc.inGameHud.getChatHud()).getMessages().remove(i);
                    ((ChatHudAccessor) mc.inGameHud.getChatHud()).getVisibleMessages().remove(i);
                }
            }
        }

        event.cancel();
        ((IChatHud) mc.inGameHud.getChatHud()).add(message, event.id, mc.inGameHud.getTicks(), false);
    }

    private Text appendAntiSpam(Text text, int index) {
        List<ChatHudLine<OrderedText>> visibleMessages = ((ChatHudAccessor) mc.inGameHud.getChatHud()).getVisibleMessages();
        if (visibleMessages.isEmpty() || index < 0 || index > visibleMessages.size() - 1) return null;

        ChatHudLine<OrderedText> visibleMessage = visibleMessages.get(index);

        LiteralText parsed = new LiteralText("");

        visibleMessage.getText().accept((i, style, codePoint) -> {
            parsed.append(new LiteralText(new String(Character.toChars(codePoint))).setStyle(style));
            return true;
        });

        String oldMessage = parsed.getString();
        String newMessage = text.getString();

        if (oldMessage.equals(newMessage)) {
            return parsed.append(new LiteralText(" (2)").formatted(Formatting.GRAY));
        }
        else {
            Matcher matcher = Pattern.compile(".*(\\([0-9]+\\)$)").matcher(oldMessage);

            if (!matcher.matches()) return null;

            String group = matcher.group(matcher.groupCount());
            int number = Integer.parseInt(group.substring(1, group.length() - 1));

            String counter = " (" + number + ")";

            if (oldMessage.substring(0, oldMessage.length() - counter.length()).equals(newMessage)) {
                for (int i = 0; i < counter.length(); i++) parsed.getSiblings().remove(parsed.getSiblings().size() - 1);
                return parsed.append(new LiteralText( " (" + (number + 1) + ")").formatted(Formatting.GRAY));
            }
        }

        return null;
    }

    @EventHandler
    private void onMessageSend(SendMessageEvent event) {
        String message = event.message;

        if (annoy.get()) message = applyAnnoy(message);

        if (fancy.get()) message = applyFancy(message);

        message = getPrefix() + message + getSuffix();

        if (coordsProtection.get() && containsCoordinates(message)) {
            BaseText warningMessage = new LiteralText("It looks like there are coordinates in your message! ");

            BaseText sendButton = getSendButton(message, "Send your message to the global chat even if there are coordinates:");
            warningMessage.append(sendButton);

            ChatUtils.sendMsg(warningMessage);

            event.cancel();
            return;
        }

        event.message = message;
    }

    // LONGER CHAT

    public boolean isInfiniteChatBox() {
        return isActive() && infiniteChatBox.get();
    }

    public boolean isLongerChat() {
        return isActive() && longerChatHistory.get();
    }

    public int getChatLength() {
        return longerChatLines.get();
    }

    // Annoy

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

    // Fancy

    private String applyFancy(String message) {
        StringBuilder sb = new StringBuilder();

        for (char ch : message.toCharArray()) {
            if (SMALL_CAPS.containsKey(ch)) sb.append(SMALL_CAPS.get(ch));
            else sb.append(ch);
        }

        return sb.toString();
    }

    // Prefix and Suffix

    private String getPrefix() {
        return prefix.get() ? getAffix(prefixText.get(), prefixSmallCaps.get(), prefixRandom.get()) : "";
    }

    private String getSuffix() {
        return suffix.get() ? getAffix(suffixText.get(), suffixSmallCaps.get(), suffixRandom.get()) : "";
    }

    private String getAffix(String text, boolean smallcaps, boolean random) {
        if (random) return String.format("(%03d) ", Utils.random(0, 1000));
        else if (smallcaps) return applyFancy(text);
        else return text;
    }

    // Coords Protection

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
}