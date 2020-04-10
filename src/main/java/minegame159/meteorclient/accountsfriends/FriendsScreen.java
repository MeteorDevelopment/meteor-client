package minegame159.meteorclient.accountsfriends;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.FriendListChangedEvent;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.*;

public class FriendsScreen extends WindowScreen implements Listenable {
    public FriendsScreen() {
        super("Friends");

        initWidgets();
    }

    @Override
    protected void init() {
        super.init();
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    private void initWidgets() {
        // Friends
        WGrid grid = add(new WGrid(4, 4, 2));
        for (String friend : FriendManager.INSTANCE.getAll()) {
            WMinus remove = new WMinus();
            remove.action = () -> FriendManager.INSTANCE.remove(friend);
            grid.addRow(
                    new WLabel(friend),
                    remove
            );
        }
        add(new WHorizontalSeparator());

        // Add
        WTextBox username = new WTextBox("", 200);
        username.setFocused(true);
        WPlus add = new WPlus();
        add.action = () -> FriendManager.INSTANCE.add(username.text);
        grid.addRow(username, add);

        layout();
    }

    @EventHandler
    private Listener<FriendListChangedEvent> onFriendListChanged = new Listener<>(event -> {
        clear();
        initWidgets();
    });

    @Override
    public void onClose() {
        MeteorClient.EVENT_BUS.unsubscribe(this);
        super.onClose();
    }
}
