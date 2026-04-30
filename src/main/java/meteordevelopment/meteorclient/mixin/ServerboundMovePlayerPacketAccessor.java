/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface ServerboundMovePlayerPacketAccessor {
    @Mutable
    @Accessor("y")
    void meteor$setY(double y);

    @Mutable
    @Accessor("onGround")
    void meteor$setOnGround(boolean onGround);
}
