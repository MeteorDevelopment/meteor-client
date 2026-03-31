/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;

public interface IPlayerInteractEntityC2SPacket {
    ServerboundInteractPacket.ActionType meteor$getType();

    Entity meteor$getEntity();
}
