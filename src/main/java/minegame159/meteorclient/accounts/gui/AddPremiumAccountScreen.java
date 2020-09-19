package minegame159.meteorclient.accounts.gui;

import minegame159.meteorclient.accounts.AccountManager;
import minegame159.meteorclient.accounts.types.PremiumAccount;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTextBox;
import minegame159.meteorclient.utils.MeteorExecutor;

public class AddPremiumAccountScreen extends WindowScreen {
    public AddPremiumAccountScreen() {
        super("Add Premium Account", true);

        add(new WLabel("Email:"));
        WTextBox email = add(new WTextBox("", 200)).getWidget();
        email.setFocused(true);
        row();

        add(new WLabel("Password:"));
        WTextBox password = add(new WTextBox("", 200)).getWidget();
        row();

        WButton add = add(new WButton("Add")).fillX().expandX().getWidget();
        add.action = button -> {
            if (!email.text.isEmpty() && !password.text.isEmpty() && email.text.contains("@")) {
                add.setText("...");
                locked = true;

                MeteorExecutor.execute(() -> {
                    PremiumAccount account = new PremiumAccount(email.text, password.text);

                    if (account.fetchInfo() && account.fetchHead()) {
                        AccountManager.INSTANCE.add(account);
                        locked = false;
                        onClose();
                    }

                    add.setText("Add");
                    locked = false;
                });
            }
        };
    }
}
