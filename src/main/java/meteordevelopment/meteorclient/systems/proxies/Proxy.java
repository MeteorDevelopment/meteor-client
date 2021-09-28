/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.proxies;

import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.NbtCompound;

import java.net.InetSocketAddress;

public class Proxy implements ISerializable<Proxy> {

    public ProxyType type = ProxyType.Socks5;
    public String address = "";
    public int port = 0;

    public String name = "";
    public String username = "";
    public String password = "";

    public boolean enabled = false;

    public boolean resolveAddress() {
        if(port < 0 || port > 65535) return false;
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        return !socketAddress.isUnresolved();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("type", type.name());
        tag.putString("ip", address);
        tag.putInt("port", port);

        tag.putString("name", name);
        tag.putString("username", username);
        tag.putString("password", password);

        tag.putBoolean("enabled", enabled);

        return tag;
    }

    @Override
    public Proxy fromTag(NbtCompound tag) {
        type = ProxyType.valueOf(tag.getString("type"));
        address = tag.getString("ip");
        port = tag.getInt("port");

        name = tag.getString("name");
        username = tag.getString("username");
        password = tag.getString("password");

        enabled = tag.getBoolean("enabled");

        return this;
    }
}
