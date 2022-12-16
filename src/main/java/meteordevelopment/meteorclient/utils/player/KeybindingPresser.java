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
import net.minecraft.item.BowItem;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class KeybindingPresser {
    private final KeyBinding keyBinding;
    private boolean pressed;

    public KeybindingPresser(KeyBinding keyBinding) {
        this.keyBinding = keyBinding;
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public void use() {
        keyBinding.setPressed(true);
        pressed = true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Pre event) {
        if (pressed) {
            boolean pressed = true;

            if (mc.player.getMainHandStack().getItem() instanceof BowItem) {
                pressed = BowItem.getPullProgress(mc.player.getItemUseTime()) < 1;
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

    public void stopAndSwapBack() {
        if (pressed) {
            keyBinding.setPressed(false);
            InvUtils.swapBack();
            pressed = false;
        }
    }

    public boolean isPressed() {
        return pressed;
    }
}
