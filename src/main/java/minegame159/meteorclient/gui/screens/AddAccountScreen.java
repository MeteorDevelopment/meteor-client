package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.accountsfriends.Account;
import minegame159.meteorclient.accountsfriends.AccountManager;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTextBox;

public class AddAccountScreen extends WindowScreen {
    public AddAccountScreen() {
        super("Add Account", true);

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
                AccountManager.INSTANCE.add(new Account(email.text, password.text));
                onClose();
            }
        };
    }
}
