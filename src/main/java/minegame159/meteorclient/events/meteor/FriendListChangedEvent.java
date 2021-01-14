/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.events.meteor;

public class FriendListChangedEvent {
    private static final FriendListChangedEvent INSTANCE = new FriendListChangedEvent();

    public static FriendListChangedEvent get() {
        return INSTANCE;
    }
}
