/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.ResourceLoadStateTracker;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.server.Services;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("fps")
    static int meteor$getFps() {
        return 0;
    }

    @Mutable
    @Accessor("user")
    void meteor$setUser(User session);

    @Accessor("reloadStateTracker")
    ResourceLoadStateTracker meteor$getReloadStateTracker();

    @Accessor("missTime")
    int meteor$getMissTime();

    @Accessor("missTime")
    void meteor$setMissTime(int attackCooldown);

    @Invoker("startAttack")
    boolean meteor$leftClick();

    @Mutable
    @Accessor("profileKeyPairManager")
    void meteor$setProfileKeyPairManager(ProfileKeyPairManager keys);

    @Mutable
    @Accessor("userApiService")
    void meteor$setUserApiService(UserApiService apiService);

    @Mutable
    @Accessor("skinManager")
    void meteor$setSkinManager(SkinManager skinProvider);

    @Mutable
    @Accessor("playerSocialManager")
    void meteor$setPlayerSocialManager(PlayerSocialManager socialInteractionsManager);

    @Mutable
    @Accessor("reportingContext")
    void meteor$setReportingContext(ReportingContext abuseReportContext);

    @Mutable
    @Accessor("profileFuture")
    void meteor$setProfileFuture(CompletableFuture<ProfileResult> future);

    @Mutable
    @Accessor("services")
    void meteor$setServices(Services apiServices);

    @Invoker("handleKeybinds")
    void meteor$handleInputEvents();
}
