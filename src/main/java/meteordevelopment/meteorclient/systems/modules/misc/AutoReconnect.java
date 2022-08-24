/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.ConnectToServerEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ServerInfo;

public class AutoReconnect extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Double> time = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("The amount of seconds to wait before reconnecting to the server.")
            .defaultValue(3.5)
            .min(0)
            .decimalPlaces(1)
            .build()
    );

    public ServerInfo lastServerInfo;

    public AutoReconnect() {
        super(Categories.Misc, "auto-reconnect", "Automatically reconnects when disconnected from a server.");
        MeteorClient.EVENT_BUS.subscribe(new StaticListener());
    }

    private class StaticListener {
        @EventHandler
        private void onConnectToServer(ConnectToServerEvent event) {
            lastServerInfo = mc.isInSingleplayer() ? null : mc.getCurrentServerEntry();
        }
    }
}
