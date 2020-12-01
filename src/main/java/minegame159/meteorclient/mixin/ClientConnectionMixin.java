/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import io.netty.channel.ChannelHandlerContext;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.packets.ReceivePacketEvent;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.misc.AntiPacketKick;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.net.InetAddress;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(method = "disconnect", at = @At("HEAD"))
    private void onDisconnect(Text disconnectReason, CallbackInfo info) {
        if (!MeteorClient.IS_DISCONNECTING) {
            MeteorClient.IS_DISCONNECTING = true;
            //MeteorClient.EVENT_BUS.post(EventStore.gameDisconnectedEvent(disconnectReason));
        }
    }

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo info) {
        ReceivePacketEvent event = EventStore.receivePacketEvent(packet);
        MeteorClient.EVENT_BUS.post(event);

        if (event.isCancelled()) info.cancel();
    }

    @Inject(method = "connect", at = @At("HEAD"))
    private static void onConnect(InetAddress address, int port, boolean shouldUseNativeTransport, CallbackInfoReturnable<ClientConnection> info) {
        MeteorClient.EVENT_BUS.post(EventStore.connectToServerEvent());
    }

    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
    private void exceptionCaught(ChannelHandlerContext context, Throwable throwable, CallbackInfo ci) {
        if (throwable instanceof IOException && ModuleManager.INSTANCE.isActive(AntiPacketKick.class)) ci.cancel();
        return;
    }
    //Thanks to 086 for this beauty <3
}