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

    public final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
        .name("name")
        .description("The name of the proxy.")
        .defaultValue("")
        .build()
    );

    public final Setting<ProxyType> type = sgGeneral.add(new EnumSetting.Builder<ProxyType>()
        .name("type")
        .description("The type of proxy.")
        .defaultValue(ProxyType.SOCKS_5)
        .build()
    );

    public final Setting<String> address = sgGeneral.add(new StringSetting.Builder()
        .name("address")
        .description("The ip address of the proxy.")
        .defaultValue("")
        .filter(Utils::ipFilter)
        .build()
    );

    public final Setting<Integer> port = sgGeneral.add(new IntSetting.Builder()
        .name("port")
        .description("The port of the proxy.")
        .defaultValue(0)
        .range(0, 65535)
        .sliderMax(65535)
        .build()
    );

    public final Setting<Boolean> enabled = sgGeneral.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Whether the proxy is enabled.")
        .defaultValue(true)
        .build()
    );

    // Optional

    public final Setting<String> username = sgOptional.add(new StringSetting.Builder()
        .name("username")
        .description("The username of the proxy.")
        .defaultValue("")
        .build()
    );

    public final Setting<String> password = sgOptional.add(new StringSetting.Builder()
        .name("password")
        .description("The password of the proxy.")
        .defaultValue("")
        .visible(() -> type.get() == ProxyType.SOCKS_5)
        .build()
    );

    private Proxy() {}
    public Proxy(NbtElement tag) {
        fromTag((NbtCompound) tag);
    }

    public boolean resolveAddress() {
        int p = port.get();
        String addr = address.get();

        if (p <= 0 || p > 65535 || addr == null || addr.isBlank()) return false;
        InetSocketAddress socketAddress = new InetSocketAddress(addr, p);
        return !socketAddress.isUnresolved();
    }

    public static class Builder {
        protected ProxyType type = ProxyType.SOCKS_5;
        protected String address = "";
        protected int port;
        protected String name = "";
        protected String username = "";
        protected boolean enabled;

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

            if (type != proxy.type.getDefaultValue()) proxy.type.set(type);
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
        if (tag.contains("settings")) settings.fromTag(tag.getCompound("settings"));
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Proxy proxy = (Proxy) o;
        return Objects.equals(proxy.address.get(), address.get())
            && Objects.equals(proxy.port.get(), port.get())
            && proxy.type.get() == type.get();
    }
}
