/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.proxies;

import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class Proxies extends System<Proxies> implements Iterable<Proxy> {
    // https://regex101.com/r/gRHjnd/latest
    public static final Pattern PROXY_PATTERN = Pattern.compile("^(?:([\\w\\s]+)=)?((?:0*(?:\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5])(?:\\.(?!:)|)){4}):(?!0)(\\d{1,4}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])(?i:@(socks[45]))?$", Pattern.MULTILINE);

    private List<Proxy> proxies = new ArrayList<>();

    public Proxies() {
        super("proxies");
    }

    public static Proxies get() {
        return Systems.get(Proxies.class);
    }

    public boolean add(Proxy proxy) {
        for (Proxy p : proxies) {
            if (p.type.get().equals(proxy.type.get()) && p.address.get().equals(proxy.address.get()) && p.port.get() == proxy.port.get()) return false;
        }

        if (proxies.isEmpty()) proxy.enabled.set(true);

        proxies.add(proxy);
        save();

        return true;
    }

    public void remove(Proxy proxy) {
        if (proxies.remove(proxy)) {
            save();
        }
    }

    public Proxy getEnabled() {
        for (Proxy proxy : proxies) {
            if (proxy.enabled.get()) return proxy;
        }

        return null;
    }

    public void setEnabled(Proxy proxy, boolean enabled) {
        for (Proxy p : proxies) {
            p.enabled.set(false);
        }

        proxy.enabled.set(enabled);
        save();
    }

    public boolean isEmpty() {
        return proxies.isEmpty();
    }

    @NotNull
    @Override
    public Iterator<Proxy> iterator() {
        return proxies.iterator();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.put("proxies", NbtUtils.listToTag(proxies));

        return tag;
    }

    @Override
    public Proxies fromTag(NbtCompound tag) {
        proxies = NbtUtils.listFromTag(tag.getList("proxies", 10), Proxy::new);

        return this;
    }
}
