/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts.types;

import com.mojang.authlib.Agent;
import com.mojang.authlib.Environment;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.mixin.YggdrasilMinecraftSessionServiceAccessor;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.AccountType;
import net.minecraft.client.util.Session;

import java.util.Optional;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TheAlteningAccount extends Account<TheAlteningAccount> {
    private static final Environment ENVIRONMENT = Environment.create("http://authserver.thealtening.com", "https://api.mojang.com", "http://sessionserver.thealtening.com", "https://api.minecraftservices.com", "The Altening");
    private static final YggdrasilAuthenticationService SERVICE = new YggdrasilAuthenticationService(((MinecraftClientAccessor) mc).getProxy(), "", ENVIRONMENT);

    public TheAlteningAccount(String token) {
        super(AccountType.TheAltening, token);
    }

    @Override
    public boolean fetchInfo() {
        YggdrasilUserAuthentication auth = getAuth();

        try {
            auth.logIn();

            cache.username = auth.getSelectedProfile().getName();
            cache.uuid = auth.getSelectedProfile().getId().toString();

            return true;
        } catch (AuthenticationException e) {
            return false;
        }
    }

    @Override
    public boolean login() {
        applyLoginEnvironment(SERVICE, YggdrasilMinecraftSessionServiceAccessor.createYggdrasilMinecraftSessionService(SERVICE, ENVIRONMENT));

        YggdrasilUserAuthentication auth = getAuth();

        try {
            auth.logIn();
            setSession(new Session(auth.getSelectedProfile().getName(), auth.getSelectedProfile().getId().toString(), auth.getAuthenticatedToken(), Optional.empty(), Optional.empty(), Session.AccountType.MOJANG));

            cache.username = auth.getSelectedProfile().getName();
            cache.loadHead();

            return true;
        } catch (AuthenticationException e) {
            MeteorClient.LOG.error("Failed to login with TheAltening.");
            return false;
        }
    }

    private YggdrasilUserAuthentication getAuth() {
        YggdrasilUserAuthentication auth = (YggdrasilUserAuthentication) SERVICE.createUserAuthentication(Agent.MINECRAFT);

        auth.setUsername(name);
        auth.setPassword("Meteor on Crack!");

        return auth;
    }
}
