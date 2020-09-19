package minegame159.meteorclient.accounts.gui;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.accounts.Account;
import minegame159.meteorclient.accounts.AccountManager;
import minegame159.meteorclient.events.AccountListChangedEvent;
import minegame159.meteorclient.gui.listeners.ButtonClickListener;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WTable;
import net.minecraft.client.MinecraftClient;

public class AccountsScreen extends WindowScreen implements Listenable {
    public AccountsScreen() {
        super("Accounts", true);

        initWidgets();
    }

    @Override
    protected void init() {
        super.init();
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    void initWidgets() {
        if (AccountManager.INSTANCE.size() > 0) {
            WTable table = add(new WTable()).fillX().expandX().getWidget();
            row();

            for (Account<?> account : AccountManager.INSTANCE) {
                table.add(new WAccount(this, account)).fillX().expandX();
                table.row();
            }
        }

        // Add Account
        WTable table = add(new WTable()).fillX().expandX().getWidget();
        addButton(table, "Cracked", button -> MinecraftClient.getInstance().openScreen(new AddCrackedAccountScreen()));
        addButton(table, "Premium", button -> MinecraftClient.getInstance().openScreen(new AddPremiumAccountScreen()));
        addButton(table, "The Altening", button -> MinecraftClient.getInstance().openScreen(new AddTheAlteningAccountScreen()));
    }

    private void addButton(WTable table, String text, ButtonClickListener action) {
        WButton button = table.add(new WButton(text)).fillX().expandX().getWidget();
        button.action = action;
    }

    @EventHandler
    private final Listener<AccountListChangedEvent> onAccountListChanged = new Listener<>(event -> {
        clear();
        initWidgets();
    });

    @Override
    public void onClose() {
        MeteorClient.EVENT_BUS.unsubscribe(this);
        super.onClose();
    }
}
