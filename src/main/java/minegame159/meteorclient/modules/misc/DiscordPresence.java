/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

//Hand crafted by seesnale ez

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.Config;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.settings.StringSetting;
import minegame159.meteorclient.utils.Utils;

public class DiscordPresence extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> line1 = sgGeneral.add(new StringSetting.Builder()
            .name("line-1")
            .description("The text it displays on line 1 of the RPC.")
            .defaultValue("{player} || {server}")
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

    public DiscordPresence() {
        super(Category.Misc, "discord-presence", "Displays a RPC for you on Discord to show that you're playing Meteor Client!");
    }

    private static final DiscordRichPresence rpc = new DiscordRichPresence();
    private static final DiscordRPC instance = DiscordRPC.INSTANCE;
    private SmallImage currentSmallImage;
    private int ticks;

    @Override
    public void onActivate() {
        DiscordEventHandlers eventHandlers = new DiscordEventHandlers();
        eventHandlers.disconnected = ((var1, var2) -> System.out.println("Discord RPC disconnected, var1: " + var1 + ", var2: " + var2));

        instance.Discord_Initialize("709793491911180378", eventHandlers, true, null);

        rpc.startTimestamp = System.currentTimeMillis() / 1000L;
        rpc.largeImageKey = "meteor_client";
        rpc.largeImageText = "Meteor Client v" + Config.INSTANCE.version;
        rpc.details = getLine(line1);
        rpc.state = getLine(line2);
        currentSmallImage = SmallImage.MineGame;

        instance.Discord_UpdatePresence(rpc);
    }

    @Override
    public void onDeactivate() {
        instance.Discord_Shutdown();
        instance.Discord_ClearPresence();
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (!Utils.canUpdate()) return;
        ticks++;

        if (ticks >= 200) {
            currentSmallImage = currentSmallImage.next();
            currentSmallImage.apply();
            instance.Discord_UpdatePresence(rpc);

            ticks = 0;
        }

        instance.Discord_RunCallbacks();

        updateDetails();
    });

    private String getLine(Setting<String> line) {
        if (line.get().length() > 0) return line.get().replace("{player}", getName()).replace("{server}", getServer());
        else return null;
    }

    private String getServer(){
        if (mc.isInSingleplayer()) return "SinglePlayer";
        else return Utils.getWorldName();
    }

    private String getName(){
        return mc.player.getGameProfile().getName();
    }

    private void updateDetails() {
        if (isActive()) {
            rpc.details = getLine(line1);
            rpc.state = getLine(line2);
            instance.Discord_UpdatePresence(rpc);
        }
    }

    private enum SmallImage {
        MineGame("minegame", "MineGame159"),
        Squid("squidoodly", "squidoodly");

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
            if (this == MineGame) return Squid;
            return MineGame;
        }
    }
}
