package minegame159.meteorclient.gui.screens;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.accountsfriends.Account;
import minegame159.meteorclient.accountsfriends.AccountManager;
import minegame159.meteorclient.events.AccountListChangedEvent;
import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.widgets.*;

public class AccountsScreen extends WindowScreen implements Listenable {
    private WLabel loggedInL;

    public AccountsScreen() {
        super("Accounts", true);

        initWidgets();
    }

    @Override
    protected void init() {
        super.init();
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    private void initWidgets() {
        // Accounts
        if (AccountManager.INSTANCE.getAll().size() > 0) {
            WTable table = add(new WTable()).getWidget();
            row();

            for (Account account : AccountManager.INSTANCE.getAll()) {
                WLabel name = table.add(new WLabel(account.getName())).getWidget();

                WLabel loggedIn = table.add(new WLabel("- logged in")).getWidget();
                loggedIn.color = GuiConfig.INSTANCE.loggedInText;
                if (mc.getSession().getUsername().equalsIgnoreCase(account.getName())) loggedInL = loggedIn;
                else loggedIn.visible = false;

                WButton logIn = table.add(new WButton("Log In")).getWidget();
                logIn.action = button -> {
                    if (account.logIn()) {
                        if (loggedInL != null) loggedInL.visible = false;
                        loggedIn.visible = true;
                        loggedInL = loggedIn;
                        name.setText(account.getName());
                    }
                };

                WMinus remove = table.add(new WMinus()).getWidget();
                remove.action = minus -> AccountManager.INSTANCE.remove(account);

                table.row();
            }

            add(new WHorizontalSeparator()).fillX().expandX();
            row();
        }

        // Add
        WPlus add = add(new WPlus()).fillX().right().getWidget();
        add.action = plus -> mc.openScreen(new AddAccountScreen());
    }

    @EventHandler
    private Listener<AccountListChangedEvent> onAccountListChanged = new Listener<>(event -> {
        clear();
        initWidgets();
    });

    @Override
    public void onClose() {
        MeteorClient.EVENT_BUS.unsubscribe(this);
        super.onClose();
    }
}
