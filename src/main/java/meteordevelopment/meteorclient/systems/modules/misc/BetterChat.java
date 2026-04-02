/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.chars.Char2CharMap;
import it.unimi.dsi.fastutil.chars.Char2CharOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.mixin.ChatComponentAccessor;
import meteordevelopment.meteorclient.mixininterface.IGuiMessage;
import meteordevelopment.meteorclient.mixininterface.IGuiMessageVisible;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.text.MeteorClickEvent;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.UnknownNullability;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class BetterChat extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgFilter = settings.createGroup("Filter");
    private final SettingGroup sgLongerChat = settings.createGroup("Longer Chat");
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
        .description("Adds client-side time stamps to the beginning of chat messages.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> showSeconds = sgGeneral.add(new BoolSetting.Builder()
        .name("show-seconds")
        .description("Shows seconds in the chat message timestamps")
        .defaultValue(false)
        .visible(timestamps::get)
        .onChanged(o -> updateDateFormat())
        .build()
    );

    private final Setting<Boolean> playerHeads = sgGeneral.add(new BoolSetting.Builder()
        .name("player-heads")
        .description("Displays player heads next to their messages.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> coordsProtection = sgGeneral.add(new BoolSetting.Builder()
        .name("coords-protection")
        .description("Prevents you from sending messages in chat that may contain coordinates.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> keepHistory = sgGeneral.add(new BoolSetting.Builder()
        .name("keep-history")
        .description("Prevents the chat history from being cleared when disconnecting.")
        .defaultValue(true)
        .build()
    );

    // Filter

    private final Setting<Boolean> antiSpam = sgFilter.add(new BoolSetting.Builder()
        .name("anti-spam")
        .description("Blocks duplicate messages from filling your chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> antiSpamDepth = sgFilter.add(new IntSetting.Builder()
        .name("depth")
        .description("How many messages to filter.")
        .defaultValue(20)
        .min(1)
        .sliderMin(1)
        .visible(antiSpam::get)
        .build()
    );

    private final Setting<Boolean> antiClear = sgFilter.add(new BoolSetting.Builder()
        .name("anti-clear")
        .description("Prevents servers from clearing chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> filterRegex = sgFilter.add(new BoolSetting.Builder()
        .name("filter-regex")
        .description("Filter out chat messages that match the regex filter.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<String>> regexFilters = sgFilter.add(new StringListSetting.Builder()
        .name("regex-filter")
        .description("Regex filter used for filtering chat messages.")
        .visible(filterRegex::get)
        .onChanged(strings -> compileFilterRegexList())
        .build()
    );


    // Longer chat

    private final Setting<Boolean> infiniteChatBox = sgLongerChat.add(new BoolSetting.Builder()
        .name("infinite-chat-box")
        .description("Lets you type infinitely long messages.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> longerChatHistory = sgLongerChat.add(new BoolSetting.Builder()
        .name("longer-chat-history")
        .description("Extends chat length.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> longerChatLines = sgLongerChat.add(new IntSetting.Builder()
        .name("extra-lines")
        .description("The amount of extra chat lines.")
        .defaultValue(1000)
        .min(0)
        .sliderRange(0, 1000)
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

    private static final Pattern antiSpamRegex = Pattern.compile(" \\(([0-9]{1,9})\\)$");
    private static final Pattern antiClearRegex = Pattern.compile("\\n(\\n|\\s)+\\n");
    private static final Pattern timestampRegex = Pattern.compile("^(<[0-9]{2}:[0-9]{2}(?::[0-9]{2})?> )");
    private static final Pattern usernameRegex = Pattern.compile("^(?:<[0-9]{2}:[0-9]{2}>\\s)?<(.*?)>.*");

    private final Char2CharMap SMALL_CAPS = new Char2CharOpenHashMap();
    public final IntList lines = new IntArrayList();

    public BetterChat() {
        super(Categories.Misc, "better-chat", "Improves your chat experience in various ways.");

        String[] a = "abcdefghijklmnopqrstuvwxyz".split("");
        String[] b = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴩqʀꜱᴛᴜᴠᴡxyᴢ".split("");
        for (int i = 0; i < a.length; i++) SMALL_CAPS.put(a[i].charAt(0), b[i].charAt(0));
        compileFilterRegexList();
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        Component message = event.getMessage();

        if (filterRegex.get()) {
            String messageString = message.getString();
            for (Pattern pattern : filterRegexList) {
                if (pattern.matcher(messageString).find()) {
                    event.cancel();
                    return;
                }
            }
        }

        if (antiClear.get()) {
            String messageString = message.getString();
            if (antiClearRegex.matcher(messageString).find()) {
                MutableComponent newMessage = Component.empty();
                message.visit((style, string) -> {
                    Matcher antiClearMatcher = antiClearRegex.matcher(string);
                    newMessage.append(Component.literal(
                        antiClearMatcher.find() ? antiClearMatcher.replaceAll("\n\n") : string
                    ).setStyle(style));

                    return Optional.empty();
                }, Style.EMPTY);
                message = newMessage;
            }
        }

        if (antiSpam.get()) {
            MutableComponent antiSpammed = appendAntiSpam(message);

            if (antiSpammed != null) {
                message = antiSpammed;
            }
        }

        if (timestamps.get()) {
            MutableComponent timestamp = Component.literal("<" + dateFormat.format(new Date()) + "> ").withStyle(ChatFormatting.GRAY);

            message = Component.empty().append(timestamp).append(message);
        }

        event.setMessage(message);
    }

    @EventHandler
    private void onMessageSend(SendMessageEvent event) {
        String message = event.message;

        if (annoy.get()) message = applyAnnoy(message);

        if (fancy.get()) message = applyFancy(message);

        message = getPrefix() + message + getSuffix();

        if (coordsProtection.get() && containsCoordinates(message)) {
            MutableComponent warningMessage = Component.literal("It looks like there are coordinates in your message! ");

            MutableComponent sendButton = getSendButton(message);
            warningMessage.append(sendButton);

            ChatUtils.sendMsg(warningMessage);

            event.cancel();
            return;
        }

        event.message = message;
    }

    // Anti Spam

    private MutableComponent appendAntiSpam(@UnknownNullability Component text) {
        String textString = text.getString();
        MutableComponent returnText = null;
        int messageIndex = -1;

        List<GuiMessage> messages = ((ChatComponentAccessor) mc.gui.getChat()).meteor$getAllMessages();
        if (messages.isEmpty()) return null;

        for (int i = 0; i < Math.min(antiSpamDepth.get(), messages.size()); i++) {
            String stringToCheck = messages.get(i).content().getString();

            Matcher timestampMatcher = timestampRegex.matcher(stringToCheck);
            if (timestampMatcher.find()) {
                stringToCheck = stringToCheck.substring(timestampMatcher.end());
            }

            if (textString.equals(stringToCheck)) {
                messageIndex = i;
                returnText = text.copy().append(Component.literal(" (2)").withStyle(ChatFormatting.GRAY));
                break;
            } else {
                Matcher matcher = antiSpamRegex.matcher(stringToCheck);
                if (!matcher.find()) continue;

                String group = matcher.group(matcher.groupCount());
                int number = Integer.parseInt(group);

                if (stringToCheck.substring(0, matcher.start()).equals(textString)) {
                    messageIndex = i;
                    returnText = text.copy().append(Component.literal(" (" + (number + 1) + ")").withStyle(ChatFormatting.GRAY));
                    break;
                }
            }
        }

        if (returnText != null) {
            List<GuiMessage.Line> visible = ((ChatComponentAccessor) mc.gui.getChat()).meteor$getTrimmedMessages();

            int start = -1;
            for (int i = 0; i < messageIndex; i++) {
                start += lines.getInt(i);
            }

            int i = lines.getInt(messageIndex);
            while (i > 0) {
                visible.remove(start + 1);
                i--;
            }

            messages.remove(messageIndex);
            lines.removeInt(messageIndex);
        }

        return returnText;
    }

    public void removeLine(int index) {
        if (index >= lines.size()) {
            if (antiSpam.get()) {
                error("Issue detected with the anti-spam system! Likely a compatibility issue with another mod. Disabling anti-spam to protect chat integrity.");
                antiSpam.set(false);
            }

            return;
        }

        lines.removeInt(index);
    }

    // Player Heads

    private record CustomHeadEntry(String prefix, Identifier texture) {
    }

    private static final List<CustomHeadEntry> CUSTOM_HEAD_ENTRIES = new ArrayList<>();

    private static final Pattern TIMESTAMP_REGEX = Pattern.compile("^<\\d{1,2}:\\d{1,2}>");

    public GuiMessage.Line line;


    /**
     * Registers a custom player head to render based on a message prefix
     */
    public static void registerCustomHead(String prefix, Identifier texture) {
        CUSTOM_HEAD_ENTRIES.add(new CustomHeadEntry(prefix, texture));
    }

    static {
        registerCustomHead("[Meteor]", MeteorClient.identifier("textures/icons/chat/meteor.png"));
        registerCustomHead("[Baritone]", MeteorClient.identifier("textures/icons/chat/baritone.png"));
    }

    public int modifyChatWidth(int width) {
        if (isActive() && playerHeads.get()) return width + 10;
        return width;
    }


    public void beforeDrawMessage(GuiGraphicsExtractor graphics, int y, int color) {
        if (!isActive() || !playerHeads.get() || line == null) return;

        // Only draw the first line of multi line messages
        if (((IGuiMessageVisible) (Object) line).meteor$isStartOfEntry()) {
            drawTexture(graphics, (IGuiMessage) (Object) line, y, color);
        }
    }

    public void afterDrawMessage() {
        if (!isActive() || !playerHeads.get()) return;

        line = null;
    }

    private void drawTexture(GuiGraphicsExtractor graphics, IGuiMessage line, int y, int color) {
        String text = line.meteor$getText().trim();

        // Custom
        int startOffset = 0;

        try {
            Matcher m = TIMESTAMP_REGEX.matcher(text);
            if (m.find()) startOffset = m.end() + 1;
        } catch (IllegalStateException ignored) {
        }

        for (CustomHeadEntry entry : CUSTOM_HEAD_ENTRIES) {
            // Check prefix
            if (text.startsWith(entry.prefix(), startOffset)) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, entry.texture(), 0, y, 0, 0, 8, 8, 64, 64, 64, 64, color);
                return;
            }
        }

        // Player
        GameProfile sender = getSender(line, text);
        if (sender == null) return;

        PlayerInfo entry = mc.getConnection().getPlayerInfo(sender.id());
        if (entry == null) return;

        PlayerFaceExtractor.extractRenderState(graphics, entry.getSkin(), 0, y, 8, color);
    }

    private GameProfile getSender(IGuiMessage line, String text) {
        GameProfile sender = line.meteor$getSender();

        // If the packet did not contain a sender field then try to get the sender from the message
        if (sender == null) {
            Matcher usernameMatcher = usernameRegex.matcher(text);

            if (usernameMatcher.matches()) {
                String username = usernameMatcher.group(1);

                PlayerInfo entry = mc.getConnection().getPlayerInfo(username);
                if (entry != null) sender = entry.getProfile();
            }
        }

        return sender;
    }

    // Timestamps

    private SimpleDateFormat dateFormat;

    private void updateDateFormat() {
        dateFormat = new SimpleDateFormat(showSeconds.get() ? "HH:mm:ss" : "HH:mm");
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
            sb.append(SMALL_CAPS.getOrDefault(ch, ch));
        }

        return sb.toString();
    }

    // Filter Regex

    private final List<Pattern> filterRegexList = new ArrayList<>();

    private void compileFilterRegexList() {
        filterRegexList.clear();

        for (int i = 0; i < regexFilters.get().size(); i++) {
            try {
                filterRegexList.add(Pattern.compile(regexFilters.get().get(i)));
            } catch (PatternSyntaxException e) {
                String removed = regexFilters.get().remove(i);
                error("Removing Invalid regex: %s", removed);
            }
        }
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

    private static final Pattern coordRegex = Pattern.compile("(?<x>-?\\d{3,}(?:\\.\\d*)?)(?:\\s+(?<y>-?\\d{1,3}(?:\\.\\d*)?))?\\s+(?<z>-?\\d{3,}(?:\\.\\d*)?)");

    private boolean containsCoordinates(String message) {
        return coordRegex.matcher(message).find();
    }

    private MutableComponent getSendButton(String message) {
        MutableComponent sendButton = Component.literal("[SEND ANYWAY]");
        MutableComponent hintBaseText = Component.literal("");

        MutableComponent hintMsg = Component.literal("Send your message to the global chat even if there are coordinates:");
        hintMsg.setStyle(hintBaseText.getStyle().applyFormat(ChatFormatting.GRAY));
        hintBaseText.append(hintMsg);

        hintBaseText.append(Component.literal('\n' + message));

        sendButton.setStyle(sendButton.getStyle()
            .applyFormat(ChatFormatting.DARK_RED)
            .withClickEvent(new MeteorClickEvent(Commands.get("say").toString(message)))
            .withHoverEvent(new HoverEvent.ShowText(
                hintBaseText
            )));
        return sendButton;
    }

    // Longer chat

    public boolean isInfiniteChatBox() {
        return isActive() && infiniteChatBox.get();
    }

    public boolean isLongerChat() {
        return isActive() && longerChatHistory.get();
    }

    public boolean keepHistory() {
        return isActive() && keepHistory.get();
    }

    public int getExtraChatLines() {
        return longerChatLines.get();
    }
}
