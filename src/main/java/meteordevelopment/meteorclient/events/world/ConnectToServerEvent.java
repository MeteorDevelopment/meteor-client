/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.world;

import net.minecraft.client.network.ServerInfo;

public class ConnectToServerEvent {
    private static final ConnectToServerEvent INSTANCE = new ConnectToServerEvent();
    public ServerInfo info;

    public static ConnectToServerEvent get(ServerInfo info) {
        INSTANCE.info = info;
        return INSTANCE;
    }
}
