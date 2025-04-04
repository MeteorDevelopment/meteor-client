/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.themes.motor.widgets;

import motordevelopment.motorclient.gui.WidgetScreen;
import motordevelopment.motorclient.gui.themes.motor.MotorWidget;
import motordevelopment.motorclient.gui.widgets.WAccount;
import motordevelopment.motorclient.systems.accounts.Account;
import motordevelopment.motorclient.utils.render.color.Color;

public class WMotorAccount extends WAccount implements MotorWidget {
    public WMotorAccount(WidgetScreen screen, Account<?> account) {
        super(screen, account);
    }

    @Override
    protected Color loggedInColor() {
        return theme().loggedInColor.get();
    }

    @Override
    protected Color accountTypeColor() {
        return theme().textSecondaryColor.get();
    }
}
