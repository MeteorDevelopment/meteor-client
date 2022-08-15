/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.proxies;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.net.InetSocketAddress;

public class Proxy implements ISerializable<Proxy> {
    public final Settings settings = new Settings();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgOptional = settings.createGroup("Optional");

    public Setting<String> nameSetting = sgGeneral.add(new StringSetting.Builder()
        .name("name")
        .description("The name of the proxy.")
        .defaultValue("")
        .build()
    );

    public Setting<ProxyType> typeSetting = sgGeneral.add(new EnumSetting.Builder<ProxyType>()
        .name("type")
        .description("The type of proxy.")
        .defaultValue(ProxyType.Socks5)
        .build()
    );

    public Setting<String> addressSetting = sgGeneral.add(new StringSetting.Builder()
        .name("address")
        .description("The ip address of the proxy.")
        .defaultValue("")
        .build()
    );

    public Setting<Integer> portSetting = sgGeneral.add(new IntSetting.Builder()
        .name("port")
        .description("The port of the proxy.")
        .defaultValue(0)
        .range(0, 65535)
        .sliderMax(65535)
        .build()
    );

    public Setting<Boolean> enabledSetting = sgGeneral.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Whether the proxy is enabled.")
        .defaultValue(true)
        .build()
    );

    // Optional

    public Setting<String> usernameSetting = sgOptional.add(new StringSetting.Builder()
        .name("username")
        .description("The username of the proxy.")
        .defaultValue("")
        .build()
    );

    public Setting<String> passwordSetting = sgOptional.add(new StringSetting.Builder()
        .name("password")
        .description("The password of the proxy.")
        .defaultValue("")
        .visible(() -> typeSetting.get().equals(ProxyType.Socks5))
        .build()
    );

    private Proxy() {}
    public Proxy(NbtElement tag) {
        fromTag((NbtCompound) tag);
    }

    public boolean resolveAddress() {
        int port = portSetting.get();
        String address = addressSetting.get();

        if (port <= 0 || port > 65535 || address == null || address.isBlank()) return false;
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        return !socketAddress.isUnresolved();
    }

    public static class Builder {
        protected ProxyType typeB = ProxyType.Socks5;
        protected String addressB = "";
        protected int portB = 0;
        protected String nameB = "";
        protected String usernameB = "";
        protected boolean enabledB = false;

        public Builder type(ProxyType type) {
            this.typeB = type;
            return this;
        }

        public Builder address(String address) {
            this.addressB = address;
            return this;
        }

        public Builder port(int port) {
            this.portB = port;
            return this;
        }

        public Builder name(String name) {
            this.nameB = name;
            return this;
        }

        public Builder username(String username) {
            this.usernameB = username;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabledB = enabled;
            return this;
        }

        public Proxy build() {
            Proxy proxy = new Proxy();

            if (!typeB.equals(proxy.typeSetting.getDefaultValue())) proxy.typeSetting.set(typeB);
            if (!addressB.equals(proxy.addressSetting.getDefaultValue())) proxy.addressSetting.set(addressB);
            if (portB != proxy.portSetting.getDefaultValue()) proxy.portSetting.set(portB);
            if (!nameB.equals(proxy.nameSetting.getDefaultValue())) proxy.nameSetting.set(nameB);
            if (!usernameB.equals(proxy.usernameSetting.getDefaultValue())) proxy.usernameSetting.set(usernameB);
            if (enabledB != proxy.enabledSetting.getDefaultValue()) proxy.enabledSetting.set(enabledB);

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
}
