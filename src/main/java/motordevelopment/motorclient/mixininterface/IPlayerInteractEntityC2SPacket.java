/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixininterface;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;

public interface IPlayerInteractEntityC2SPacket {
    PlayerInteractEntityC2SPacket.InteractType motor$getType();

    Entity motor$getEntity();
}
