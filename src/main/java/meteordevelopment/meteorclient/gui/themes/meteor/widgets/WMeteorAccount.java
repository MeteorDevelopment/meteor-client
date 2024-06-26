/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WAccount;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WMeteorAccount extends WAccount implements MeteorWidget {
    public WMeteorAccount(WidgetScreen screen, Account<?> account) {
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
