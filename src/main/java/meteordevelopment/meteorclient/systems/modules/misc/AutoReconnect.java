/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.ServerConnectBeginEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.AccountCache;
import meteordevelopment.meteorclient.systems.accounts.Accounts;
import meteordevelopment.meteorclient.systems.accounts.types.MicrosoftAccount;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoReconnect extends Module {
    private static final long REAUTH_COOLDOWN_MS = 30_000;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Double> time = sgGeneral.add(new DoubleSetting.Builder()
        .name("delay")
        .description("The amount of seconds to wait before reconnecting to the server.")
        .defaultValue(3.5)
        .min(0)
        .decimalPlaces(1)
        .build()
    );

    public final Setting<Boolean> button = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-buttons")
        .description("Will hide the buttons related to Auto Reconnect.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> reAuthenticate = sgGeneral.add(new BoolSetting.Builder()
        .name("re-authenticate")
        .description("Re-authenticates Microsoft accounts before reconnecting if they are saved in Accounts.")
        .defaultValue(false)
        .build()
    );

    private final AtomicBoolean reAuthInFlight = new AtomicBoolean(false);
    private final Map<String, Long> lastReAuthAttempt = new ConcurrentHashMap<>();

    public Pair<ServerAddress, ServerData> lastServerConnection;

    public AutoReconnect() {
        super(Categories.Misc, "auto-reconnect", "Automatically reconnects when disconnected from a server.");
        MeteorClient.EVENT_BUS.subscribe(new StaticListener());
    }

    public boolean isReAuthInFlight() {
        return reAuthenticate.get() && reAuthInFlight.get();
    }

    private void tryReAuthenticate() {
        if (!isActive() || !reAuthenticate.get() || lastServerConnection == null) return;

        User user = mc.getUser();
        if (user == null) {
            MeteorClient.LOG.info("Auto Reconnect re-authentication skipped: no current Minecraft session.");
            return;
        }

        Account<?> account = findCurrentAccount(user);
        if (account == null) {
            MeteorClient.LOG.info("Auto Reconnect re-authentication skipped: no account matches the current session.");
            return;
        }

        String accountKey = getAccountKey(account, user);
        if (isOnCooldown(accountKey)) return;

        if (!(account instanceof MicrosoftAccount microsoftAccount)) {
            lastReAuthAttempt.put(accountKey, System.currentTimeMillis());
            MeteorClient.LOG.info("Auto Reconnect re-authentication skipped: account {} is {}, not Microsoft.", getDisplayName(account), account.getType());
            return;
        }

        if (!reAuthInFlight.compareAndSet(false, true)) return;

        lastReAuthAttempt.put(accountKey, System.currentTimeMillis());
        String username = getDisplayName(account);
        MeteorClient.LOG.info("Auto Reconnect re-authentication started for {}.", username);

        MeteorExecutor.execute(() -> {
            try {
                if (microsoftAccount.fetchInfo() && microsoftAccount.login()) {
                    Accounts.get().save();
                    lastReAuthAttempt.remove(accountKey);
                    MeteorClient.LOG.info("Auto Reconnect re-authentication succeeded for {}.", getDisplayName(microsoftAccount));
                } else {
                    MeteorClient.LOG.warn("Auto Reconnect re-authentication failed for {}.", username);
                }
            } catch (Exception e) {
                MeteorClient.LOG.warn("Auto Reconnect re-authentication failed for {}.", username, e);
            } finally {
                reAuthInFlight.set(false);
            }
        });
    }

    private Account<?> findCurrentAccount(User user) {
        String currentUuid = normalizeUuid(user.getProfileId());
        String currentName = normalizeName(user.getName());
        Account<?> usernameMatch = null;

        for (Account<?> account : Accounts.get()) {
            AccountCache cache = account.getCache();
            String accountUuid = normalizeUuid(cache.uuid);

            if (!currentUuid.isEmpty() && currentUuid.equals(accountUuid)) return account;

            if (usernameMatch == null && !currentName.isEmpty() && currentName.equals(normalizeName(cache.username))) {
                usernameMatch = account;
            }
        }

        return usernameMatch;
    }

    private boolean isOnCooldown(String accountKey) {
        Long lastAttemptAt = lastReAuthAttempt.get(accountKey);
        return lastAttemptAt != null && System.currentTimeMillis() - lastAttemptAt < REAUTH_COOLDOWN_MS;
    }

    private String getAccountKey(Account<?> account, User user) {
        AccountCache cache = account.getCache();
        String uuid = normalizeUuid(cache.uuid);
        if (!uuid.isEmpty()) return "uuid:" + uuid;

        String username = normalizeName(cache.username);
        if (!username.isEmpty()) return "username:" + username;

        String currentUuid = normalizeUuid(user.getProfileId());
        if (!currentUuid.isEmpty()) return "current-uuid:" + currentUuid;

        return "current-username:" + normalizeName(user.getName());
    }

    private String getDisplayName(Account<?> account) {
        String username = account.getCache().username;
        if (username != null && !username.isBlank()) return username;

        return account.getType().name();
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUuid(UUID uuid) {
        return uuid == null ? "" : normalizeUuid(uuid.toString());
    }

    private String normalizeUuid(String uuid) {
        if (uuid == null) return "";
        return uuid.replace("-", "").trim().toLowerCase(Locale.ROOT);
    }

    private class StaticListener {
        @EventHandler
        private void onGameJoined(ServerConnectBeginEvent event) {
            lastServerConnection = new ObjectObjectImmutablePair<>(event.address, event.info);
        }

        @EventHandler
        private void onOpenScreen(OpenScreenEvent event) {
            if (event.screen instanceof DisconnectedScreen) tryReAuthenticate();
        }
    }
}
