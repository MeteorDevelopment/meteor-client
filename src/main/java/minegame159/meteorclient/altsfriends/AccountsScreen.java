package minegame159.meteorclient.altsfriends;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.AccountListChangedEvent;
import minegame159.meteorclient.gui.Alignment;
import minegame159.meteorclient.gui.PanelListScreen;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.modules.setting.GUI;

public class AccountsScreen extends PanelListScreen implements Listenable {
    private WLabel loggedInL;

    public AccountsScreen() {
        super("Accounts");
        parent = mc.currentScreen;

        initWidgets();

        MeteorClient.eventBus.subscribe(this);
    }

    private void initWidgets() {
        // Accounts
        if (AccountManager.INSTANCE.getAll().size() > 0) {
            WGrid grid = add(new WGrid(4, 4, 4));
            for (Account account : AccountManager.INSTANCE.getAll()) {
                WLabel name = new WLabel(account.username);

                WLabel loggedIn = new WLabel("- logged in");
                loggedIn.color = GUI.textLoggedIn;
                if (mc.getSession().getUsername().equalsIgnoreCase(account.username)) loggedInL = loggedIn;
                else loggedIn.visible = false;

                WButton logIn = new WButton("Log In");
                logIn.action = () -> {
                    if (account.logIn()) {
                        if (loggedInL != null) loggedInL.visible = false;
                        loggedIn.visible = true;
                        loggedInL = loggedIn;
                    }
                };

                WMinus remove = new WMinus();
                remove.action = () -> AccountManager.INSTANCE.remove(account);

                grid.addRow(name, loggedIn, logIn, remove);
            }
            add(new WHorizontalSeparator());
        }

        // Add
        WPlus add = add(new WPlus());
        add.boundingBox.alignment.x = Alignment.X.Right;
        add.action = () -> mc.openScreen(new AddAccountScreen());

        layout();
    }

    @EventHandler
    private Listener<AccountListChangedEvent> onAccountListChanged = new Listener<>(event -> {
        clear();
        initWidgets();
    });

    @Override
    public void onClose() {
        MeteorClient.eventBus.unsubscribe(this);
        super.onClose();
    }
}
