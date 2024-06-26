/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts.types;

import com.mojang.authlib.Environment;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import de.florianmichael.waybackauthlib.InvalidCredentialsException;
import de.florianmichael.waybackauthlib.WaybackAuthLib;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.mixin.YggdrasilMinecraftSessionServiceAccessor;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.AccountType;
import meteordevelopment.meteorclient.systems.accounts.TokenAccount;
import meteordevelopment.meteorclient.utils.misc.NbtException;
import net.minecraft.client.session.Session;
import net.minecraft.nbt.NbtCompound;

import java.util.Optional;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TheAlteningAccount extends Account<TheAlteningAccount> implements TokenAccount {
    private static final Environment ENVIRONMENT = new Environment("http://sessionserver.thealtening.com", "http://authserver.thealtening.com", "The Altening");
    private static final YggdrasilAuthenticationService SERVICE = new YggdrasilAuthenticationService(((MinecraftClientAccessor) mc).getProxy(), ENVIRONMENT);
    private String token;

    public TheAlteningAccount(String token) {
        super(AccountType.TheAltening, token);
        this.token = token;
    }

    @Override
    public boolean fetchInfo() {
        WaybackAuthLib auth = getAuth();

        try {
            auth.logIn();

            cache.username = auth.getCurrentProfile().getName();
            cache.uuid = auth.getCurrentProfile().getId().toString();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean login() {
        applyLoginEnvironment(SERVICE, YggdrasilMinecraftSessionServiceAccessor.createYggdrasilMinecraftSessionService(SERVICE.getServicesKeySet(), SERVICE.getProxy(), ENVIRONMENT));

        WaybackAuthLib auth = getAuth();

        try {
            auth.logIn();
            setSession(new Session(auth.getCurrentProfile().getName(), auth.getCurrentProfile().getId(), auth.getAccessToken(), Optional.empty(), Optional.empty(), Session.AccountType.MOJANG));

            cache.username = auth.getCurrentProfile().getName();
            cache.loadHead();

            return true;
        } catch (InvalidCredentialsException e) {
            MeteorClient.LOG.error("Invalid TheAltening credentials.");
            return false;
        } catch (Exception e) {
            MeteorClient.LOG.error("Failed to login with TheAltening.");
            return false;
        }
    }

    private WaybackAuthLib getAuth() {
        WaybackAuthLib auth = new WaybackAuthLib(ENVIRONMENT.servicesHost());

        auth.setUsername(name);
        auth.setPassword("Meteor on Crack!");

        return auth;
    }

    @Override
    public String getToken() {
        return token;
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
    public TheAlteningAccount fromTag(NbtCompound tag) {
        if (!tag.contains("name") || !tag.contains("cache") || !tag.contains("token")) throw new NbtException();

        name = tag.getString("name");
        token = tag.getString("token");
        cache.fromTag(tag.getCompound("cache"));

        return this;
    }
}
