package minegame159.meteorclient.gui.screens.topbar;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.accountsfriends.Friend;
import minegame159.meteorclient.accountsfriends.FriendManager;
import minegame159.meteorclient.events.FriendListChangedEvent;
import minegame159.meteorclient.gui.GuiRenderer;
import minegame159.meteorclient.gui.TopBarType;
import minegame159.meteorclient.gui.screens.EditFriendScreen;
import minegame159.meteorclient.gui.widgets.*;

public class TopBarFriends extends TopBarScreen implements Listenable {
    private WWindow window;

    public TopBarFriends() {
        super(TopBarType.Friends);

        window = add(new WWindow(title, true)).centerXY().getWidget();

        initWidgets();
    }

    @Override
    public void clear() {
        window.clear();
    }

    @Override
    protected void init() {
        super.init();
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    private void initWidgets() {
        // Friends
        for (Friend friend : FriendManager.INSTANCE.getAll()) {
            window.add(new WLabel(friend.name));

            window.add(new WButton(GuiRenderer.TEX_EDIT)).getWidget().action = button -> mc.openScreen(new EditFriendScreen(friend));

            WMinus remove = window.add(new WMinus()).getWidget();
            remove.action = minus -> FriendManager.INSTANCE.remove(friend);

            window.row();
        }

        // Add
        WTable addList = window.add(new WTable()).fillX().expandX().getWidget();
        WTextBox username = addList.add(new WTextBox("", 200)).fillX().expandX().getWidget();
        username.setFocused(true);

        WPlus add = addList.add(new WPlus()).getWidget();
        add.action = plus -> FriendManager.INSTANCE.add(new Friend(username.text.trim()));
    }

    @EventHandler
    private Listener<FriendListChangedEvent> onFriendListChanged = new Listener<>(event -> {
        clear();
        initWidgets();
    });

    @Override
    public void onClose() {
        MeteorClient.EVENT_BUS.unsubscribe(this);
        FriendManager.INSTANCE.save();
        super.onClose();
    }
}
