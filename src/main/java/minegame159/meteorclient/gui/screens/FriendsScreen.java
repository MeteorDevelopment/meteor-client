package minegame159.meteorclient.gui.screens;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.accountsfriends.FriendManager;
import minegame159.meteorclient.events.FriendListChangedEvent;
import minegame159.meteorclient.gui.widgets.*;

public class FriendsScreen extends WindowScreen implements Listenable {
    public FriendsScreen() {
        super("Friends", true);

        initWidgets();
    }

    @Override
    protected void init() {
        super.init();
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    private void initWidgets() {
        // Friends
        for (String friend : FriendManager.INSTANCE.getAll()) {
            add(new WLabel(friend));

            WMinus remove = add(new WMinus()).getWidget();
            remove.action = minus -> FriendManager.INSTANCE.remove(friend);

            row();
        }

        // Add
        WTable addList = add(new WTable()).fillX().expandX().getWidget();
        WTextBox username = addList.add(new WTextBox("", 200)).fillX().expandX().getWidget();
        username.setFocused(true);

        WPlus add = addList.add(new WPlus()).getWidget();
        add.action = plus -> FriendManager.INSTANCE.add(username.text);
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
