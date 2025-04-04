/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.systems.accounts.types;

import com.mojang.util.UndashedUuid;
import motordevelopment.motorclient.systems.accounts.Account;
import motordevelopment.motorclient.systems.accounts.AccountType;
import motordevelopment.motorclient.systems.accounts.MicrosoftLogin;
import net.minecraft.client.session.Session;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class MicrosoftAccount extends Account<MicrosoftAccount> {
    private @Nullable String token;
    public MicrosoftAccount(String refreshToken) {
        super(AccountType.Microsoft, refreshToken);
    }

    @Override
    public boolean fetchInfo() {
        token = auth();
        return token != null;
    }

    @Override
    public boolean login() {
        if (token == null) return false;

        super.login();
        cache.loadHead();

        setSession(new Session(cache.username, UndashedUuid.fromStringLenient(cache.uuid), token, Optional.empty(), Optional.empty(), Session.AccountType.MSA));
        return true;
    }

    private @Nullable String auth() {
        MicrosoftLogin.LoginData data = MicrosoftLogin.login(name);
        if (!data.isGood()) return null;

        name = data.newRefreshToken;
        cache.username = data.username;
        cache.uuid = data.uuid;

        return data.mcToken;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MicrosoftAccount)) return false;
        return ((MicrosoftAccount) o).name.equals(this.name);
    }
}
