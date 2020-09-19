package minegame159.meteorclient.accounts.gui;

import minegame159.meteorclient.accounts.AccountManager;
import minegame159.meteorclient.accounts.types.TheAlteningAccount;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTextBox;
import minegame159.meteorclient.utils.MeteorExecutor;

public class AddTheAlteningAccountScreen extends WindowScreen {
    public AddTheAlteningAccountScreen() {
        super("Add The Altening Account", true);

        add(new WLabel("Token:"));
        WTextBox token = add(new WTextBox("", 200)).getWidget();
        token.setFocused(true);
        row();

        WButton add = add(new WButton("Add")).fillX().expandX().getWidget();
        add.action = button -> {
            if (!token.text.isEmpty()) {
                add.setText("...");
                locked = true;

                MeteorExecutor.execute(() -> {
                    TheAlteningAccount account = new TheAlteningAccount(token.text);

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
