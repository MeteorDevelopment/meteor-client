/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui;

public class GuiKeyEvents {
    public static int postKeyEvents = 0;

    public static void setPostKeyEvents(boolean post) {
        postKeyEvents += post ? 1 : -1;
    }

    public static boolean canUseKeys() {
        return postKeyEvents <= 0;
    }

    public static void resetPostKeyEvents() {
        postKeyEvents = 0;
    }
}
