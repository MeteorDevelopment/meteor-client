/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.accounts.gui;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.accounts.Account;
import minegame159.meteorclient.accounts.Accounts;
import minegame159.meteorclient.events.meteor.AccountListChangedEvent;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.MinecraftClient;

public class AccountsScreen extends WindowScreen {
    public AccountsScreen() {
        super("Accounts", true);

        initWidgets();
    }

    void initWidgets() {
        // Accounts
        if (Accounts.get().size() > 0) {
            WTable t = add(new WTable()).fillX().expandX().getWidget();
            row();

            for (Account<?> account : Accounts.get()) {
                t.add(new WAccount(this, account)).fillX().expandX();
                t.row();
            }
        }

        // Add account
        WTable t = add(new WTable()).fillX().expandX().getWidget();
        addButton(t, "Cracked", () -> MinecraftClient.getInstance().openScreen(new AddCrackedAccountScreen()));
        addButton(t, "Premium", () -> MinecraftClient.getInstance().openScreen(new AddPremiumAccountScreen()));
        addButton(t, "The Altening", () -> MinecraftClient.getInstance().openScreen(new AddTheAlteningAccountScreen()));
    }

    private void addButton(WTable t, String text, Runnable action) {
        WButton button = t.add(new WButton(text)).fillX().expandX().getWidget();
        button.action = action;
    }

    @EventHandler
    private void onAccountListChanged(AccountListChangedEvent event) {
        clear();
        initWidgets();
    }

    static void addAccount(WButton add, WidgetScreen screen, Account<?> account) {
        add.setText("...");
        screen.locked = true;

        MeteorExecutor.execute(() -> {
            if (account.fetchInfo() && account.fetchHead()) {
                Accounts.get().add(account);
                screen.locked = false;
                screen.onClose();
            }

            add.setText("Add");
            screen.locked = false;
        });
    }
}
