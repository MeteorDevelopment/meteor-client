/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.server.Services;

@Mixin(Minecraft.class)
public interface MinecraftClientAccessor {
    @Accessor("fps")
    static int meteor$getFps() {
        return 0;
    }

    @Mutable
    @Accessor("user")
    void meteor$setSession(User session);

    @Accessor("reloadStateTracker")
    ResourceLoadStateTracker meteor$getResourceReloadLogger();

    @Accessor("missTime")
    int meteor$getAttackCooldown();

    @Accessor("missTime")
    void meteor$setAttackCooldown(int attackCooldown);

    @Invoker("startAttack")
    boolean meteor$leftClick();

    @Mutable
    @Accessor("profileKeyPairManager")
    void meteor$setProfileKeys(ProfileKeyPairManager keys);

    @Mutable
    @Accessor("userApiService")
    void meteor$setUserApiService(UserApiService apiService);

    @Mutable
    @Accessor("skinManager")
    void meteor$setSkinProvider(SkinManager skinProvider);

    @Mutable
    @Accessor("playerSocialManager")
    void meteor$setSocialInteractionsManager(PlayerSocialManager socialInteractionsManager);

    @Mutable
    @Accessor("reportingContext")
    void meteor$setAbuseReportContext(ReportingContext abuseReportContext);

    @Mutable
    @Accessor("profileFuture")
    void meteor$setGameProfileFuture(CompletableFuture<ProfileResult> future);

    @Mutable
    @Accessor("services")
    void meteor$setApiServices(Services apiServices);

    @Invoker("handleKeybinds")
    void meteor$handleInputEvents();
}
