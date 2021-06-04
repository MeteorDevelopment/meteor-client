/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.events.entity.player;

import minegame159.meteorclient.events.Cancellable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

public class InteractEntityEvent extends Cancellable {
    public static final InteractEntityEvent INSTANCE = new InteractEntityEvent();
    
    public PlayerEntity playerEntity;
    public Entity entity;
    public Hand hand;
    
    public static InteractEntityEvent get(PlayerEntity playerEntity, Entity entity, Hand hand) {
        INSTANCE.setCancelled(false);
        INSTANCE.playerEntity = playerEntity;
        INSTANCE.entity = entity;
        INSTANCE.hand = hand;
        
        return INSTANCE;
    }
}
