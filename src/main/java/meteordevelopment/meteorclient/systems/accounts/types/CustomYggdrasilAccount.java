/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts.types;

import com.mojang.authlib.exceptions.AuthenticationException;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.AccountType;
import meteordevelopment.meteorclient.systems.accounts.CustomYggdrasilLogin;
import meteordevelopment.meteorclient.utils.misc.NbtException;
import net.minecraft.client.util.Session;
import net.minecraft.nbt.NbtCompound;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CustomYggdrasilAccount extends Account<CustomYggdrasilAccount> {
    private String password, server;

    public CustomYggdrasilAccount(String name, String password, String server) {
        super(AccountType.Yggdrasil, name);
        this.password = password;
        this.server = server;
    }

    @Override
    public boolean fetchInfo() {
        try {
            Session session = CustomYggdrasilLogin.login(name, password, server);

            cache.username = session.getUsername();
            cache.uuid = session.getUuid();

            return true;
        } catch (AuthenticationException e) {
            return false;
        }
    }

    @Override
    public boolean login() {
        try {
            Session session = CustomYggdrasilLogin.login(name, password, server);
            CustomYggdrasilLogin.LocalYggdrasilAuthenticationService service = new CustomYggdrasilLogin.LocalYggdrasilAuthenticationService(((MinecraftClientAccessor) mc).getProxy(), server);
            CustomYggdrasilLogin.applyYggdrasilAccount(service, session);
            cache.username = session.getUsername();
            cache.loadHead();
            return true;
        } catch (AuthenticationException e) {
            if (e.getMessage().contains("Invalid username or password") || e.getMessage().contains("account migrated"))
                MeteorClient.LOG.error("Wrong password.");
            else MeteorClient.LOG.error("Failed to contact the authentication server.");
            return false;
        }
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();

        tag.putString("password", password);
        tag.putString("server", server);

        return tag;
    }

    @Override
    public CustomYggdrasilAccount fromTag(NbtCompound tag) {
        super.fromTag(tag);
        if (!tag.contains("password")) throw new NbtException();

        password = tag.getString("password");
        server = tag.getString("server");

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CustomYggdrasilAccount)) return false;
        return ((CustomYggdrasilAccount) o).name.equals(this.name);
    }
}
