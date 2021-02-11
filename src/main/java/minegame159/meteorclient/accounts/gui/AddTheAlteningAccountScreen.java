/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.accounts.gui;

import minegame159.meteorclient.accounts.types.TheAlteningAccount;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WLabel;

public class AddTheAlteningAccountScreen extends WindowScreen {
    public AddTheAlteningAccountScreen() {
        super("Add The Altening Account", true);

        // Token
        add(new WLabel("Token:"));
        WAccountField token = add(new WAccountField("", 400)).getWidget();
        token.setFocused(true);
        row();

        // Add
        WButton add = add(new WButton("Add")).fillX().expandX().getWidget();
        add.action = () -> {
            if (!token.getText().isEmpty()) {
                AccountsScreen.addAccount(add, this, new TheAlteningAccount(token.getText()));
            }
        };
    }
}