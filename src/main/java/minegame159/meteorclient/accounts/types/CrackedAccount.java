/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.accounts.types;

import minegame159.meteorclient.accounts.Account;
import minegame159.meteorclient.accounts.AccountType;
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
        return cache.makeHead("http://meteorclient.com:8082/steve.png");
    }

    @Override
    public boolean login() {
        super.login();

        setSession(new Session(name, "", "", "mojang"));
        return true;
    }
}
