/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.resource.ResourceReloadLogger;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.util.ApiServices;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("currentFps")
    static int meteor$getFps() {
        return 0;
    }

    @Mutable
    @Accessor("session")
    void meteor$setSession(Session session);

    @Accessor("resourceReloadLogger")
    ResourceReloadLogger meteor$getResourceReloadLogger();

    @Accessor("attackCooldown")
    int meteor$getAttackCooldown();

    @Accessor("attackCooldown")
    void meteor$setAttackCooldown(int attackCooldown);

    @Invoker("doAttack")
    boolean meteor$leftClick();

    @Mutable
    @Accessor("profileKeys")
    void meteor$setProfileKeys(ProfileKeys keys);

    @Mutable
    @Accessor("userApiService")
    void meteor$setUserApiService(UserApiService apiService);

    @Mutable
    @Accessor("skinProvider")
    void meteor$setSkinProvider(PlayerSkinProvider skinProvider);

    @Mutable
    @Accessor("socialInteractionsManager")
    void meteor$setSocialInteractionsManager(SocialInteractionsManager socialInteractionsManager);

    @Mutable
    @Accessor("abuseReportContext")
    void meteor$setAbuseReportContext(AbuseReportContext abuseReportContext);

    @Mutable
    @Accessor("gameProfileFuture")
    void meteor$setGameProfileFuture(CompletableFuture<ProfileResult> future);

    @Mutable
    @Accessor("apiServices")
    void meteor$setApiServices(ApiServices apiServices);
}
