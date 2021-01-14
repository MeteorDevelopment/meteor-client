/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.events.meteor;

public class MacroListChangedEvent {
    private static final MacroListChangedEvent INSTANCE = new MacroListChangedEvent();

    public static MacroListChangedEvent get() {
        return INSTANCE;
    }
}
