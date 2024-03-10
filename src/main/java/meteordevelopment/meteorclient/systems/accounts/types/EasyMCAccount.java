/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts.types;

import com.mojang.authlib.Environment;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.util.UndashedUuid;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.mixin.YggdrasilMinecraftSessionServiceAccessor;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.AccountType;
import meteordevelopment.meteorclient.systems.accounts.TokenAccount;
import meteordevelopment.meteorclient.utils.misc.NbtException;
import meteordevelopment.meteorclient.utils.network.Http;
import net.minecraft.client.session.Session;
import net.minecraft.nbt.NbtCompound;

import java.util.Optional;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EasyMCAccount extends Account<EasyMCAccount> implements TokenAccount {

    private static final Environment ENVIRONMENT = new Environment("https://sessionserver.easymc.io", "https://authserver.mojang.com", "EasyMC");
    private static final YggdrasilAuthenticationService SERVICE = new YggdrasilAuthenticationService(((MinecraftClientAccessor) mc).getProxy(), ENVIRONMENT);
    private String token;

    public EasyMCAccount(String token) {
        super(AccountType.EasyMC, token);
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    @Override
    public boolean fetchInfo() {
        // we set the name to the session id after we redeem the token - the token length is 20, session id length is 43
        if (name.length() == 43) return true;

        AuthResponse res = Http.post("https://api.easymc.io/v1/token/redeem")
            .bodyJson("{\"token\":\"" + name + "\"}")
            .sendJson(AuthResponse.class);

        if (res != null) {
            cache.username = res.mcName;
            cache.uuid = res.uuid;

            name = res.session;

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean login() {
        applyLoginEnvironment(SERVICE, YggdrasilMinecraftSessionServiceAccessor.createYggdrasilMinecraftSessionService(SERVICE.getServicesKeySet(), SERVICE.getProxy(), ENVIRONMENT));
        setSession(new Session(cache.username, UndashedUuid.fromStringLenient(cache.uuid), name, Optional.empty(), Optional.empty(), Session.AccountType.MOJANG));

        cache.loadHead();
        return true;
    }

    private static class AuthResponse {
        public String mcName;
        public String uuid;
        public String session;
        public String message;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("type", type.name());
        tag.putString("name", name);
        tag.putString("token", token);
        tag.put("cache", cache.toTag());

        return tag;
    }

    @Override
    public EasyMCAccount fromTag(NbtCompound tag) {
        if (!tag.contains("name") || !tag.contains("cache") || !tag.contains("token")) throw new NbtException();

        name = tag.getString("name");
        token = tag.getString("token");
        cache.fromTag(tag.getCompound("cache"));

        return this;
    }
}
