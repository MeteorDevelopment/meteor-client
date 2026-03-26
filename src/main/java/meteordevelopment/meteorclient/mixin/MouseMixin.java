/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import com.mojang.blaze3d.platform.Window;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {
    @Shadow
    public abstract double getScaledXPos(Window window);

    @Shadow
    public abstract double getScaledYPos(Window window);

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseButtonInfo mouseInput, int action, CallbackInfo ci) {
        Input.setButtonState(mouseInput.button(), action != GLFW_RELEASE);

        MouseButtonEvent click = new MouseButtonEvent(getScaledXPos(minecraft.getWindow()), getScaledYPos(minecraft.getWindow()), mouseInput);
        if (MeteorClient.EVENT_BUS.post(MouseClickEvent.get(click, KeyAction.get(action))).isCancelled()) ci.cancel();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo info) {
        if (MeteorClient.EVENT_BUS.post(MouseScrollEvent.get(vertical)).isCancelled()) info.cancel();
    }
}
