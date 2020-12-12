/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import it.unimi.dsi.fastutil.chars.Char2CharArrayMap;
import it.unimi.dsi.fastutil.chars.Char2CharMap;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.Config;
import minegame159.meteorclient.commands.commands.Ignore;
import minegame159.meteorclient.events.SendMessageEvent;
import minegame159.meteorclient.friends.Friend;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.mixininterface.IChatHudLine;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.ChatUtil;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BetterChat extends ToggleModule {
    // Ignore
    private final SettingGroup sgIgnore = settings.createGroup("Ignore");

    private final Setting<Boolean> ignoreEnabled = sgIgnore.add(new BoolSetting.Builder()
            .name("ignore-enabled")
            .description("Ignores player defined by .ignore command.")
            .defaultValue(true)
            .build()
    );

    // Anti Spam
    private final SettingGroup sgAntiSpam = settings.createGroup("Anti Spam");

    private final Setting<Boolean> antiSpamEnabled = sgAntiSpam.add(new BoolSetting.Builder()
            .name("anti-spam-enabled")
            .description("Enables anti spam.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> antiSpamDepth = sgAntiSpam.add(new IntSetting.Builder()
            .name("anti-spam-depth")
            .description("How many messages to check for duplicates.")
            .defaultValue(4)
            .min(1)
            .sliderMin(1)
            .build()
    );

    private final Setting<Boolean> antiSpamMoveToBottom = sgAntiSpam.add(new BoolSetting.Builder()
            .name("anti-spam-move-to-bottom")
            .description("Move duplicate messages to bottom.")
            .defaultValue(true)
            .build()
    );

    // Longer Chat
    private final SettingGroup sgLongerChat = settings.createGroup("Longer Chat");

    private final Setting<Boolean> longerChatEnabled = sgLongerChat.add(new BoolSetting.Builder()
            .name("longer-chat-enabled")
            .description("Makes chat longer.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> longerChatLines = sgLongerChat.add(new IntSetting.Builder()
            .name("longer-chat-lines")
            .description("Chat lines.")
            .defaultValue(1000)
            .min(100)
            .sliderMax(1000)
            .build()
    );

    private final SettingGroup sgPrefix = settings.createGroup("Prefix");

    private final Setting<Boolean> prefixEnabled = sgPrefix.add(new BoolSetting.Builder()
            .name("prefix-enabled")
            .description("Enables prefix.")
            .defaultValue(false)
            .build()
    );

    private final Setting<String> prefixText = sgPrefix.add(new StringSetting.Builder()
            .name("text")
            .description("Text to add.")
            .defaultValue("> ")
            .build()
    );

    private final Setting<Boolean> prefixSmallCaps = sgPrefix.add(new BoolSetting.Builder()
            .name("small-caps")
            .description("Uses small font.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> prefixRandom = sgPrefix.add(new BoolSetting.Builder()
            .name("random")
            .description("Example: <msg> (538)")
            .defaultValue(false)
            .build()
    );

    private final SettingGroup sgSuffix = settings.createGroup("Suffix");

    private final Setting<Boolean> suffixEnabled = sgSuffix.add(new BoolSetting.Builder()
            .name("suffix-enabled")
            .description("Enables suffix.")
            .defaultValue(true)
            .build()
    );


    private final Setting<String> suffixText = sgSuffix.add(new StringSetting.Builder()
            .name("text")
            .description("Text to add.")
            .defaultValue(" | Meteor on Crack!")
            .build()
    );

    private final Setting<Boolean> suffixSmallCaps = sgSuffix.add(new BoolSetting.Builder()
            .name("small-caps")
            .description("Uses small font.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> suffixRandom = sgSuffix.add(new BoolSetting.Builder()
            .name("random")
            .description("Example: <msg> (538)")
            .defaultValue(false)
            .build()
    );

    // Friend Color
    /*private final SettingGroup sgFriendColor = settings.createGroup("Friend Color");

    private final Setting<Boolean> friendColorEnabled = sgFriendColor.add(new BoolSetting.Builder()
            .name("friend-color-enabled")
            .description("Highlights friends with color in chat.")
            .defaultValue(true)
            .build()
    );*/

    private boolean skipMessage;

    private static final Char2CharMap SMALL_CAPS = new Char2CharArrayMap();

    static {
        String[] a = "abcdefghijklmnopqrstuvwxyz".split("");
        String[] b = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴩqʀꜱᴛᴜᴠᴡxyᴢ".split("");
        for (int i = 0; i < a.length; i++) SMALL_CAPS.put(a[i].charAt(0), b[i].charAt(0));
    }

    private final StringBuilder sb = new StringBuilder();


    public BetterChat() {
        super(Category.Misc, "better-chat", "Improves chat in many ways.");
    }

    public boolean onMsg(String message, int messageId, int timestamp, List<ChatHudLine<Text>> messages, List<ChatHudLine<OrderedText>> visibleMessages) {
        if (!isActive() || skipMessage) return false;

        if (ignoreEnabled.get() && ignoreOnMsg(message)) return true;
        return antiSpamEnabled.get() && antiSpamOnMsg(message, messageId, timestamp, messages, visibleMessages);
        //return friendColorEnabled.get() && friendColorOnMsg(message);
    }

    @EventHandler
    private final Listener<SendMessageEvent> onSendMessage = new Listener<>(event -> {
        if (!event.msg.startsWith(Config.INSTANCE.getPrefix() + "b")) {
            event.msg = getPrefix() + event.msg + getSuffix();
        }
    });

    // IGNORE

    private boolean ignoreOnMsg(String message) {
        for (String name : Ignore.ignoredPlayers) {
            if (message.contains("<" + name + ">")) {
                return true;
            }
        }

        return false;
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

            ((IChatHudLine<Text>) msg).setText(new LiteralText(msgString));
            ((IChatHudLine<Text>) msg).setTimestamp(newTimestamp);
            ((IChatHudLine<Text>) msg).setId(newId);

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

                    ((IChatHudLine) msg).setText(new LiteralText(msgString));
                    ((IChatHudLine) msg).setTimestamp(newTimestamp);
                    ((IChatHudLine) msg).setId(newId);

                    return true;
                }
            }

            return false;
        }
    }

    // LONGER CHAT

    public boolean isLongerChat() {
        return longerChatEnabled.get();
    }

    public int getChatLength() {
        return longerChatLines.get();
    }

    // FRIEND COLOR

    private boolean friendColorOnMsg(String message) {
        List<Friend> friends = FriendManager.INSTANCE.getAll();
        boolean hadFriends = false;

        for (Friend friend : friends) {
            if (message.contains(friend.name)) {
                message = message.replaceAll(friend.name, "§d" + friend.name + "§r");
                hadFriends = true;
            }
        }

        if (hadFriends) {
            skipMessage = true;
            Utils.sendMessage(message);
            skipMessage = false;

            return true;
        }

        return false;
    }

    // PREFIX/SUFFIX

    private String getPrefix() {
        String text;

        if (prefixEnabled.get()) {
            if (prefixRandom.get()) {
                text = String.format("(%03d) ", Utils.random(0, 1000));
            } else {
                text = prefixText.get();

                if (prefixSmallCaps.get()) {
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

    private String getSuffix() {
        String text;

        if (suffixEnabled.get()) {
            if (suffixRandom.get()) {
                text = String.format(" (%03d)", Utils.random(0, 1000));
            } else {
                text = suffixText.get();

                if (suffixSmallCaps.get()) {
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
}
