/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.option.KeyBinding;

import java.util.function.BooleanSupplier;

public class KeybindingPresser {
    private final KeyBinding keyBinding;
    private final BooleanSupplier pressCondition;
    private boolean pressed;

    public KeybindingPresser(KeyBinding keyBinding, BooleanSupplier pressCondition) {
        this.keyBinding = keyBinding;
        this.pressCondition = pressCondition;
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public KeybindingPresser(KeyBinding keyBinding) {
        this.keyBinding = keyBinding;
        this.pressCondition = null;
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public void press() {
        keyBinding.setPressed(true);
        pressed = true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Pre event) {
        if (pressed) {
            boolean pressed = true;

            if (pressCondition != null) {
                pressed = pressCondition.getAsBoolean();
            }

            keyBinding.setPressed(pressed);
        }
    }

    public void stopIfPressed() {
        if (pressed) {
            keyBinding.setPressed(false);
            pressed = false;
        }
    }

    public boolean isPressed() {
        return pressed;
    }
}
