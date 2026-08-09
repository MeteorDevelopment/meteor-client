/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.util.Util;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import net.raphimc.minecraftauth.msa.data.MsaConstants;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class MicrosoftLogin {
    private static final MsaApplicationConfig APPLICATION_CONFIG = new MsaApplicationConfig(
        MsaConstants.JAVA_TITLE_ID,
        MsaConstants.SCOPE_TITLE_AUTH
    );

    private static final AtomicReference<Future<?>> loginTask = new AtomicReference<>();
    private static final AtomicBoolean cancelled = new AtomicBoolean();

    private MicrosoftLogin() {
    }

    public record LoginData(String mcToken, String newRefreshToken, String uuid, String username) {
    }

    public static String getRefreshToken(Consumer<String> callback) {
        cancelLogin();
        cancelled.set(false);

        CompletableFuture<String> urlFuture = new CompletableFuture<>();

        loginTask.set(MeteorExecutor.executor.submit(() -> {
            try {
                JavaAuthManager authManager = JavaAuthManager.create(MinecraftAuth.createHttpClient())
                    .msaApplicationConfig(APPLICATION_CONFIG)
                    .login(DeviceCodeMsaAuthService::new, (Consumer<MsaDeviceCode>) deviceCode -> {
                        String urlString = deviceCode.getDirectVerificationUri();
                        urlFuture.complete(urlString);
                        Util.getOperatingSystem().open(urlString);
                    });

                MsaToken msaToken = authManager.getMsaToken().getUpToDate();

                if (!cancelled.get()) {
                    callback.accept(msaToken.getRefreshToken());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!urlFuture.isDone()) urlFuture.completeExceptionally(e);
                if (cancelled.get()) return;
                MeteorClient.LOG.error("Error logging into Microsoft account", e);
                callback.accept(null);
            } catch (Exception e) {
                if (!urlFuture.isDone()) urlFuture.completeExceptionally(e);
                if (cancelled.get()) return;
                MeteorClient.LOG.error("Error logging into Microsoft account", e);
                callback.accept(null);
            }

        }));

        try {
            return urlFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while starting Microsoft login", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to start Microsoft login", e.getCause());
        }
    }

    public static @Nullable LoginData login(String refreshToken) {
        try {
            JavaAuthManager authManager = JavaAuthManager.create(MinecraftAuth.createHttpClient())
                .msaApplicationConfig(APPLICATION_CONFIG)
                .login(refreshToken);

            MsaToken msaToken = authManager.getMsaToken().getUpToDate();
            MinecraftToken minecraftToken = authManager.getMinecraftToken().getUpToDate();
            MinecraftProfile profile = authManager.getMinecraftProfile().getUpToDate();

            return new LoginData(
                minecraftToken.getToken(),
                msaToken.getRefreshToken(),
                profile.getId().toString(),
                profile.getName()
            );
        } catch (Exception e) {
            MeteorClient.LOG.error("Error logging into Microsoft account", e);
            return null;
        }
    }

    public static void cancelLogin() {
        cancelled.set(true);

        Future<?> task = loginTask.getAndSet(null);

        if (task != null) {
            task.cancel(true);
        }
    }
}
