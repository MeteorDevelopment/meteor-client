/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.events.game;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.text.Text;

public class ReceiveMessageEvent extends Cancellable {
    private static final ReceiveMessageEvent INSTANCE = new ReceiveMessageEvent();

    public Text message;
    public int id;

    public static ReceiveMessageEvent get(Text message, int id) {
        INSTANCE.setCancelled(false);
        INSTANCE.message = message;
        INSTANCE.id = id;
        return INSTANCE;
    }
}
