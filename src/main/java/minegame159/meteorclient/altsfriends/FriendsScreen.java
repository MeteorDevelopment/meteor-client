package minegame159.meteorclient.altsfriends;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.FriendListChangedEvent;
import minegame159.meteorclient.gui.Alignment;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.gui.clickgui.WHorizontalSeparatorBigger;
import minegame159.meteorclient.gui.widgets.*;
import net.minecraft.client.MinecraftClient;

public class FriendsScreen extends WidgetScreen implements Listenable {
    private WVerticalList list;

    public FriendsScreen() {
        super("Friends");
        parent = mc.currentScreen instanceof WidgetScreen ? mc.currentScreen : null;

        initWidgets();

        MeteorClient.eventBus.subscribe(this);
    }

    private void initWidgets() {
        WPanel panel = add(new WPanel());
        panel.boundingBox.setMargin(6);
        panel.boundingBox.alignment.set(Alignment.X.Center, Alignment.Y.Center);

        list = panel.add(new WVerticalList(4));
        list.maxHeight = MinecraftClient.getInstance().window.getScaledHeight() - 32;

        // Title
        list.add(new WLabel("Friends", true)).boundingBox.alignment.x = Alignment.X.Center;
        list.add(new WHorizontalSeparatorBigger());

        // Friends
        WGrid grid = list.add(new WGrid(4, 4, 2));
        for (String friend : FriendManager.INSTANCE.getAll()) {
            WMinus remove = new WMinus();
            remove.action = () -> FriendManager.INSTANCE.remove(friend);
            grid.addRow(
                    new WLabel(friend),
                    remove
            );
        }
        list.add(new WHorizontalSeparator());

        // Add
        WHorizontalList addList = list.add(new WHorizontalList(4));
        WTextBox username = addList.add(new WTextBox("", 16));
        username.focused = true;
        addList.add(new WPlus()).action = () -> FriendManager.INSTANCE.add(username.text);

        layout();
    }

    @EventHandler
    private Listener<FriendListChangedEvent> onFriendListChanged = new Listener<>(event -> {
        clear();
        initWidgets();
    });

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        list.maxHeight = height - 32;
        super.resize(client, width, height);
    }

    @Override
    public void onClose() {
        MeteorClient.eventBus.unsubscribe(this);
        super.onClose();
    }
}
