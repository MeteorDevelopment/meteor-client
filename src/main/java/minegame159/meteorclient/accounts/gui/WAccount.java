/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.accounts.gui;

import minegame159.meteorclient.accounts.Account;
import minegame159.meteorclient.accounts.Accounts;
import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.utils.network.MeteorExecutor;
import minegame159.meteorclient.utils.network.OnlinePlayers;
import net.minecraft.client.MinecraftClient;

public class WAccount extends WTable {
    public WAccount(AccountsScreen screen, Account<?> account, Runnable onRemoved) {
        // Head
        add(new WTexture(16, 16, 90, account.getCache().getHeadTexture()));

        // Name
        WLabel name = add(new WLabel(account.getUsername())).getWidget();
        if (MinecraftClient.getInstance().getSession().getUsername().equalsIgnoreCase(account.getUsername())) name.color = GuiConfig.get().loggedInText;

        // Account Type
        WLabel type = add(new WLabel("(" + account.getType() + ")")).fillX().right().getWidget();
        type.color = GuiConfig.get().accountTypeText;

        // Login
        WButton login = add(new WButton("Login")).getWidget();
        login.action = () -> {
            login.freezeWidth();
            login.setText("...");
            screen.locked = true;

            MeteorExecutor.execute(() -> {
                if (account.login()) {
                    name.setText(account.getUsername());

                    Accounts.get().save();
                    OnlinePlayers.forcePing();

                    screen.clear();
                    screen.initWidgets();
                }

                login.setText("Login");
                screen.locked = false;
            });
        };

        // Remove
        WMinus minus = add(new WMinus()).getWidget();
        minus.action = () -> {
            Accounts.get().remove(account);
            if (onRemoved != null) onRemoved.run();
        };
    }
}
