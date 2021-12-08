/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts;

import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.NbtException;
import net.minecraft.client.util.Session;
import net.minecraft.nbt.NbtCompound;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public abstract class Account<T extends Account<?>> implements ISerializable<T> {
    protected AccountType type;
    protected String name;

    protected final AccountCache cache;

    public Account(AccountType type, String name) {
        this.type = type;
        this.name = name;
        this.cache = new AccountCache();
    }

    public abstract boolean fetchInfo();

    public boolean fetchHead() {
        String url = AccountUtils.getSkinUrl(cache.username);
        if (url == null) return true;

        return cache.loadHead(url);
    }

    public boolean login() {
        YggdrasilMinecraftSessionService service = (YggdrasilMinecraftSessionService) mc.getSessionService();
        AccountUtils.setBaseUrl(service, YggdrasilEnvironment.PROD.getEnvironment().getSessionHost() + "/session/minecraft/");
        AccountUtils.setJoinUrl(service, YggdrasilEnvironment.PROD.getEnvironment().getSessionHost() + "/session/minecraft/join");
        AccountUtils.setCheckUrl(service, YggdrasilEnvironment.PROD.getEnvironment().getSessionHost() + "/session/minecraft/hasJoined");

        return true;
    }

    public String getUsername() {
        if (cache.username.isEmpty()) return name;
        return cache.username;
    }

    public AccountType getType() {
        return type;
    }

    public AccountCache getCache() {
        return cache;
    }

    protected void setSession(Session session) {
        ((MinecraftClientAccessor) mc).setSession(session);
        mc.getSessionProperties().clear();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("type", type.name());
        tag.putString("name", name);
        tag.put("cache", cache.toTag());

        return tag;
    }

    @Override
    public T fromTag(NbtCompound tag) {
        if (!tag.contains("name") || !tag.contains("cache")) throw new NbtException();

        name = tag.getString("name");
        cache.fromTag(tag.getCompound("cache"));

        return (T) this;
    }
}
