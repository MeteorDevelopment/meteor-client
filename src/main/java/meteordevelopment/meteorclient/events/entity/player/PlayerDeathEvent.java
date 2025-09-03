/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity.player;

public class PlayerDeathEvent {
    private static final PlayerDeathEvent INSTANCE = new PlayerDeathEvent();

    public static PlayerDeathEvent get() {
        return INSTANCE;
    }
}
