/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AntiPacketKick;
import net.minecraft.network.PacketInflater;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.zip.DataFormatException;

@Mixin(PacketInflater.class)
public class PacketInflaterMixin {
    @Inject(method = "decode", at = @At("HEAD"), cancellable = true)
    private void onDecode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list, CallbackInfo info) throws DataFormatException {
        if (Modules.get().isActive(AntiPacketKick.class)) info.cancel();
    }
}
