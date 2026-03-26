/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerboundMovePlayerPacket.class)
public abstract class PlayerMoveC2SPacketMixin implements IPlayerMoveC2SPacket {
    @Unique private int tag;

    @Override
    public void meteor$setTag(int tag) { this.tag = tag; }

    @Override
    public int meteor$getTag() { return this.tag; }
}
