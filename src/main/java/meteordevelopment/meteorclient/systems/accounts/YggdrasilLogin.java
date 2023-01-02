/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

// Credit to https://github.com/IAFEnvoy/AccountSwitcher

package meteordevelopment.meteorclient.systems.accounts;

import com.google.common.collect.Iterables;
import com.google.gson.*;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.InsecureTextureException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.util.UUIDTypeAdapter;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.mixin.PlayerSkinProviderAccessor;
import meteordevelopment.meteorclient.utils.network.Http;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.util.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class YggdrasilLogin {
    public static Session login(String name, String password, String server) throws AuthenticationException {
        try {
            String url = server + "/authserver/authenticate";
            JsonObject agent = new JsonObject();
            agent.addProperty("name", "Minecraft");
            agent.addProperty("version", 1);

            JsonObject root = new JsonObject();
            root.add("agent", agent);
            root.addProperty("username", name);
            root.addProperty("password", password);

            String data = Http.post(url).bodyJson(root).sendString();
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            if (json.has("error")) {
                throw new AuthenticationException(json.get("errorMessage").getAsString());
            }
            String token = json.get("accessToken").getAsString();
            String uuid = json.get("selectedProfile").getAsJsonObject().get("id").getAsString();
            String username = json.get("selectedProfile").getAsJsonObject().get("name").getAsString();
            return new Session(username, uuid, token, Optional.empty(), Optional.empty(), Session.AccountType.MOJANG);
        } catch (Exception e) {
            throw new AuthenticationException(e);
        }
    }

    public static void applyYggdrasilAccount(LocalYggdrasilAuthenticationService authService, Session session) throws AuthenticationException {
        MinecraftSessionService service;
        if (authService.server.equals(""))
            service = new LocalYggdrasilMinecraftSessionService(authService, authService.server);
        else service = authService.createMinecraftSessionService();
        File skinDir = ((PlayerSkinProviderAccessor) mc.getSkinProvider()).getSkinCacheDir();
        ((MinecraftClientAccessor) mc).setSession(session);
        ((MinecraftClientAccessor) mc).setSessionService(service);
        ((MinecraftClientAccessor) mc).setSkinProvider(new PlayerSkinProvider(mc.getTextureManager(), skinDir, service));
        try {
            // If available
            UserApiService userApiService = authService.createUserApiService(session.getAccessToken());
            ((MinecraftClientAccessor) mc).setUserApiService(userApiService);
            ((MinecraftClientAccessor) mc).setSocialInteractionsManager(new SocialInteractionsManager(mc, userApiService));
        } catch (Exception ignored) {}
    }

    public static class LocalYggdrasilApi implements Environment {
        private final String url;

        public LocalYggdrasilApi(String serverUrl) {
            this.url = serverUrl;
        }

        @Override
        public String getAuthHost() {
            return url + "/authserver";
        }

        @Override
        public String getAccountsHost() {
            return url + "/api";
        }

        @Override
        public String getSessionHost() {
            return url + "/sessionserver";
        }

        @Override
        public String getServicesHost() {
            return url + "/minecraftservices";
        }

        @Override
        public String getName() {
            return "Custom-Yggdrasil";
        }

        @Override
        public String asString() {
            return new StringJoiner(", ", "", "")
                .add("authHost='" + getAuthHost() + "'")
                .add("accountsHost='" + getAccountsHost() + "'")
                .add("sessionHost='" + getSessionHost() + "'")
                .add("servicesHost='" + getServicesHost() + "'")
                .add("name='" + getName() + "'")
                .toString();
        }
    }

    public static class LocalYggdrasilMinecraftSessionService extends YggdrasilMinecraftSessionService {
        private static final Logger LOGGER = LogManager.getLogger();
        private final PublicKey publicKey;
        private final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();

        public LocalYggdrasilMinecraftSessionService(YggdrasilAuthenticationService service, String serverUrl) {
            super(service, new LocalYggdrasilApi(serverUrl));
            String data = Http.get("https://" + serverUrl + "/api/yggdrasil").sendString();
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            this.publicKey = getPublicKey(json.get("signaturePublickey").getAsString());
        }

        private static PublicKey getPublicKey(String key) {
            key = key.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
            try {
                byte[] byteKey = Base64.getDecoder().decode(key.replace("\n", ""));
                X509EncodedKeySpec spec = new X509EncodedKeySpec(byteKey);
                KeyFactory factory = KeyFactory.getInstance("RSA");
                return factory.generatePublic(spec);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(final GameProfile profile, final boolean requireSecure) {
            final Property textureProperty = Iterables.getFirst(profile.getProperties().get("textures"), null);

            if (textureProperty == null)
                return new HashMap<>();

            if (requireSecure) {
                if (!textureProperty.hasSignature()) {
                    LOGGER.error("Signature is missing from textures payload");
                    throw new InsecureTextureException("Signature is missing from textures payload");
                }
                if (!textureProperty.isSignatureValid(publicKey)) {
                    LOGGER.error("Textures payload has been tampered with (signature invalid)");
                    throw new InsecureTextureException("Textures payload has been tampered with (signature invalid)");
                }
            }

            final MinecraftTexturesPayload result;
            try {
                final String json = new String(org.apache.commons.codec.binary.Base64.decodeBase64(textureProperty.getValue()), StandardCharsets.UTF_8);
                result = gson.fromJson(json, MinecraftTexturesPayload.class);
            } catch (final JsonParseException e) {
                LOGGER.error("Could not decode textures payload", e);
                return new HashMap<>();
            }

            if (result == null || result.getTextures() == null)
                return new HashMap<>();

            return result.getTextures();
        }
    }

    public static class LocalYggdrasilAuthenticationService extends YggdrasilAuthenticationService {
        public final String server;

        public LocalYggdrasilAuthenticationService(Proxy proxy, String server) {
            super(proxy, new LocalYggdrasilApi(server));
            this.server = server;
        }
    }

}
