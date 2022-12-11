/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.meteordev.meteor.impl.mixin.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.meteordev.meteor.impl.commands.CommandManagerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreeMixin {
    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void meteor_sendMessage(String message, boolean addToHistory, CallbackInfoReturnable<Boolean> info) {
        // TODO: Commands: Get the prefix from a config system
        String prefix = ".";

        if (message.startsWith(prefix)) {
            try {
                CommandManagerImpl.INSTANCE.dispatch(message.substring(prefix.length()));
            } catch (CommandSyntaxException e) {
                // TODO: Print the error to chat
            }

            MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(message);
            info.setReturnValue(true);
        }
    }
}
