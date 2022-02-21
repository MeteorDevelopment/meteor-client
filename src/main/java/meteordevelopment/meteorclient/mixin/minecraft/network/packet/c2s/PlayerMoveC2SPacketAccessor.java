/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.minecraft.network.packet.c2s;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerMoveC2SPacket.class)
public interface PlayerMoveC2SPacketAccessor {
    @Mutable
    @Accessor("y")
    void setY(double y);

    @Mutable
    @Accessor("onGround")
    void setOnGround(boolean onGround);
}
