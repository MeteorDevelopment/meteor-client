/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

//Created by squidoodly

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.Config;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.settings.StringSetting;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.discord.RichPresence;
import minegame159.meteorclient.utils.misc.discord.RpcClient;

public class DiscordPresence extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> line1 = sgGeneral.add(new StringSetting.Builder()
            .name("line-1")
            .description("The text it displays on line 1 of the RPC.")
            .defaultValue("{player} || {server}")
            .onChanged(booleanSetting -> onSettingChanged())
            .build()
    );

    private final Setting<String> line2 = sgGeneral.add(new StringSetting.Builder()
            .name("line-2")
            .description("The text it displays on line 2 of the RPC.")
            .defaultValue("Meteor on Crack!")
            .onChanged(booleanSetting -> onSettingChanged())
            .build()
    );

    private final RpcClient client = new RpcClient(this::onReady);
    private final RichPresence presence = new RichPresence();

    private SmallImage currentSmallImage = SmallImage.MineGame;
    private int ticks;

    public DiscordPresence() {
        super(Category.Misc, "discord-presence", "Displays a RPC for you on Discord to show that you're playing Meteor Client!");
    }

    @Override
    public void onActivate() {
        presence.startTimestamp = System.currentTimeMillis();
        presence.largeImage = "meteor_client";

        String largeText = "Meteor Client " + Config.get().version.getOriginalString();
        if (!Config.get().devBuild.isEmpty()) largeText += " Dev Build: " + Config.get().devBuild;
        presence.largeText = largeText;

        currentSmallImage = SmallImage.MineGame;
        updateDetails();

        client.connect(709793491911180378L);
    }

    @Override
    public void onDeactivate() {
        client.close();
    }

    private void onReady() {
        client.send(presence);
    }

    private void onSettingChanged() {
        if (isActive() && Utils.canUpdate()) {
            updateDetails();
            client.send(presence);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        ticks++;

        if (ticks >= 200) {
            currentSmallImage = currentSmallImage.next();

            presence.smallImage = currentSmallImage.key;
            presence.smallText = currentSmallImage.text;

            updateDetails();
            client.send(presence);

            ticks = 0;
        }
    }

    private String getLine(Setting<String> line) {
        if (line.get().length() > 0) return line.get().replace("{player}", getName()).replace("{server}", getServer());
        else return null;
    }

    private String getServer(){
        if (mc.isInSingleplayer()) return "Singleplayer";
        else return Utils.getWorldName();
    }

    private String getName(){
        return mc.player.getGameProfile().getName();
    }

    private void updateDetails() {
        presence.details = getLine(line1);
        presence.state = getLine(line2);
    }

    private enum SmallImage {
        MineGame("minegame", "MineGame159"),
        Squid("squidoodly", "squidoodly");

        public final String key, text;

        SmallImage(String key, String text) {
            this.key = key;
            this.text = text;
        }

        SmallImage next() {
            if (this == MineGame) return Squid;
            return MineGame;
        }
    }
}
