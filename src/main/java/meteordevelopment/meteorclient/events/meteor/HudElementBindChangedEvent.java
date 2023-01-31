/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.meteor;

import meteordevelopment.meteorclient.systems.hud.HudElement;

public class HudElementBindChangedEvent {
    private static final HudElementBindChangedEvent INSTANCE = new HudElementBindChangedEvent();

    public HudElement element;

    public static HudElementBindChangedEvent get(HudElement element) {
        INSTANCE.element = element;
        return INSTANCE;
    }
}
