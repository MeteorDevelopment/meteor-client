/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import meteordevelopment.meteorclient.mixin.FileCacheAccessor;
import meteordevelopment.meteorclient.mixin.MinecraftAccessor;
import meteordevelopment.meteorclient.mixin.SkinManagerAccessor;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.NbtException;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Services;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public abstract class Account<T extends Account<?>> implements ISerializable<T> {
    protected AccountType type;
    protected String name;

    protected final AccountCache cache;

    protected Account(AccountType type, String name) {
        this.type = type;
        this.name = name;
        this.cache = new AccountCache();
    }

    public abstract boolean fetchInfo();

    public boolean login() {
        YggdrasilAuthenticationService authenticationService = new YggdrasilAuthenticationService(mc.getProxy());
        applyLoginEnvironment(authenticationService);

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

    public static void setSession(User session) {
        MinecraftAccessor mca = (MinecraftAccessor) mc;
        mca.meteor$setUser(session);

        YggdrasilAuthenticationService yggdrasilAuthenticationService = new YggdrasilAuthenticationService(mc.getProxy());

        UserApiService apiService = yggdrasilAuthenticationService.createUserApiService(session.getAccessToken());
        mca.meteor$setUserApiService(apiService);
        mca.meteor$setPlayerSocialManager(new PlayerSocialManager(mc, apiService));
        mca.meteor$setProfileKeyPairManager(ProfileKeyPairManager.create(apiService, session, mc.gameDirectory.toPath()));
        mca.meteor$setReportingContext(ReportingContext.create(ReportEnvironment.local(), apiService));
        mca.meteor$setProfileFuture(CompletableFuture.supplyAsync(() -> mc.services().sessionService().fetchProfile(mc.getUser().getProfileId(), true), Util.ioPool()));
    }

    public static void applyLoginEnvironment(YggdrasilAuthenticationService authService) {
        MinecraftAccessor mca = (MinecraftAccessor) mc;
        SignatureValidator.from(authService.getServicesKeySet(), ServicesKeyType.PROFILE_KEY);
        SkinManager.TextureCache skinCache = ((SkinManagerAccessor) mc.getSkinManager()).meteor$getSkinTextures();
        Path skinCachePath = ((FileCacheAccessor) skinCache).meteor$getRoot();
        mca.meteor$setServices(Services.create(authService, mc.gameDirectory));
        mca.meteor$setSkinManager(new SkinManager(skinCachePath, mc.services(), new SkinTextureDownloader(mc.getProxy(), mc.getTextureManager(), mc), mc));
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("type", type.name());
        tag.putString("name", name);
        tag.put("cache", cache.toTag());

        return tag;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T fromTag(CompoundTag tag) {
        if (tag.getString("name").isEmpty() || tag.getCompound("cache").isEmpty()) throw new NbtException();

        name = tag.getString("name").get();
        cache.fromTag(tag.getCompound("cache").get());

        return (T) this;
    }
}
