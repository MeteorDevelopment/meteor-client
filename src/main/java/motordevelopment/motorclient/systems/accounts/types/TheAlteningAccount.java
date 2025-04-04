/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.systems.accounts.types;

import com.mojang.authlib.Environment;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import de.florianmichael.waybackauthlib.InvalidCredentialsException;
import de.florianmichael.waybackauthlib.WaybackAuthLib;
import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.mixin.MinecraftClientAccessor;
import motordevelopment.motorclient.mixin.YggdrasilMinecraftSessionServiceAccessor;
import motordevelopment.motorclient.systems.accounts.Account;
import motordevelopment.motorclient.systems.accounts.AccountType;
import motordevelopment.motorclient.systems.accounts.TokenAccount;
import motordevelopment.motorclient.utils.misc.NbtException;
import net.minecraft.client.session.Session;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static motordevelopment.motorclient.MotorClient.mc;

public class TheAlteningAccount extends Account<TheAlteningAccount> implements TokenAccount {
    private static final Environment ENVIRONMENT = new Environment("http://sessionserver.thealtening.com", "http://authserver.thealtening.com", "The Altening");
    private static final YggdrasilAuthenticationService SERVICE = new YggdrasilAuthenticationService(((MinecraftClientAccessor) mc).getProxy(), ENVIRONMENT);
    private String token;
    private @Nullable WaybackAuthLib auth;

    public TheAlteningAccount(String token) {
        super(AccountType.TheAltening, token);
        this.token = token;
    }

    @Override
    public boolean fetchInfo() {
        auth = getAuth();

        try {
            auth.logIn();

            cache.username = auth.getCurrentProfile().getName();
            cache.uuid = auth.getCurrentProfile().getId().toString();
            cache.loadHead();

            return true;
        } catch (InvalidCredentialsException e) {
            MotorClient.LOG.error("Invalid TheAltening credentials.");
            return false;
        } catch (Exception e) {
            MotorClient.LOG.error("Failed to fetch info for TheAltening account!");
            return false;
        }
    }

    @Override
    public boolean login() {
        if (auth == null) return false;
        applyLoginEnvironment(SERVICE, YggdrasilMinecraftSessionServiceAccessor.createYggdrasilMinecraftSessionService(SERVICE.getServicesKeySet(), SERVICE.getProxy(), ENVIRONMENT));

        try {
            setSession(new Session(auth.getCurrentProfile().getName(), auth.getCurrentProfile().getId(), auth.getAccessToken(), Optional.empty(), Optional.empty(), Session.AccountType.MOJANG));
            return true;
        } catch (Exception e) {
            MotorClient.LOG.error("Failed to login with TheAltening.");
            return false;
        }
    }

    private WaybackAuthLib getAuth() {
        WaybackAuthLib auth = new WaybackAuthLib(ENVIRONMENT.servicesHost());

        auth.setUsername(name);
        auth.setPassword("motor on Crack!");

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
