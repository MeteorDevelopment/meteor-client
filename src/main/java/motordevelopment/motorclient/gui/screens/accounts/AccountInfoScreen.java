/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.screens.accounts;

import motordevelopment.motorclient.gui.GuiTheme;
import motordevelopment.motorclient.gui.WindowScreen;
import motordevelopment.motorclient.gui.widgets.containers.WHorizontalList;
import motordevelopment.motorclient.gui.widgets.pressable.WButton;
import motordevelopment.motorclient.systems.accounts.Account;
import motordevelopment.motorclient.systems.accounts.TokenAccount;
import motordevelopment.motorclient.utils.render.color.Color;

import static motordevelopment.motorclient.MotorClient.mc;

public class AccountInfoScreen extends WindowScreen {
    private final Account<?> account;

    public AccountInfoScreen(GuiTheme theme, Account<?> account) {
        super(theme, account.getUsername() + " details");
        this.account = account;
    }

    @Override
    public void initWidgets() {
        TokenAccount e = (TokenAccount) account;
        WHorizontalList l = add(theme.horizontalList()).expandX().widget();

        WButton copy = theme.button("Copy");
        copy.action = () -> mc.keyboard.setClipboard(e.getToken());

        l.add(theme.label("TheAltening token:"));
        l.add(theme.label(e.getToken()).color(Color.GRAY)).pad(5);
        l.add(copy);
    }
}
