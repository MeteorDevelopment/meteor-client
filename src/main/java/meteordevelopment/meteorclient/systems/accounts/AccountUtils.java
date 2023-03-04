/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.mixin.PlayerSkinProviderAccessor;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.report.AbuseReportContext;
import net.minecraft.client.report.ReporterEnvironment;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.util.ProfileKeys;
import net.minecraft.client.util.Session;
import net.minecraft.network.encryption.SignatureVerifier;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AccountUtils {
    public static void applyLoginEnvironment(YggdrasilAuthenticationService authService, MinecraftSessionService sessService, Session session) {
        ((MinecraftClientAccessor) mc).setSession(session);
        ((MinecraftClientAccessor) mc).setAuthenticationService(authService);
        ((MinecraftClientAccessor) mc).setSessionService(sessService);
        ((MinecraftClientAccessor) mc).setServicesSignatureVerifier(SignatureVerifier.create(authService.getServicesKey()));
        ((MinecraftClientAccessor) mc).setSkinProvider(new PlayerSkinProvider(mc.getTextureManager(), ((PlayerSkinProviderAccessor) mc.getSkinProvider()).getSkinCacheDir(), sessService));
        UserApiService apiService;
        try {
            apiService = authService.createUserApiService(session.getAccessToken());
        } catch (AuthenticationException e) {
            apiService = UserApiService.OFFLINE;
        }
        ((MinecraftClientAccessor) mc).setUserApiService(apiService);
        ((MinecraftClientAccessor) mc).setSocialInteractionsManager(new SocialInteractionsManager(mc, apiService));
        ((MinecraftClientAccessor) mc).setProfileKeys(ProfileKeys.create(apiService, session, mc.runDirectory.toPath()));
        ((MinecraftClientAccessor) mc).setAbuseReportContext(AbuseReportContext.create(ReporterEnvironment.ofIntegratedServer(), apiService));
    }
}
