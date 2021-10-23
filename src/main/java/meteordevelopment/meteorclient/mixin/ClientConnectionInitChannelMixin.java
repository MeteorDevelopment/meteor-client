/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import io.netty.channel.Channel;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import meteordevelopment.meteorclient.systems.proxies.Proxy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

@Mixin(targets = "net.minecraft.network.ClientConnection$1")
public class ClientConnectionInitChannelMixin {
    @Inject(method = "initChannel", at = @At("HEAD"))
    private void onInitChannel(Channel channel, CallbackInfo info) {
        Proxy proxy = Proxies.get().getEnabled();
        if (proxy == null) return;

        switch (proxy.type) {
            case Socks4 -> channel.pipeline().addFirst(new Socks4ProxyHandler(new InetSocketAddress(proxy.address, proxy.port), proxy.username));
            case Socks5 -> channel.pipeline().addFirst(new Socks5ProxyHandler(new InetSocketAddress(proxy.address, proxy.port), proxy.username, proxy.password));
        }
    }
}
