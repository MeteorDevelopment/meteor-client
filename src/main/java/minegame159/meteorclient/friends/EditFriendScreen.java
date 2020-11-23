/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.friends;

import minegame159.meteorclient.gui.screens.WindowScreen;

public class EditFriendScreen extends WindowScreen {
    public EditFriendScreen(Friend friend) {
        super(friend.name, true);
    }
}
