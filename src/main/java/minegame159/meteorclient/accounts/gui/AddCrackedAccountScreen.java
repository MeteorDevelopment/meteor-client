/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.accounts.gui;

import minegame159.meteorclient.accounts.types.CrackedAccount;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTextBox;

public class AddCrackedAccountScreen extends WindowScreen {
    public AddCrackedAccountScreen() {
        super("Add Cracked Account", true);

        // Name
        add(new WLabel("Name:"));
        WTextBox name = add(new WTextBox("", 400)).getWidget();
        name.setFocused(true);
        row();

        // Add
        WButton add = add(new WButton("Add")).fillX().expandX().getWidget();
        add.action = () -> {
            if (!name.getText().isEmpty()) {
                AccountsScreen.addAccount(add, this, new CrackedAccount(name.getText()));
            }
        };
    }
}
