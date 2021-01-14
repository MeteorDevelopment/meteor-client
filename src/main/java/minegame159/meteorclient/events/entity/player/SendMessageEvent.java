/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.events.entity.player;

public class SendMessageEvent {
    private static final SendMessageEvent INSTANCE = new SendMessageEvent();

    public String msg;

    public static SendMessageEvent get(String msg) {
        INSTANCE.msg = msg;
        return INSTANCE;
    }
}


