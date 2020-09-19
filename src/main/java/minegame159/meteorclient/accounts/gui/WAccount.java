package minegame159.meteorclient.accounts.gui;

import minegame159.meteorclient.accounts.Account;
import minegame159.meteorclient.accounts.AccountManager;
import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.utils.MeteorExecutor;
import net.minecraft.client.MinecraftClient;

public class WAccount extends WTable {
    public WAccount(AccountsScreen screen, Account<?> account) {
        // Head
        add(new WTexture(16, 16, 90, account.getCache().getHeadTexture()));

        // Name
        WLabel name = add(new WLabel(account.getUsername())).getWidget();
        if (MinecraftClient.getInstance().getSession().getUsername().equalsIgnoreCase(account.getUsername())) name.color = GuiConfig.INSTANCE.loggedInText;

        // Account Type
        WLabel type = add(new WLabel("(" + account.getType().toString() + ")")).fillX().right().getWidget();
        type.color = GuiConfig.INSTANCE.accountTypeText;

        // Log In
        WButton logIn = add(new WButton("Log In")).getWidget();
        logIn.action = button -> {
            logIn.setText("...");
            screen.locked = true;

            MeteorExecutor.execute(() -> {
                if (account.login()) {
                    name.setText(account.getUsername());

                    AccountManager.INSTANCE.save();

                    screen.clear();
                    screen.initWidgets();
                }

                logIn.setText("Log In");
                screen.locked = false;
            });
        };

        // Remove
        WMinus minus = add(new WMinus()).getWidget();
        minus.action = minus1 -> AccountManager.INSTANCE.remove(account);
    }
}
