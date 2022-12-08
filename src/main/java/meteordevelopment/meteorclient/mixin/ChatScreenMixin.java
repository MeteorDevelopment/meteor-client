/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Shadow protected TextFieldWidget chatField;
    @Shadow abstract public boolean sendMessage(String chatText, boolean addToHistory);
    
    private boolean ignoreChatMessage;

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setMaxLength(I)V", shift = At.Shift.AFTER))
    private void onInit(CallbackInfo info) {
        if (Modules.get().get(BetterChat.class).isInfiniteChatBox()) chatField.setMaxLength(Integer.MAX_VALUE);
    }


    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String chatText, boolean addToHistory, CallbackInfoReturnable<Boolean> info) {
        if (ignoreChatMessage) return;

        if (!chatText.startsWith(Config.get().prefix.get()) && !chatText.startsWith("/") && !chatText.startsWith(BaritoneAPI.getSettings().prefix.value)) {
            SendMessageEvent event = MeteorClient.EVENT_BUS.post(SendMessageEvent.get(chatText));

            if (!event.isCancelled()) {
                ignoreChatMessage = true;
                sendMessage(event.message, addToHistory);
                ignoreChatMessage = false;
            }

            info.cancel();
            return;
        }

        if (chatText.startsWith(Config.get().prefix.get())) {
            try {
                Commands.get().dispatch(chatText.substring(Config.get().prefix.get().length()));
            } catch (CommandSyntaxException e) {
                ChatUtils.error(e.getMessage());
            }

            info.cancel();
        }
    }
}
