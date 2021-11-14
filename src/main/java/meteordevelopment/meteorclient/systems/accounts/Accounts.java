/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts;

import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.accounts.types.CrackedAccount;
import meteordevelopment.meteorclient.systems.accounts.types.MicrosoftAccount;
import meteordevelopment.meteorclient.systems.accounts.types.PremiumAccount;
import meteordevelopment.meteorclient.systems.accounts.types.TheAlteningAccount;
import meteordevelopment.meteorclient.utils.misc.NbtException;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Accounts extends System<Accounts> implements Iterable<Account<?>> {
    private List<Account<?>> accounts = new ArrayList<>();

    public Accounts() {
        super("accounts");
    }

    public static Accounts get() {
        return Systems.get(Accounts.class);
    }

    @Override
    public void init() {
        AccountCache.loadSteveHead();
    }

    public void add(Account<?> account) {
        accounts.add(account);
        save();
    }

    public boolean exists(Account<?> account) {
        return accounts.contains(account);
    }

    public void remove(Account<?> account) {
        if (accounts.remove(account)) {
            save();
        }
    }

    public int size() {
        return accounts.size();
    }

    @Override
    public Iterator<Account<?>> iterator() {
        return accounts.iterator();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.put("accounts", NbtUtils.listToTag(accounts));

        return tag;
    }

    @Override
    public Accounts fromTag(NbtCompound tag) {
        MeteorExecutor.execute(() -> accounts = NbtUtils.listFromTag(tag.getList("accounts", 10), tag1 -> {
            NbtCompound t = (NbtCompound) tag1;
            if (!t.contains("type")) return null;

            AccountType type = AccountType.valueOf(t.getString("type"));

            try {
                Account<?> account = switch (type) {
                    case Cracked ->    new CrackedAccount(null).fromTag(t);
                    case Premium ->    new PremiumAccount(null, null).fromTag(t);
                    case Microsoft ->  new MicrosoftAccount(null).fromTag(t);
                    case TheAltening -> new TheAlteningAccount(null).fromTag(t);
                };

                if (account.fetchHead()) return account;
            } catch (NbtException e) {
                return null;
            }

            return null;
        }));

        return this;
    }
}
