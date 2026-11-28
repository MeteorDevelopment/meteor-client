/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Keeps {@link ModuleTier#CHEAT} modules out of multiplayer until the whole server agreed to them.
 *
 * <p>On a server cheat modules are hidden and can not be toggled. When every player that is online types the unlock
 * keyword (default {@code cheat}) in chat, all cheat modules come back for the rest of the session. In singleplayer
 * nothing is hidden.
 */
public class CheatGate extends System<CheatGate> {
    private static final Pattern VANILLA_CHAT = Pattern.compile("^<([A-Za-z0-9_]{1,16})>\\s*(.*)$");
    private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[\\s!.?,;:]+$");
    private static final Pattern FORMATTING = Pattern.compile("(?i)\\u00a7[0-9a-fk-or]");
    private static final String SEPARATORS = " \t:>»|-–—";

    private final Map<String, Long> consent = new HashMap<>();
    private boolean unlocked;

    public CheatGate() {
        // Nothing to save, the unlock only lasts for the current session.
        super(null);
    }

    public static CheatGate get() {
        return Systems.get(CheatGate.class);
    }

    /**
     * Only here so the rest of the code does not have to care about the system not being initialised yet.
     */
    public static boolean isAvailable(Module module) {
        CheatGate gate = get();
        return gate == null || gate.available(module);
    }

    public static boolean inMultiplayer() {
        return mc != null && mc.getNetworkHandler() != null && !mc.isInSingleplayer();
    }

    /**
     * Whether the module may be used right now.
     */
    public boolean available(Module module) {
        if (module.tier != ModuleTier.CHEAT) return true;
        if (!enabled()) return true;
        return !inMultiplayer() || unlocked;
    }

    public boolean isUnlocked() {
        return !enabled() || unlocked;
    }

    /**
     * Tells the player why a module just did nothing.
     */
    public void reportBlocked(Module module) {
        if (mc.world == null || !enabled() || !inMultiplayer() || unlocked) return;

        ChatUtils.warning(
            "%s is a cheat and hidden in multiplayer. All %d players online have to type \"%s\" in chat to unlock cheats.",
            module.title, onlinePlayerNames().size(), keyword()
        );
    }

    // Sessions

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        consent.clear();
        unlocked = false;

        if (!enabled() || !inMultiplayer()) return;

        int disabled = 0;
        for (Module module : Modules.get().getAll()) {
            if (module.tier == ModuleTier.CHEAT && module.isActive()) {
                module.disable();
                disabled++;
            }
        }

        String message = "Cheat modules are hidden on this server, everyone online has to type \"%s\" in chat to unlock them.";
        if (disabled > 0) message += " (%d) active cheats were disabled.";

        ChatUtils.info(message, keyword(), disabled);
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        consent.clear();
        unlocked = false;
    }

    // Chat

    @EventHandler
    private void onSendMessage(SendMessageEvent event) {
        if (!enabled() || unlocked || !inMultiplayer() || mc.player == null) return;

        Config config = Config.get();
        String message = event.message == null ? "" : event.message.trim();

        // Commands are not chat
        if (config != null && message.startsWith(config.prefix.get())) return;
        if (!isKeyword(message)) return;

        addConsent(mc.player.getGameProfile().name());
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (!enabled() || unlocked || !inMultiplayer()) return;

        String text = FORMATTING.matcher(event.getMessage().getString()).replaceAll("").trim();
        if (text.isEmpty()) return;

        String[] parsed = parse(text);
        if (parsed == null) return;

        // Only players that are actually on the server count towards the consensus
        if (!isOnline(parsed[0])) return;
        if (!isKeyword(parsed[1])) return;

        addConsent(parsed[0]);
    }

    private void addConsent(String playerName) {
        if (consent.containsKey(playerName)) return;

        consent.put(playerName, expiry());

        Set<String> online = onlinePlayerNames();
        if (online.isEmpty()) return;

        int agreed = agreedCount(online);

        if (agreed >= online.size()) {
            unlocked = true;
            consent.clear();

            ChatUtils.info("Every player on this server agreed. All cheats are unlocked until you leave the server.");
        } else {
            ChatUtils.info("Cheats unlocked for (%d)/(%d) players.", agreed, online.size());
        }
    }

    private int agreedCount(Set<String> online) {
        long now = java.lang.System.currentTimeMillis();

        int agreed = 0;
        for (String playerName : online) {
            Long until = consent.get(playerName);
            if (until != null && until >= now) agreed++;
        }

        return agreed;
    }

    private long expiry() {
        Config config = Config.get();
        int window = config == null ? 300 : config.cheatConsentWindow.get();
        if (window <= 0) return Long.MAX_VALUE;

        return java.lang.System.currentTimeMillis() + window * 1000L;
    }

    // Parsing

    private String[] parse(String text) {
        Matcher matcher = VANILLA_CHAT.matcher(text);
        if (matcher.matches()) return new String[] {matcher.group(1), matcher.group(2)};

        // Servers that prefix the name, e.g. "[VIP] Name: message" or "Name » message"
        for (String playerName : onlinePlayerNames()) {
            int index = text.indexOf(playerName);
            if (index < 0 || index > 32) continue;
            if (!isNameBoundary(text, index, playerName.length())) continue;

            return new String[] {playerName, stripSeparators(text.substring(index + playerName.length()))};
        }

        return null;
    }

    private boolean isNameBoundary(String text, int index, int length) {
        if (index > 0 && isWordCharacter(text.charAt(index - 1))) return false;

        int end = index + length;
        return end >= text.length() || !isWordCharacter(text.charAt(end));
    }

    private boolean isWordCharacter(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private String stripSeparators(String text) {
        int index = 0;

        while (index < text.length() && SEPARATORS.indexOf(text.charAt(index)) >= 0) index++;

        return text.substring(index).trim();
    }

    private boolean isKeyword(String text) {
        String value = TRAILING_PUNCTUATION.matcher(text.trim()).replaceAll("");
        return value.equalsIgnoreCase(keyword());
    }

    private boolean isOnline(String playerName) {
        for (String online : onlinePlayerNames()) {
            if (online.equalsIgnoreCase(playerName)) return true;
        }

        return false;
    }

    private Set<String> onlinePlayerNames() {
        Set<String> names = new HashSet<>();

        if (mc == null || mc.getNetworkHandler() == null) return names;

        for (PlayerListEntry info : mc.getNetworkHandler().getPlayerList()) {
            names.add(info.getProfile().name());
        }

        return names;
    }

    // Config

    private boolean enabled() {
        Config config = Config.get();
        return config != null && config.cheatGate.get();
    }

    private String keyword() {
        Config config = Config.get();
        if (config == null) return "cheat";

        String keyword = config.cheatKeyword.get();
        return keyword == null || keyword.isEmpty() ? "cheat" : keyword.trim();
    }
}
