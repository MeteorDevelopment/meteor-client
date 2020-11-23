/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import it.unimi.dsi.fastutil.chars.Char2CharArrayMap;
import it.unimi.dsi.fastutil.chars.Char2CharMap;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.SendMessageEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class FancyChat extends ToggleModule {
    private static final Char2CharMap SMALL_CAPS = new Char2CharArrayMap();
    
    static {
        String[] a = "abcdefghijklmnopqrstuvwxyz".split("");
        String[] b = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴩqʀꜱᴛᴜᴠᴡxyᴢ".split("");
        for (int i = 0; i < a.length; i++) SMALL_CAPS.put(a[i].charAt(0), b[i].charAt(0));
    }

    private final StringBuilder sb = new StringBuilder();

    public FancyChat() {
        super(Category.Misc, "FancyChat", "Make your chat messages fancy!");
    }
    @EventHandler
    private final Listener<SendMessageEvent> onSendMessage = new Listener<>(event -> {
            event.msg = changeMessage(event.msg);
    });

    private String changeMessage(String changeFrom) {
        String output = changeFrom;
        sb.setLength(0);

        for (char ch : output.toCharArray()) {
            if (SMALL_CAPS.containsKey(ch)) sb.append(SMALL_CAPS.get(ch));
            else sb.append(ch);
        }

        output = sb.toString();

        return output;
    }
}
