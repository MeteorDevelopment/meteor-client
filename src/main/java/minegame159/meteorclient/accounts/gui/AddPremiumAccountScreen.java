/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.accounts.gui;

import minegame159.meteorclient.accounts.Accounts;
import minegame159.meteorclient.accounts.types.PremiumAccount;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WLabel;

public class AddPremiumAccountScreen extends WindowScreen {
    public AddPremiumAccountScreen() {
        super("Add Premium Account", true);

        // Email
        add(new WLabel("Email:"));
        WAccountField email = add(new WAccountField("", 400)).getWidget();
        email.setFocused(true);
        row();

        // Password
        add(new WLabel("Password:"));
        WAccountField password = add(new WAccountField("", 400)).getWidget();
        row();

        // Add
        WButton add = add(new WButton("Add")).fillX().expandX().getWidget();
        add.action = () -> {
            PremiumAccount account = new PremiumAccount(email.getText(), password.getText());
            if (!email.getText().isEmpty() && !password.getText().isEmpty() && email.getText().contains("@") && !Accounts.get().exists(account)) {
                AccountsScreen.addAccount(add, this, account);
            }
        };
    }
}