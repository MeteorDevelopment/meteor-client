/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.mixin.PlayerSkinProviderAccessor;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.report.AbuseReportContext;
import net.minecraft.client.report.ReporterEnvironment;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.util.ProfileKeys;
import net.minecraft.client.util.Session;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AccountUtils {
    public static void setBaseUrl(YggdrasilMinecraftSessionService service, String url) {
        try {
            Field field = service.getClass().getDeclaredField("baseUrl");
            field.setAccessible(true);
            field.set(service, url);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static void setJoinUrl(YggdrasilMinecraftSessionService service, String url) {
        try {
            Field field = service.getClass().getDeclaredField("joinUrl");
            field.setAccessible(true);
            field.set(service, new URL(url));
        } catch (IllegalAccessException | NoSuchFieldException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void setCheckUrl(YggdrasilMinecraftSessionService service, String url) {
        try {
            Field field = service.getClass().getDeclaredField("checkUrl");
            field.setAccessible(true);
            field.set(service, new URL(url));
        } catch (IllegalAccessException | NoSuchFieldException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static MinecraftSessionService setMinecraftService(YggdrasilAuthenticationService authService, MinecraftSessionService sessService, Session session) {
        File skinDir = ((PlayerSkinProviderAccessor) mc.getSkinProvider()).getSkinCacheDir();
        ((MinecraftClientAccessor) mc).setSession(session);
        ((MinecraftClientAccessor) mc).setSessionService(sessService);
        ((MinecraftClientAccessor) mc).setSkinProvider(new PlayerSkinProvider(mc.getTextureManager(), skinDir, sessService));
        UserApiService apiService = createUserApiService(authService, session);
        ((MinecraftClientAccessor) mc).setUserApiService(apiService);
        ((MinecraftClientAccessor) mc).setSocialInteractionsManager(new SocialInteractionsManager(mc, apiService));
        ((MinecraftClientAccessor) mc).setProfileKeys(ProfileKeys.create(apiService, session, mc.runDirectory.toPath()));
        ((MinecraftClientAccessor) mc).setAbuseReportContext(AbuseReportContext.create(ReporterEnvironment.ofIntegratedServer(), apiService));
        return sessService;
    }

    public static UserApiService createUserApiService(YggdrasilAuthenticationService authService, Session session) {
        try {
            return authService.createUserApiService(session.getAccessToken());
        } catch (AuthenticationException var4) {
            return UserApiService.OFFLINE;
        }
    }
}
