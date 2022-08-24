/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.game;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.client.option.Perspective;

public class ChangePerspectiveEvent extends Cancellable {
    private static final ChangePerspectiveEvent INSTANCE = new ChangePerspectiveEvent();

    public Perspective perspective;

    public static ChangePerspectiveEvent get(Perspective perspective) {
        INSTANCE.setCancelled(false);
        INSTANCE.perspective = perspective;
        return INSTANCE;
    }
}
