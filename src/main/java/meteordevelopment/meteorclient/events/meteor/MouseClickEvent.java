/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.meteor;

import meteordevelopment.meteorclient.events.Cancellable;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.MouseInput;

public class MouseClickEvent extends Cancellable {
    private static final MouseClickEvent INSTANCE = new MouseClickEvent();

    public MouseInput input;
    public Click click;
    public KeyAction action;

    public static MouseClickEvent get(MouseInput mouseInput, Click click, KeyAction action) {
        INSTANCE.setCancelled(false);
        INSTANCE.input = mouseInput;
        INSTANCE.click = click;
        INSTANCE.action = action;
        return INSTANCE;
    }
}
