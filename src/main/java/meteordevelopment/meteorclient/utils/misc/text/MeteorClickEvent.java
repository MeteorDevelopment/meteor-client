/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.text;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * This class does nothing except ensure that {@link ClickEvent}'s containing Meteor Client commands can only be executed if they come from the client.
 * @see meteordevelopment.meteorclient.mixin.ScreenMixin#onRunCommand(Style, CallbackInfoReturnable)
 */
public class MeteorClickEvent implements ClickEvent {
    public final String value;

    public MeteorClickEvent(String value) {
        this.value = value;
    }

    @Override
    public Action getAction() {
        return Action.RUN_COMMAND;
    }
}
