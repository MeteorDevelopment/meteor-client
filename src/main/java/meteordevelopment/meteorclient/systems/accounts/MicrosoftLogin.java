/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.network.Http;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class MicrosoftLogin {
    private MicrosoftLogin() {
    }

    public static class LoginData {
        public String mcToken;
        public String newRefreshToken;
        public String uuid, username;

        public LoginData() {}

        public LoginData(String mcToken, String newRefreshToken, String uuid, String username) {
            this.mcToken = mcToken;
            this.newRefreshToken = newRefreshToken;
            this.uuid = uuid;
            this.username = username;
        }

        public boolean isGood() {
            return mcToken != null;
        }
    }

    private static final String CLIENT_ID = "4673b348-3efa-4f6a-bbb6-34e141cdc638";
    private static final int PORT = 9675;

    private static volatile HttpServer server;
    private static volatile Consumer<String> callback;

    public static String getRefreshToken(Consumer<String> callback) {
        MicrosoftLogin.callback = callback;

        startServer();
        String url = "https://login.live.com/oauth20_authorize.srf?client_id=" + CLIENT_ID + "&response_type=code&redirect_uri=http://127.0.0.1:" + PORT + "&scope=XboxLive.signin%20offline_access&prompt=select_account";
        Util.getOperatingSystem().open(url);

        return url;
    }

    public static LoginData login(String refreshToken) {
        // Refresh access token
        AuthTokenResponse res = Http.post("https://login.live.com/oauth20_token.srf")
            .bodyForm("client_id=" + CLIENT_ID + "&refresh_token=" + refreshToken + "&grant_type=refresh_token&redirect_uri=http://127.0.0.1:" + PORT)
            .sendJson(AuthTokenResponse.class);

        if (res == null) return new LoginData();

        String accessToken = res.access_token;
        refreshToken = res.refresh_token;

        // XBL
        XblXstsResponse xblRes = Http.post("https://user.auth.xboxlive.com/user/authenticate")
            .bodyJson("{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d=" + accessToken + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}")
            .sendJson(XblXstsResponse.class);

        if (xblRes == null) return new LoginData();

        // XSTS
        XblXstsResponse xstsRes = Http.post("https://xsts.auth.xboxlive.com/xsts/authorize")
            .bodyJson("{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" + xblRes.Token + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}")
            .sendJson(XblXstsResponse.class);

        if (xstsRes == null) return new LoginData();

        // Minecraft
        McResponse mcRes = Http.post("https://api.minecraftservices.com/authentication/login_with_xbox")
            .bodyJson("{\"identityToken\":\"XBL3.0 x=" + xblRes.DisplayClaims.xui[0].uhs + ";" + xstsRes.Token + "\"}")
            .sendJson(McResponse.class);

        if (mcRes == null) return new LoginData();

        // Check game ownership
        GameOwnershipResponse gameOwnershipRes = Http.get("https://api.minecraftservices.com/entitlements/mcstore")
            .bearer(mcRes.access_token)
            .sendJson(GameOwnershipResponse.class);

        if (gameOwnershipRes == null || !gameOwnershipRes.hasGameOwnership()) return new LoginData();

        // Profile
        ProfileResponse profileRes = Http.get("https://api.minecraftservices.com/minecraft/profile")
            .bearer(mcRes.access_token)
            .sendJson(ProfileResponse.class);

        if (profileRes == null) return new LoginData();

        return new LoginData(mcRes.access_token, refreshToken, profileRes.id, profileRes.name);
    }

    private static void startServer() {
        if (server != null) return;

        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);

            server.createContext("/", MicrosoftLogin::handleRequest);
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();
        } catch (IOException e) {
            MeteorClient.LOG.error("Error starting Microsoft login server", e);
            stopServer();
        }
    }

    public static void stopServer() {
        if (server == null) return;

        server.stop(0);
        server = null;

        callback = null;
    }

    private static void handleRequest(HttpExchange req) throws IOException {
        if (req.getRequestMethod().equals("GET")) {
            // Login
            List<Pair<String, String>> query = parseURL(req.getRequestURI().getRawQuery());

            boolean ok = false;
            for (Pair<String, String> pair : query) {
                if (pair.getLeft().equals("code")) {
                    handleCode(pair.getRight());

                    ok = true;
                    break;
                }
            }


            if (!ok) {
                writeText(req, "Cannot authenticate.");
                callback.accept(null);
            }
            else writeText(req, "You may now close this page.");
        }

        stopServer();
    }

    private static void handleCode(String code) {
        AuthTokenResponse res = Http.post("https://login.live.com/oauth20_token.srf")
            .bodyForm("client_id=" + CLIENT_ID + "&code=" + code + "&grant_type=authorization_code&redirect_uri=http://127.0.0.1:" + PORT)
            .sendJson(AuthTokenResponse.class);

        if (res == null) callback.accept(null);
        else callback.accept(res.refresh_token);
    }

    private static void writeText(HttpExchange req, String text) throws IOException {
        byte[] responseBody = text.getBytes(StandardCharsets.UTF_8);
        req.sendResponseHeaders(200, responseBody.length);
        try (var out = req.getResponseBody()) {
            out.write(responseBody);
        }
    }

    // reimplementation of apache https URLEncodedUtils#parse
    private static List<Pair<String, String>> parseURL(String string) {
        List<Pair<String, String>> query = new ArrayList<>();
        char[] buf = string.toCharArray();
        int i = 0;
        while (i < buf.length) {
            StringBuilder name = new StringBuilder();
            StringBuilder value = new StringBuilder();

            for (; i < buf.length; i++) {
                if (buf[i] == '&' || buf[i] == ';' || buf[i] == '=') break;
                else name.append(buf[i]);
            }

            if (i < buf.length) {
                char ch = buf[i];
                i += 1;

                if (ch == '=') {
                    for (; i < buf.length; i++) {
                        if (buf[i] == '&' || buf[i] == ';') {
                            i += 1;
                            break;
                        } else value.append(buf[i]);
                    }
                }
            }

            if (!name.isEmpty()) {
                query.add(new Pair<>(urlDecode(name.toString()), urlDecode(value.toString())));
            }
        }

        return query;
    }

    // near 1:1 copy of apache https URLEncodedUtils#urlDecode
    // not really necessary for this particular use case but doesn't hurt to be thorough
    private static String urlDecode(String s) {
        if (s == null) return null;

        final ByteBuffer bb = ByteBuffer.allocate(s.length());
        final CharBuffer cb = CharBuffer.wrap(s);
        while (cb.hasRemaining()) {
            final char c = cb.get();
            if (c == '%' && cb.remaining() >= 2) {
                final char uc = cb.get();
                final char lc = cb.get();
                final int u = Character.digit(uc, 16);
                final int l = Character.digit(lc, 16);
                if (u != -1 && l != -1) {
                    bb.put((byte) ((u << 4) + l));
                } else {
                    bb.put((byte) '%');
                    bb.put((byte) uc);
                    bb.put((byte) lc);
                }
            } else if (c == '+') {
                bb.put((byte) ' ');
            } else {
                bb.put((byte) c);
            }
        }
        bb.flip();
        return StandardCharsets.UTF_8.decode(bb).toString();
    }

    private static class AuthTokenResponse {
        public String access_token;
        public String refresh_token;
    }

    private static class XblXstsResponse {
        public String Token;
        public DisplayClaims DisplayClaims;

        private static class DisplayClaims {
            private Claim[] xui;

            private static class Claim {
                private String uhs;
            }
        }
    }

    private static class McResponse {
        public String access_token;
    }

    private static class GameOwnershipResponse {
        private Item[] items;

        private static class Item {
            private String name;
        }

        private boolean hasGameOwnership() {
            boolean hasProduct = false;
            boolean hasGame = false;

            for (Item item : items) {
                if (item.name.equals("product_minecraft")) hasProduct = true;
                else if (item.name.equals("game_minecraft")) hasGame = true;
            }

            return hasProduct && hasGame;
        }
    }

    private static class ProfileResponse {
        public String id;
        public String name;
    }
}
