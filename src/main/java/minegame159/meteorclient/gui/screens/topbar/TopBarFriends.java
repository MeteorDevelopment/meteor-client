/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.gui.screens.topbar;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.FriendListChangedEvent;
import minegame159.meteorclient.friends.EditFriendScreen;
import minegame159.meteorclient.friends.Friend;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.gui.widgets.*;
import net.minecraft.client.MinecraftClient;

public class TopBarFriends extends TopBarWindowScreen {
    public TopBarFriends() {
        super(TopBarType.Friends);
    }

    @Override
    protected void initWidgets() {
        // Friends
        for (Friend friend : FriendManager.INSTANCE) {
            add(new WLabel(friend.name));
            add(new WButton(WButton.ButtonRegion.Edit)).getWidget().action = () -> MinecraftClient.getInstance().openScreen(new EditFriendScreen(friend));

            WMinus remove = add(new WMinus()).getWidget();
            remove.action = () -> FriendManager.INSTANCE.remove(friend);

            row();
        }

        // Add friend
        WTable t = add(new WTable()).fillX().expandX().getWidget();
        WTextBox username = t.add(new WTextBox("", 400)).fillX().expandX().getWidget();
        username.setFocused(true);

        WPlus add = t.add(new WPlus()).getWidget();
        add.action = () -> {
            String name = username.getText().trim();
            if (!name.isEmpty()) FriendManager.INSTANCE.add(new Friend(name));
        };
    }

    @EventHandler
    private final Listener<FriendListChangedEvent> onFriendListChanged = new Listener<>(event -> {
        clear();
        initWidgets();
    });
}
