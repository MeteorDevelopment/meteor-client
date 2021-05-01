/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.misc;

//Created by squidoodly

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.settings.StringSetting;
import minegame159.meteorclient.systems.config.Config;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.Placeholders;

public class DiscordPresence extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> line1 = sgGeneral.add(new StringSetting.Builder()
            .name("line-1")
            .description("The text it displays on line 1 of the RPC.")
            .defaultValue("{username} || {server}")
            .onChanged(booleanSetting -> updateDetails())
            .build()
    );

    private final Setting<String> line2 = sgGeneral.add(new StringSetting.Builder()
            .name("line-2")
            .description("The text it displays on line 2 of the RPC.")
            .defaultValue("Meteor on Crack!")
            .onChanged(booleanSetting -> updateDetails())
            .build()
    );

    private static final DiscordRichPresence rpc = new DiscordRichPresence();
    private static final DiscordRPC instance = DiscordRPC.INSTANCE;
    private SmallImage currentSmallImage;
    private int ticks;

    public DiscordPresence() {
        super(Categories.Misc, "discord-presence", "Displays a RPC for you on Discord to show that you're playing Meteor Client!");
    }

    @Override
    public void onActivate() {
        DiscordEventHandlers handlers = new DiscordEventHandlers();
        instance.Discord_Initialize("835240968533049424", handlers, true, null);

        rpc.startTimestamp = System.currentTimeMillis() / 1000L;
        rpc.largeImageKey = "meteor_client";
        String largeText = "Meteor Client " + Config.get().version.getOriginalString();
        if (!Config.get().devBuild.isEmpty()) largeText += " Dev Build: " + Config.get().devBuild;
        rpc.largeImageText = largeText;
        currentSmallImage = SmallImage.MineGame;
        updateDetails();

        instance.Discord_UpdatePresence(rpc);
        instance.Discord_RunCallbacks();
    }

    @Override
    public void onDeactivate() {
        instance.Discord_ClearPresence();
        instance.Discord_Shutdown();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!Utils.canUpdate()) return;
        ticks++;

        if (ticks >= 200) {
            currentSmallImage = currentSmallImage.next();
            currentSmallImage.apply();
            instance.Discord_UpdatePresence(rpc);

            ticks = 0;
        }

        updateDetails();
        instance.Discord_RunCallbacks();
    }

    private void updateDetails() {
        if (isActive() && Utils.canUpdate()) {
            rpc.details = Placeholders.apply(line1.get());
            rpc.state = Placeholders.apply(line2.get());

            instance.Discord_UpdatePresence(rpc);
        }
    }

    private enum SmallImage {
        MineGame("minegame", "MineGame159"),
        Snail("seasnail", "seasnail8169");

        private final String key, text;

        SmallImage(String key, String text) {
            this.key = key;
            this.text = text;
        }

        void apply() {
            rpc.smallImageKey = key;
            rpc.smallImageText = text;
        }

        SmallImage next() {
            if (this == MineGame) return Snail;
            return MineGame;
        }
    }
}
