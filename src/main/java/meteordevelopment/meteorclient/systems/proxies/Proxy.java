/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.proxies;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.net.InetSocketAddress;
import java.util.Objects;

public class Proxy implements ISerializable<Proxy> {
    public final Settings settings = new Settings();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgOptional = settings.createGroup("Optional");

    public Setting<String> name = sgGeneral.add(new StringSetting.Builder()
        .name("name")
        .description("The name of the proxy.")
        .defaultValue("")
        .build()
    );

    public Setting<ProxyType> type = sgGeneral.add(new EnumSetting.Builder<ProxyType>()
        .name("type")
        .description("The type of proxy.")
        .defaultValue(ProxyType.Socks5)
        .build()
    );

    public Setting<String> address = sgGeneral.add(new StringSetting.Builder()
        .name("address")
        .description("The ip address of the proxy.")
        .defaultValue("")
        .filter(Utils::ipFilter)
        .build()
    );

    public Setting<Integer> port = sgGeneral.add(new IntSetting.Builder()
        .name("port")
        .description("The port of the proxy.")
        .defaultValue(0)
        .range(0, 65535)
        .sliderMax(65535)
        .build()
    );

    public Setting<Boolean> enabled = sgGeneral.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Whether the proxy is enabled.")
        .defaultValue(true)
        .build()
    );

    // Optional

    public Setting<String> username = sgOptional.add(new StringSetting.Builder()
        .name("username")
        .description("The username of the proxy.")
        .defaultValue("")
        .build()
    );

    public Setting<String> password = sgOptional.add(new StringSetting.Builder()
        .name("password")
        .description("The password of the proxy.")
        .defaultValue("")
        .visible(() -> type.get().equals(ProxyType.Socks5))
        .build()
    );

    private Proxy() {}
    public Proxy(NbtElement tag) {
        fromTag((NbtCompound) tag);
    }

    public boolean resolveAddress() {
        int port = this.port.get();
        String address = this.address.get();

        if (port <= 0 || port > 65535 || address == null || address.isBlank()) return false;
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        return !socketAddress.isUnresolved();
    }

    public static class Builder {
        protected ProxyType type = ProxyType.Socks5;
        protected String address = "";
        protected int port = 0;
        protected String name = "";
        protected String username = "";
        protected boolean enabled = false;

        public Builder type(ProxyType type) {
            this.type = type;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Proxy build() {
            Proxy proxy = new Proxy();

            if (!type.equals(proxy.type.getDefaultValue())) proxy.type.set(type);
            if (!address.equals(proxy.address.getDefaultValue())) proxy.address.set(address);
            if (port != proxy.port.getDefaultValue()) proxy.port.set(port);
            if (!name.equals(proxy.name.getDefaultValue())) proxy.name.set(name);
            if (!username.equals(proxy.username.getDefaultValue())) proxy.username.set(username);
            if (enabled != proxy.enabled.getDefaultValue()) proxy.enabled.set(enabled);

            return proxy;
        }
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.put("settings", settings.toTag());

        return tag;
    }

    @Override
    public Proxy fromTag(NbtCompound tag) {
        if (tag.contains("settings")) {
            settings.fromTag(tag.getCompound("settings"));
        }

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Proxy proxy = (Proxy) o;
        return Objects.equals(proxy.address.get(), this.address.get()) && Objects.equals(proxy.port.get(), this.port.get());
    }
}
