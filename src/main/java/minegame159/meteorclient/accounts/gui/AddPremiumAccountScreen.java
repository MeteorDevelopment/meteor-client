/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.accounts.gui;

import minegame159.meteorclient.accounts.types.PremiumAccount;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTextBox;

public class AddPremiumAccountScreen extends WindowScreen {
    public AddPremiumAccountScreen() {
        super("Add Premium Account", true);

        // Email
        add(new WLabel("Email:"));
        WTextBox email = add(new WTextBox("", 400)).getWidget();
        email.setFocused(true);
        row();

        // Password
        add(new WLabel("Password:"));
        WTextBox password = add(new WTextBox("", 400)).getWidget();
        row();

        // Add
        WButton add = add(new WButton("Add")).fillX().expandX().getWidget();
        add.action = () -> {
            if (!email.getText().isEmpty() && !password.getText().isEmpty() && email.getText().contains("@")) {
                AccountsScreen.addAccount(add, this, new PremiumAccount(email.getText(), password.getText()));
            }
        };
    }
}
