/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.config.Config;

public class MeteorAccount {
    public static MeteorAccount ACCOUNT;

    public String username;
    public String email;
    public boolean admin;
    public boolean donator;
    public String discord_id;
    public int max_mc_accounts;
    public String[] mc_accounts;
    public String cape;
    public boolean can_have_custom_cape;
    public String discord_name;
    public String discord_avatar;
    public Cape[] capes;

    public static void login() {
        String token = Config.get().token;
        if (token == null || token.isBlank()) return;

        ACCOUNT = Http.get("https://meteorclient.com/api/account/info").bearer(token).sendJson(MeteorAccount.class);
        MeteorClient.LOG.info(ACCOUNT.toString());
    }

    private static class Cape {
        public String id;
        public String url;
        public String title;
        public boolean current;
    }
}
