/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.events.world;

public class ConnectToServerEvent {
    private static final ConnectToServerEvent INSTANCE = new ConnectToServerEvent();

    public static ConnectToServerEvent get() {
        return INSTANCE;
    }
}
