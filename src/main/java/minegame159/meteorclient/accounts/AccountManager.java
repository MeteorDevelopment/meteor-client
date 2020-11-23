/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.accounts;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.accounts.types.CrackedAccount;
import minegame159.meteorclient.accounts.types.PremiumAccount;
import minegame159.meteorclient.accounts.types.TheAlteningAccount;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.utils.MeteorExecutor;
import minegame159.meteorclient.utils.NbtException;
import minegame159.meteorclient.utils.NbtUtils;
import minegame159.meteorclient.utils.Savable;
import net.minecraft.nbt.CompoundTag;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AccountManager extends Savable<AccountManager> implements Iterable<Account<?>> {
    public static final AccountManager INSTANCE = new AccountManager();

    private List<Account<?>> accounts = new ArrayList<>();

    private AccountManager() {
        super(new File(MeteorClient.FOLDER, "accounts.nbt"));
    }

    public void add(Account<?> account) {
        accounts.add(account);
        MeteorClient.EVENT_BUS.post(EventStore.accountListChangedEvent());
        save();
    }

    public void remove(Account<?> account) {
        if (accounts.remove(account)) {
            MeteorClient.EVENT_BUS.post(EventStore.accountListChangedEvent());
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
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.put("accounts", NbtUtils.listToTag(accounts));

        return tag;
    }

    @Override
    public AccountManager fromTag(CompoundTag tag) {
        MeteorExecutor.execute(() -> accounts = NbtUtils.listFromTag(tag.getList("accounts", 10), tag1 -> {
            CompoundTag t = (CompoundTag) tag1;
            if (!t.contains("type")) return null;

            AccountType type = AccountType.valueOf(t.getString("type"));

            try {
                Account<?> account = null;
                if (type == AccountType.Cracked) {
                    account = new CrackedAccount(null).fromTag(t);
                } else if (type == AccountType.Premium) {
                    account = new PremiumAccount(null, null).fromTag(t);
                } else if (type == AccountType.TheAltening) {
                    account = new TheAlteningAccount(null).fromTag(t);
                }

                if (account.fetchHead()) return account;
            } catch (NbtException e) {
                return null;
            }

            return null;
        }));

        return this;
    }
}
