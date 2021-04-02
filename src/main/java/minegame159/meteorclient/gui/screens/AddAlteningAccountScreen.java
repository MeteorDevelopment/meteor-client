/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.accounts.types.TheAlteningAccount;
import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.WindowScreen;
import minegame159.meteorclient.gui.widgets.containers.WTable;
import minegame159.meteorclient.gui.widgets.input.WTextBox;
import minegame159.meteorclient.gui.widgets.pressable.WButton;

public class AddAlteningAccountScreen extends WindowScreen {
    public AddAlteningAccountScreen(GuiTheme theme) {
        super(theme, "Add The Altening Account");

        WTable t = add(theme.table()).widget();

        // Token
        t.add(theme.label("Token: "));
        WTextBox token = t.add(theme.textBox("")).minWidth(400).expandX().widget();
        token.setFocused(true);
        t.row();

        // Add
        WButton add = t.add(theme.button("Add")).expandX().widget();
        add.action = () -> {
            if (!token.get().isEmpty()) {
                AccountsScreen.addAccount(add, this, new TheAlteningAccount(token.get()));
            }
        };
    }
}
