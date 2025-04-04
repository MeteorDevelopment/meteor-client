/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import motordevelopment.motorclient.mixininterface.IPlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerMoveC2SPacket.class)
public abstract class PlayerMoveC2SPacketMixin implements IPlayerMoveC2SPacket {
    @Unique private int tag;

    @Override
    public void motor$setTag(int tag) { this.tag = tag; }

    @Override
    public int motor$getTag() { return this.tag; }
}
