/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity.player;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

public class InteractEntityEvent extends Cancellable {
    private static final InteractEntityEvent INSTANCE = new InteractEntityEvent();

    public Entity entity;
    public InteractionHand hand;

    public static InteractEntityEvent get(Entity entity, InteractionHand hand) {
        INSTANCE.setCancelled(false);
        INSTANCE.entity = entity;
        INSTANCE.hand = hand;
        return INSTANCE;
    }
}
