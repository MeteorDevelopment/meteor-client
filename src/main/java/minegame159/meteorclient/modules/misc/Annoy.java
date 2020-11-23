/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.SendMessageEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class Annoy extends ToggleModule {
    public Annoy() {
        super(Category.Misc, "annoy", "Makes your messages wEiRd.");
    }

    @EventHandler
    private final Listener<SendMessageEvent> onSendMessage = new Listener<>(event -> {
        StringBuilder sb = new StringBuilder(event.msg.length());

        boolean upperCase = true;
        for (int cp : event.msg.codePoints().toArray()) {
            if (upperCase) sb.appendCodePoint(Character.toUpperCase(cp));
            else sb.appendCodePoint(Character.toLowerCase(cp));

            upperCase = !upperCase;
        }

        event.msg = sb.toString();
    });
}
