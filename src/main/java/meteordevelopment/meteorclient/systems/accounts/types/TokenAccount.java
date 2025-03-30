package meteordevelopment.meteorclient.systems.accounts.types;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import java.util.Optional;

import javax.annotation.Nullable;

import com.mojang.authlib.Environment;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import de.florianmichael.waybackauthlib.WaybackAuthLib;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.mixin.YggdrasilMinecraftSessionServiceAccessor;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.AccountType;
import meteordevelopment.meteorclient.utils.misc.NbtException;
import net.minecraft.client.session.Session;
import net.minecraft.nbt.NbtCompound;

// TODO
public class TokenAccount extends Account<TokenAccount> {
    public TokenAccount(String token) {
        super(AccountType.Token, token);
        this.token = token;
    }

    private static final Environment ENVIRONMENT = new Environment("http://sessionserver.thealtening.com",
            "http://authserver.thealtening.com", "The Altening");
    private static final YggdrasilAuthenticationService SERVICE = new YggdrasilAuthenticationService(
            ((MinecraftClientAccessor) mc).getProxy(), ENVIRONMENT);
    private String token;
    private @Nullable WaybackAuthLib auth;

    public String getToken() {
        return token;
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
            MeteorClient.LOG.error("Invalid TheAltening credentials.");
            return false;
        } catch (Exception e) {
            MeteorClient.LOG.error("Failed to fetch info for TheAltening account!");
            return false;
        }
    }

    @Override
    public boolean login() {
        if (auth == null)
            return false;
        applyLoginEnvironment(SERVICE, YggdrasilMinecraftSessionServiceAccessor
                .createYggdrasilMinecraftSessionService(SERVICE.getServicesKeySet(), SERVICE.getProxy(), ENVIRONMENT));

        try {
            setSession(new Session(auth.getCurrentProfile().getName(), auth.getCurrentProfile().getId(),
                    auth.getAccessToken(), Optional.empty(), Optional.empty(), Session.AccountType.MOJANG));
            return true;
        } catch (Exception e) {
            MeteorClient.LOG.error("Failed to login with a token.");
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
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("type", type.name());
        tag.putString("name", name);
        tag.putString("token", token);
        tag.put("cache", cache.toTag());

        return tag;
    }

    @Override
    public TokenAccount fromTag(NbtCompound tag) {
        if (!tag.contains("name") || !tag.contains("cache") || !tag.contains("token"))
            throw new NbtException();

        name = tag.getString("name");
        token = tag.getString("token");
        cache.fromTag(tag.getCompound("cache"));

        return this;
    }
}
