/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.events.entity.player;

import minegame159.meteorclient.events.Cancellable;

public class SendMessageEvent extends Cancellable {
    private static final SendMessageEvent INSTANCE = new SendMessageEvent();

    public String msg;

    public static SendMessageEvent get(String msg) {
        INSTANCE.setCancelled(false);
        INSTANCE.msg = msg;
        return INSTANCE;
    }
}


