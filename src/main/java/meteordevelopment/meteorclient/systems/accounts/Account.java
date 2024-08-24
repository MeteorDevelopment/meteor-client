/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import meteordevelopment.meteorclient.mixin.FileCacheAccessor;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.mixin.PlayerSkinProviderAccessor;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.NbtException;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.encryption.SignatureVerifier;
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
        YggdrasilAuthenticationService authenticationService = new YggdrasilAuthenticationService(((MinecraftClientAccessor) mc).getProxy());
        applyLoginEnvironment(authenticationService, authenticationService.createMinecraftSessionService());

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

    public static void setSession(Session session) {
        MinecraftClientAccessor mca = (MinecraftClientAccessor) mc;
        mca.setSession(session);
        UserApiService apiService;
        apiService = mca.getAuthenticationService().createUserApiService(session.getAccessToken());
        mca.setUserApiService(apiService);
        mca.setSocialInteractionsManager(new SocialInteractionsManager(mc, apiService));
        mca.setProfileKeys(ProfileKeys.create(apiService, session, mc.runDirectory.toPath()));
        mca.setAbuseReportContext(AbuseReportContext.create(ReporterEnvironment.ofIntegratedServer(), apiService));
        mca.setGameProfileFuture(CompletableFuture.supplyAsync(() -> mc.getSessionService().fetchProfile(mc.getSession().getUuidOrNull(), true), Util.getIoWorkerExecutor()));
    }

    public static void applyLoginEnvironment(YggdrasilAuthenticationService authService, MinecraftSessionService sessService) {
        MinecraftClientAccessor mca = (MinecraftClientAccessor) mc;
        mca.setAuthenticationService(authService);
        SignatureVerifier.create(authService.getServicesKeySet(), ServicesKeyType.PROFILE_KEY);
        mca.setSessionService(sessService);
        PlayerSkinProvider.FileCache skinCache = ((PlayerSkinProviderAccessor) mc.getSkinProvider()).getSkinCache();
        Path skinCachePath = ((FileCacheAccessor) skinCache).getDirectory();
        mca.setSkinProvider(new PlayerSkinProvider(mc.getTextureManager(), skinCachePath, sessService, mc));
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
