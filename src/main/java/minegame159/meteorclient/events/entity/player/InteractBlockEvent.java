/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.events.entity.player;

import minegame159.meteorclient.events.Cancellable;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

public class InteractBlockEvent extends Cancellable {
    private static final InteractBlockEvent INSTANCE = new InteractBlockEvent();
    
    public ClientPlayerEntity clientPlayerEntity;
    public ClientWorld clientWorld;
    public Hand hand;
    public BlockHitResult blockHitResult;
    public ActionResult toReturn;
    
    public static InteractBlockEvent get(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld, Hand hand, BlockHitResult blockHitResult) {
        INSTANCE.setCancelled(false);
        INSTANCE.clientPlayerEntity = clientPlayerEntity;
        INSTANCE.clientWorld = clientWorld;
        INSTANCE.hand = hand;
        INSTANCE.blockHitResult = blockHitResult;
        INSTANCE.toReturn = null;
        
        return INSTANCE;
    }
}
