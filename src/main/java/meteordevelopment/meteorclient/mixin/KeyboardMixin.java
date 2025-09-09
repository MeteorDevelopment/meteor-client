/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.CharTypedEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.gui.GuiKeyEvents;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    public void onKey(long window, int action, KeyInput arg, CallbackInfo ci) { // todo verify this is correct when they update the mappings
        int modifiers = arg.modifiers();
        if (arg.key() != GLFW.GLFW_KEY_UNKNOWN) {
            // on Linux/X11 the modifier is not active when the key is pressed and still active when the key is released
            // https://github.com/glfw/glfw/issues/1630
            if (action == GLFW.GLFW_PRESS) {
                modifiers |= Input.getModifier(arg.key());
            } else if (action == GLFW.GLFW_RELEASE) {
                modifiers &= ~Input.getModifier(arg.key());
            }

            if (client.currentScreen instanceof WidgetScreen && action == GLFW.GLFW_REPEAT) {
                ((WidgetScreen) client.currentScreen).keyRepeated(arg.key(), modifiers);
            }

            if (GuiKeyEvents.canUseKeys) {
                Input.setKeyState(arg.key(), action != GLFW.GLFW_RELEASE);
                if (MeteorClient.EVENT_BUS.post(KeyEvent.get(arg, arg.key(), modifiers, KeyAction.get(action))).isCancelled()) ci.cancel();
            }
        }
    }

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void onChar(long window, CharInput arg, CallbackInfo ci) {
        if (Utils.canUpdate() && !client.isPaused() && (client.currentScreen == null || client.currentScreen instanceof WidgetScreen)) {
            if (MeteorClient.EVENT_BUS.post(CharTypedEvent.get((char) arg.codepoint())).isCancelled()) ci.cancel();
        }
    }
}
