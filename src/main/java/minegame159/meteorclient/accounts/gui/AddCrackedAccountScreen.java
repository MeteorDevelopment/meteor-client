package minegame159.meteorclient.accounts.gui;

import minegame159.meteorclient.accounts.AccountManager;
import minegame159.meteorclient.accounts.types.CrackedAccount;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTextBox;
import minegame159.meteorclient.utils.MeteorExecutor;

public class AddCrackedAccountScreen extends WindowScreen {
    public AddCrackedAccountScreen() {
        super("Add Cracked Account", true);

        add(new WLabel("Name:"));
        WTextBox name = add(new WTextBox("", 200)).getWidget();
        name.setFocused(true);
        row();

        WButton add = add(new WButton("Add")).fillX().expandX().getWidget();
        add.action = button -> {
            if (!name.text.isEmpty()) {
                add.setText("...");
                locked = true;

                MeteorExecutor.execute(() -> {
                    CrackedAccount account = new CrackedAccount(name.text);

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
