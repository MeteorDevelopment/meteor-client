/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.widgets;

import motordevelopment.motorclient.gui.WidgetScreen;
import motordevelopment.motorclient.gui.screens.accounts.AccountInfoScreen;
import motordevelopment.motorclient.gui.widgets.containers.WHorizontalList;
import motordevelopment.motorclient.gui.widgets.pressable.WButton;
import motordevelopment.motorclient.gui.widgets.pressable.WMinus;
import motordevelopment.motorclient.systems.accounts.Account;
import motordevelopment.motorclient.systems.accounts.Accounts;
import motordevelopment.motorclient.systems.accounts.TokenAccount;
import motordevelopment.motorclient.utils.network.MotorExecutor;
import motordevelopment.motorclient.utils.render.color.Color;

import static motordevelopment.motorclient.MotorClient.mc;

public abstract class WAccount extends WHorizontalList {
    public Runnable refreshScreenAction;
    private final WidgetScreen screen;
    private final Account<?> account;

    public WAccount(WidgetScreen screen, Account<?> account) {
        this.screen = screen;
        this.account = account;
    }

    protected abstract Color loggedInColor();
    protected abstract Color accountTypeColor();

    @Override
    public void init() {
        // Head
        add(theme.texture(32, 32, account.getCache().getHeadTexture().needsRotate() ? 90 : 0, account.getCache().getHeadTexture()));

        // Name
        WLabel name = add(theme.label(account.getUsername())).widget();
        if (mc.getSession().getUsername().equalsIgnoreCase(account.getUsername())) name.color = loggedInColor();

        // Type
        WLabel label = add(theme.label("(" + account.getType() + ")")).expandCellX().right().widget();
        label.color = accountTypeColor();

        // Info
        if (account instanceof TokenAccount) {
            WButton info = add(theme.button("Info")).widget();
            info.action = () -> mc.setScreen(new AccountInfoScreen(theme, account));
        }

        // Login
        WButton login = add(theme.button("Login")).widget();
        login.action = () -> {
            login.minWidth = login.width;
            login.set("...");
            screen.locked = true;

            MotorExecutor.execute(() -> {
                if (account.fetchInfo() && account.login()) {
                    name.set(account.getUsername());

                    Accounts.get().save();

                    screen.taskAfterRender = refreshScreenAction;
                }

                login.minWidth = 0;
                login.set("Login");
                screen.locked = false;
            });
        };

        // Remove
        WMinus remove = add(theme.minus()).widget();
        remove.action = () -> {
            Accounts.get().remove(account);
            if (refreshScreenAction != null) refreshScreenAction.run();
        };
    }
}
