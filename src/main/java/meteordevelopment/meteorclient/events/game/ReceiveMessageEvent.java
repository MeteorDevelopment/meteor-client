/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.game;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;

public class ReceiveMessageEvent extends Cancellable {
    private static final ReceiveMessageEvent INSTANCE = new ReceiveMessageEvent();

    private Component message;
    private GuiMessageTag indicator;
    private boolean modified;
    public int id;

    public static ReceiveMessageEvent get(Component message, GuiMessageTag indicator, int id) {
        INSTANCE.setCancelled(false);
        INSTANCE.message = message;
        INSTANCE.indicator = indicator;
        INSTANCE.modified = false;
        INSTANCE.id = id;
        return INSTANCE;
    }

    public Component getMessage() {
        return message;
    }

    public GuiMessageTag getIndicator() {
        return indicator;
    }

    public void setMessage(Component message) {
        this.message = message;
        this.modified = true;
    }

    public void setIndicator(GuiMessageTag indicator) {
        this.indicator = indicator;
        this.modified = true;
    }

    public boolean isModified() {
        return modified;
    }
}
