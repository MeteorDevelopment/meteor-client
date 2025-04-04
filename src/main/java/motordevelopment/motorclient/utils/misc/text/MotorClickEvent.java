/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.utils.misc.text;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * This class does nothing except ensure that {@link ClickEvent}'s containing motor Client commands can only be executed if they come from the client.
 * @see motordevelopment.motorclient.mixin.ScreenMixin#onRunCommand(Style, CallbackInfoReturnable)
 */
public class MotorClickEvent extends ClickEvent {
    public MotorClickEvent(Action action, String value) {
        super(action, value);
    }
}
