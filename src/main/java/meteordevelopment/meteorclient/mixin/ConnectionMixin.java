/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.timeout.TimeoutException;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ServerConnectEndEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AntiPacketKick;
import meteordevelopment.meteorclient.systems.modules.world.HighwayBuilder;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import meteordevelopment.meteorclient.systems.proxies.Proxy;
import net.minecraft.ChatFormatting;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.Connection;
import net.minecraft.network.SkipPacketEncoderException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.network.EventLoopGroupHolder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetSocketAddress;
import java.util.Iterator;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void onHandlePacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ClientboundBundlePacket bundle) {
            for (Iterator<Packet<? super ClientGamePacketListener>> it = bundle.subPackets().iterator(); it.hasNext(); ) {
                if (MeteorClient.EVENT_BUS.post(new PacketEvent.Receive(it.next(), (Connection) (Object) this)).isCancelled())
                    it.remove();
            }
        } else if (MeteorClient.EVENT_BUS.post(new PacketEvent.Receive(packet, (Connection) (Object) this)).isCancelled())
            ci.cancel();
    }

    @Inject(method = "disconnect(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void disconnect(Component disconnectReason, CallbackInfo ci) {
        if (Modules.get().get(HighwayBuilder.class).isActive()) {
            MutableComponent text = Component.literal("%n%n%s[%sHighway Builder%s] Statistics:%n".formatted(ChatFormatting.GRAY, ChatFormatting.BLUE, ChatFormatting.GRAY));
            text.append(Modules.get().get(HighwayBuilder.class).getStatsText());

            ((MutableComponent) disconnectReason).append(text);
        }
    }

    @Inject(method = "connect(Ljava/net/InetSocketAddress;Lnet/minecraft/server/network/EventLoopGroupHolder;Lnet/minecraft/network/Connection;)Lio/netty/channel/ChannelFuture;", at = @At("HEAD"))
    private static void onConnect(InetSocketAddress address, EventLoopGroupHolder backend, Connection connection, CallbackInfoReturnable<ChannelFuture> cir) {
        MeteorClient.EVENT_BUS.post(ServerConnectEndEvent.get(address));
    }

    @Inject(at = @At("HEAD"), method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", cancellable = true)
    private void onSendPacketHead(Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, CallbackInfo ci) {
        if (MeteorClient.EVENT_BUS.post(new PacketEvent.Send(packet, (Connection) (Object) this)).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("TAIL"))
    private void onSendPacketTail(Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(new PacketEvent.Sent(packet, (Connection) (Object) this));
    }

    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
    private void exceptionCaught(ChannelHandlerContext context, Throwable throwable, CallbackInfo ci) {
        AntiPacketKick apk = Modules.get().get(AntiPacketKick.class);
        if (!(throwable instanceof TimeoutException) && !(throwable instanceof SkipPacketEncoderException) && apk.catchExceptions()) {
            if (apk.logExceptions.get()) apk.warning("Caught exception: %s", throwable);
            ci.cancel();
        }
    }

    @Inject(method = "configureSerialization", at = @At("RETURN"))
    private static void onAddHandlers(ChannelPipeline pipeline, PacketFlow side, boolean local, BandwidthDebugMonitor packetSizeLogger, CallbackInfo ci) {
        if (side != PacketFlow.CLIENTBOUND || local) return;

        Proxy proxy = Proxies.get().getEnabled();
        if (proxy == null) return;

        switch (proxy.type.get()) {
            case Socks4 ->
                pipeline.addFirst(new Socks4ProxyHandler(new InetSocketAddress(proxy.address.get(), proxy.port.get()), proxy.username.get()));
            case Socks5 ->
                pipeline.addFirst(new Socks5ProxyHandler(new InetSocketAddress(proxy.address.get(), proxy.port.get()), proxy.username.get(), proxy.password.get()));
        }
    }
}
