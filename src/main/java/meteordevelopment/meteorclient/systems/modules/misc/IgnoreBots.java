/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Detects and hides chat-bot spam. Tuned for 2b2t, where chat is delivered as system
 * messages ({@code <name> message}) and the signed-chat sender is unavailable, so every
 * detector works by parsing the {@code <name>} out of the message text.
 */
public class IgnoreBots extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBehavioral = settings.createGroup("Behavioral");
    private final SettingGroup sgCadence = settings.createGroup("Inhuman Cadence");
    private final SettingGroup sgContent = settings.createGroup("Content Filter");
    private final SettingGroup sgLists = settings.createGroup("Lists");

    // General

    private final Setting<Action> action = sgGeneral.add(new EnumSetting.Builder<Action>()
        .name("action")
        .description("What to do with a message identified as a bot.")
        .defaultValue(Action.Hide)
        .build()
    );

    private final Setting<String> chatFormat = sgGeneral.add(new StringSetting.Builder()
        .name("chat-format")
        .description("Regex with named groups 'name' and 'msg' used to parse player chat. Lines that don't match (queue, death, broadcasts, system messages) are always left untouched.")
        .defaultValue("^(?:<\\d{1,2}:\\d{1,2}(?::\\d{1,2})?>\\s*)?<(?<name>[A-Za-z0-9_]{1,16})>\\s?(?<msg>.*)$")
        .onChanged(_ -> compilePattern())
        .build()
    );

    private final Setting<Boolean> whitelistFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("whitelist-friends")
        .description("Never treat your friends as bots.")
        .defaultValue(true)
        .build()
    );

    // Behavioral - Flood

    private final Setting<Boolean> flood = sgBehavioral.add(new BoolSetting.Builder()
        .name("flood")
        .description("Flags accounts that send too many messages in a short window.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> floodMaxMessages = sgBehavioral.add(new IntSetting.Builder()
        .name("flood-max-messages")
        .description("Maximum messages an account may send within the window before it is flagged.")
        .defaultValue(4)
        .min(1)
        .sliderRange(1, 20)
        .visible(flood::get)
        .build()
    );

    private final Setting<Double> floodWindow = sgBehavioral.add(new DoubleSetting.Builder()
        .name("flood-window")
        .description("Length of the flood window, in seconds.")
        .defaultValue(5)
        .min(0.5)
        .sliderRange(0.5, 30)
        .visible(flood::get)
        .build()
    );

    // Behavioral - Swarm

    private final Setting<Boolean> swarm = sgBehavioral.add(new BoolSetting.Builder()
        .name("coordinated-spam")
        .description("Flags the same message when it is sent by many different accounts at once (a bot swarm).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> swarmMinAccounts = sgBehavioral.add(new IntSetting.Builder()
        .name("swarm-min-accounts")
        .description("How many distinct accounts must send the same message within the window to flag it.")
        .defaultValue(3)
        .min(2)
        .sliderRange(2, 20)
        .visible(swarm::get)
        .build()
    );

    private final Setting<Double> swarmWindow = sgBehavioral.add(new DoubleSetting.Builder()
        .name("swarm-window")
        .description("Length of the swarm detection window, in seconds.")
        .defaultValue(10)
        .min(1)
        .sliderRange(1, 60)
        .visible(swarm::get)
        .build()
    );

    // Inhuman cadence

    private final Setting<Boolean> cadence = sgCadence.add(new BoolSetting.Builder()
        .name("inhuman-cadence")
        .description("Flags accounts that send long messages faster than a human could type them.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> minLength = sgCadence.add(new IntSetting.Builder()
        .name("min-length")
        .description("Only judge cadence for messages at least this many characters long.")
        .defaultValue(20)
        .min(1)
        .sliderRange(1, 100)
        .visible(cadence::get)
        .build()
    );

    private final Setting<Double> minInterval = sgCadence.add(new DoubleSetting.Builder()
        .name("min-interval")
        .description("Minimum plausible time between two long messages, in seconds. Shorter gaps are flagged.")
        .defaultValue(1.5)
        .min(0)
        .sliderRange(0, 10)
        .visible(cadence::get)
        .build()
    );

    private final Setting<Double> maxCharsPerSecond = sgCadence.add(new DoubleSetting.Builder()
        .name("max-chars-per-second")
        .description("Maximum plausible sustained typing speed. Faster than this is flagged.")
        .defaultValue(20)
        .min(1)
        .sliderRange(1, 100)
        .visible(cadence::get)
        .build()
    );

    // Content filter

    private final Setting<Boolean> blockLinks = sgContent.add(new BoolSetting.Builder()
        .name("block-links")
        .description("Flags messages containing URLs or Discord/Telegram invites.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> blockIps = sgContent.add(new BoolSetting.Builder()
        .name("block-ips")
        .description("Flags messages containing server IP addresses.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> blockZalgo = sgContent.add(new BoolSetting.Builder()
        .name("block-zalgo")
        .description("Flags messages with excessive combining/zalgo characters.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> maxCharRepeat = sgContent.add(new IntSetting.Builder()
        .name("max-char-repeat")
        .description("Flags a message when any character repeats at least this many times in a row. 0 disables.")
        .defaultValue(8)
        .min(0)
        .sliderRange(0, 30)
        .build()
    );

    private final Setting<List<String>> keywords = sgContent.add(new StringListSetting.Builder()
        .name("keyword-filter")
        .description("Flags messages containing any of these (case-insensitive) substrings.")
        .build()
    );

    // Lists

    private final Setting<List<String>> blacklist = sgLists.add(new StringListSetting.Builder()
        .name("blacklist")
        .description("Accounts whose messages are always hidden.")
        .build()
    );

    private final Setting<List<String>> whitelist = sgLists.add(new StringListSetting.Builder()
        .name("whitelist")
        .description("Accounts that are never treated as bots.")
        .build()
    );

    private final Setting<Boolean> rememberBots = sgLists.add(new BoolSetting.Builder()
        .name("remember-bots")
        .description("Once an account is flagged, keep hiding all of its messages (even harmless filler).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> cacheTtl = sgLists.add(new DoubleSetting.Builder()
        .name("cache-ttl")
        .description("How long a remembered bot stays flagged, in seconds. 0 keeps it until you disconnect.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 600)
        .visible(rememberBots::get)
        .build()
    );

    private static final Pattern LINK = Pattern.compile("(?i)(?:https?://|www\\.|discord\\.gg/|discord(?:app)?\\.com/invite/|t\\.me/|telegram\\.me/)\\S+|\\b[a-z0-9-]{2,}(?:\\.[a-z0-9-]{2,})*\\.(?:gg|net|com|org|xyz|club|top|shop|io|ru|site|store|link|invite)\\b");
    private static final Pattern IP = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d{1,5})?\\b");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]");

    private Pattern chatPattern;

    // Per-account sliding window of message times (flood)
    private final Map<String, Deque<Long>> floodTimes = new HashMap<>();
    // Normalized content -> senders + first-seen time (swarm)
    private final Map<String, SwarmEntry> swarmEntries = new HashMap<>();
    // Per-account last message time (cadence)
    private final Object2LongOpenHashMap<String> lastTime = new Object2LongOpenHashMap<>();
    // Learned bots -> time first flagged (for TTL)
    private final Object2LongOpenHashMap<String> botCache = new Object2LongOpenHashMap<>();

    public IgnoreBots() {
        super(Categories.Misc, "ignore-bots", "Detects and hides chat-bot spam. Tuned for 2b2t.");
        compilePattern();
    }

    @Override
    public void onDeactivate() {
        clearState();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        clearState();
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        if (chatPattern == null) return;

        Matcher matcher = chatPattern.matcher(event.getMessage().getString());
        if (!matcher.matches()) return; // Not player chat - leave it alone

        String name;
        String msg;
        try {
            name = matcher.group("name");
            msg = matcher.group("msg");
        } catch (IllegalArgumentException _) {
            return; // Custom regex without the required groups
        }
        if (name == null) return;
        if (msg == null) msg = "";

        String key = name.toLowerCase(Locale.ROOT);

        // Never touch yourself, friends or whitelisted accounts
        if (isWhitelisted(name)) return;

        long now = System.currentTimeMillis();

        // Already known bot
        if (rememberBots.get() && isCachedBot(key, now)) {
            hide(event);
            return;
        }

        // Manual blacklist
        if (contains(blacklist.get(), name)) {
            flag(key, now);
            hide(event);
            return;
        }

        boolean bot = false;
        if (flood.get() && checkFlood(key, now)) bot = true;
        if (swarm.get() && checkSwarm(key, msg, now)) bot = true;
        if (cadence.get() && checkCadence(key, msg, now)) bot = true; // also updates lastTime
        if (checkContent(msg)) bot = true;

        if (bot) {
            flag(key, now);
            hide(event);
        }
    }

    // Detectors

    private boolean checkFlood(String key, long now) {
        long window = (long) (floodWindow.get() * 1000);
        Deque<Long> times = floodTimes.computeIfAbsent(key, _ -> new ArrayDeque<>());

        times.addLast(now);
        while (!times.isEmpty() && now - times.peekFirst() > window) times.pollFirst();

        return times.size() > floodMaxMessages.get();
    }

    private boolean checkSwarm(String key, String msg, long now) {
        String norm = NON_ALNUM.matcher(msg.toLowerCase(Locale.ROOT)).replaceAll("");
        if (norm.isEmpty()) return false;

        long window = (long) (swarmWindow.get() * 1000);
        swarmEntries.entrySet().removeIf(e -> now - e.getValue().firstSeen > window);

        SwarmEntry entry = swarmEntries.computeIfAbsent(norm, _ -> new SwarmEntry(now));
        entry.senders.add(key);

        if (entry.senders.size() >= swarmMinAccounts.get()) {
            for (String sender : entry.senders) flag(sender, now); // whole swarm is bots
            return true;
        }

        return false;
    }

    private boolean checkCadence(String key, String msg, long now) {
        boolean fast = false;

        if (lastTime.containsKey(key) && msg.length() >= minLength.get()) {
            long dt = now - lastTime.getLong(key);
            double seconds = dt / 1000.0;
            double cps = seconds > 0 ? msg.length() / seconds : Double.POSITIVE_INFINITY;

            if (dt <= (long) (minInterval.get() * 1000) || cps > maxCharsPerSecond.get()) fast = true;
        }

        lastTime.put(key, now);
        return fast;
    }

    private boolean checkContent(String msg) {
        if (blockLinks.get() && LINK.matcher(msg).find()) return true;
        if (blockIps.get() && IP.matcher(msg).find()) return true;
        if (blockZalgo.get() && isZalgo(msg)) return true;
        if (maxCharRepeat.get() > 0 && hasCharRun(msg, maxCharRepeat.get())) return true;

        if (!keywords.get().isEmpty()) {
            String lower = msg.toLowerCase(Locale.ROOT);
            for (String keyword : keywords.get()) {
                if (!keyword.isEmpty() && lower.contains(keyword.toLowerCase(Locale.ROOT))) return true;
            }
        }

        return false;
    }

    // Helpers

    private boolean isZalgo(String s) {
        int combining = 0;
        for (int i = 0; i < s.length(); i++) {
            int type = Character.getType(s.charAt(i));
            if (type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK || type == Character.COMBINING_SPACING_MARK) combining++;
        }
        return combining >= 4 || (!s.isEmpty() && (double) combining / s.length() > 0.15);
    }

    private boolean hasCharRun(String s, int n) {
        int run = 1;
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) == s.charAt(i - 1)) {
                if (++run >= n) return true;
            } else {
                run = 1;
            }
        }
        return false;
    }

    private boolean isWhitelisted(String name) {
        if (mc.getUser() != null && name.equalsIgnoreCase(mc.getUser().getName())) return true;
        if (whitelistFriends.get() && Friends.get().get(name) != null) return true;
        return contains(whitelist.get(), name);
    }

    private boolean contains(List<String> list, String name) {
        for (String s : list) {
            if (s.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private void flag(String key, long now) {
        if (rememberBots.get()) botCache.put(key, now);
    }

    private boolean isCachedBot(String key, long now) {
        if (!botCache.containsKey(key)) return false;

        double ttl = cacheTtl.get();
        if (ttl > 0 && now - botCache.getLong(key) > ttl * 1000) {
            botCache.removeLong(key);
            return false;
        }

        return true;
    }

    private void hide(ReceiveMessageEvent event) {
        switch (action.get()) {
            case Hide -> event.cancel();
            case Tag -> event.setMessage(Component.empty()
                .append(Component.literal("[BOT?] ").withStyle(ChatFormatting.DARK_GRAY))
                .append(event.getMessage()));
            case Dim -> event.setMessage(event.getMessage().copy().withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private void clearState() {
        floodTimes.clear();
        swarmEntries.clear();
        lastTime.clear();
        botCache.clear();
    }

    private void compilePattern() {
        try {
            chatPattern = Pattern.compile(chatFormat.get(), Pattern.DOTALL);
        } catch (PatternSyntaxException e) {
            chatPattern = null;
            error("Invalid chat-format regex: %s", e.getMessage());
        }
    }

    private static final class SwarmEntry {
        final Set<String> senders = new HashSet<>();
        final long firstSeen;

        SwarmEntry(long firstSeen) {
            this.firstSeen = firstSeen;
        }
    }

    public enum Action {
        Hide,
        Tag,
        Dim
    }
}
