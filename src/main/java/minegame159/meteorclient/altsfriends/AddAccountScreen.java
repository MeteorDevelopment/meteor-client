package minegame159.meteorclient.altsfriends;

import minegame159.meteorclient.gui.PanelListScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WGrid;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTextBox;

public class AddAccountScreen extends PanelListScreen {
    public AddAccountScreen() {
        super("Add Account");

        WGrid grid = add(new WGrid(4, 4, 2));

        WLabel emailL = new WLabel("Email:");
        WTextBox emailT = new WTextBox("", 32);
        grid.addRow(emailL, emailT);

        WLabel passwordL = new WLabel("Password:");
        WTextBox passwordT = new WTextBox("", 32);
        grid.addRow(passwordL, passwordT);

        WButton add = add(new WButton("Add"));
        add.boundingBox.fullWidth = true;
        add.action = () -> {
            AccountManager.INSTANCE.add(new Account(emailT.text, passwordT.text));
            onClose();
        };

        layout();
    }
}
