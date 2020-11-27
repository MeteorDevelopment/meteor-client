/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

//Created by squidoodly 12/05/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Utils;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;

public class DiscordPresence extends ToggleModule {
    private enum SmallImage {
        MineGame("minegame", "MineGame159"),
        Squid("squidoodly", "squidoodly");

        private final String key, text;

        SmallImage(String key, String text) {
            this.key = key;
            this.text = text;
        }

        void apply(DiscordRichPresence presence) {
            presence.smallImageKey = key;
            presence.smallImageText = text;
        }

        SmallImage next() {
            if (this == MineGame) return Squid;
            return MineGame;
        }
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> displayName = sgGeneral.add(new BoolSetting.Builder()
            .name("display-name")
            .description("Displays your name in discord rpc.")
            .defaultValue(true)
            .onChanged(booleanSetting -> updateDetails())
            .build()
    );

    private final Setting<Boolean> displayServer = sgGeneral.add(new BoolSetting.Builder()
            .name("display-server")
            .description("Displays the server you are in.")
            .defaultValue(true)
            .onChanged(booleanSetting -> updateDetails())
            .build()
    );

    private final DiscordRichPresence presence = new DiscordRichPresence();
    private boolean ready;

    private int ticks;
    private SmallImage currentSmallImage;

    public DiscordPresence() {
        super(Category.Misc, "discord-presence", "That stuff you see in discord");
    }

    @Override
    public void onActivate(){
        ticks = 0;
        currentSmallImage = SmallImage.MineGame;

        DiscordRPC.discordInitialize("709793491911180378", new DiscordEventHandlers.Builder()
                .setReadyEventHandler(user -> {
                    ready = true;

                    presence.startTimestamp = System.currentTimeMillis();
                    presence.details = getText();
                    presence.largeImageKey = "meteor_client";
                    presence.largeImageText = "https://meteorclient.com/";
                    currentSmallImage.apply(presence);

                    DiscordRPC.discordUpdatePresence(presence);
                })
                .setDisconnectedEventHandler((errorCode, message) -> ready = false)
                .setErroredEventHandler((errorCode, message) -> ready = false)
                .build(), false);
    }

    @Override
    public void onDeactivate(){
        DiscordRPC.discordShutdown();
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (!Utils.canUpdate()) return;

        if (ready) {
            ticks++;

            if (ticks >= 200) {
                currentSmallImage = currentSmallImage.next();
                currentSmallImage.apply(presence);
                DiscordRPC.discordUpdatePresence(presence);

                ticks = 0;
            }
        }

        DiscordRPC.discordRunCallbacks();
    });

    private void updateDetails() {
        if (isActive()) {
            presence.details = getText();
            if (ready) DiscordRPC.discordUpdatePresence(presence);
        }
    }

    private String getText() {
        if (mc.isInSingleplayer()) {
            if (displayName.get()) return getName() + " || SinglePlayer";
            else return "SinglePlayer";
        }

        if (displayName.get() && displayServer.get()) return getName() + " || " + getServer();
        else if (!displayName.get() && displayServer.get()) return getServer();
        else if (displayName.get() && !displayServer.get()) return getName();

        return "";
    }

    private String getServer(){
        return Utils.getWorldName();
    }

    private String getName(){
        return mc.player.getEntityName();
    }
}
