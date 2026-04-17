/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AntiPacketKick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @see net.minecraft.network.handler.PacketBundler#decode(io.netty.channel.ChannelHandlerContext, net.minecraft.network.packet.Packet, java.util.List)
 */
@Mixin(targets = "net/minecraft/network/handler/PacketBundleHandler$1$1")
public class PacketBundlingMixin {
    @ModifyExpressionValue(method = "add", at = @At(value = "CONSTANT", args = "intValue=4096"))
    private int add(int value) {
        if (Modules.get().get(AntiPacketKick.class).isActive()) return Integer.MAX_VALUE;
        return value;
    }
}
