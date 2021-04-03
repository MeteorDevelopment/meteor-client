/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.accounts.types;

import minegame159.meteorclient.systems.accounts.Account;
import minegame159.meteorclient.systems.accounts.AccountType;
import minegame159.meteorclient.systems.accounts.ProfileResponse;
import minegame159.meteorclient.utils.network.HttpUtils;
import net.minecraft.client.util.Session;

public class CrackedAccount extends Account<CrackedAccount> {
    public CrackedAccount(String name) {
        super(AccountType.Cracked, name);

    }

    @Override
    public boolean fetchInfo() {
        cache.username = name;
        return true;
    }

    @Override
    public boolean fetchHead() {
        try {
            ProfileResponse response = HttpUtils.get("https://api.mojang.com/users/profiles/minecraft/" + cache.username, ProfileResponse.class);
            return cache.makeHead("https://crafatar.com/avatars/" + response.getId() + "?size=8&overlay&default=MHF_Steve");
        } catch (Exception e) {
            return cache.makeHead("steve");
        }
    }

    @Override
    public boolean login() {
        super.login();

        setSession(new Session(name, "", "", "mojang"));
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CrackedAccount)) return false;
        return ((CrackedAccount) o).getUsername().equals(this.getUsername());
    }
}