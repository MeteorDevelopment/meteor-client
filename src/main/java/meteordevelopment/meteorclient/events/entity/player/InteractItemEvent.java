/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity.player;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

public class InteractItemEvent {
    private static final InteractItemEvent INSTANCE = new InteractItemEvent();

    public InteractionHand hand;
    public InteractionResult toReturn;

    public static InteractItemEvent get(InteractionHand hand) {
        INSTANCE.hand = hand;
        INSTANCE.toReturn = null;

        return INSTANCE;
    }
}
