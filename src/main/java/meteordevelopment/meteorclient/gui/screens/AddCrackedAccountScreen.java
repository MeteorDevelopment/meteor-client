/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.accounts.Accounts;
import meteordevelopment.meteorclient.systems.accounts.types.CrackedAccount;

public class AddCrackedAccountScreen extends WindowScreen {
    public AddCrackedAccountScreen(GuiTheme theme) {
        super(theme, "Add Cracked Account");

        WTable t = add(theme.table()).widget();

        // Name
        t.add(theme.label("Name: "));
        WTextBox name = t.add(theme.textBox("")).minWidth(400).expandX().widget();
        name.setFocused(true);
        t.row();

        // Add
        WButton add = t.add(theme.button("Add")).expandX().widget();
        add.action = () -> {
            CrackedAccount account = new CrackedAccount(name.get());
            if (!name.get().trim().isEmpty() && !(Accounts.get().exists(account))) {
                AccountsScreen.addAccount(add, this, account);
            }
        };

        enterAction = add.action;
    }
}
