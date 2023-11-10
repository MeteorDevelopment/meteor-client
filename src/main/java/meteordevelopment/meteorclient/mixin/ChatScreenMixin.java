/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChatScreen.class, priority = 1337)
public abstract class ChatScreenMixin {

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setMaxLength(I)V"))
    private int removeChatLength(int maxLength) { /* This is better to use as we aren't injecting again, just modifying an argument. */
        return Modules.get().get(BetterChat.class).isInfiniteChatBox() ? Integer.MAX_VALUE : 256;
    }

    @Inject(method = "normalize", at = @At("RETURN"), cancellable = true)
    private void removeChatBox(String chatText, CallbackInfoReturnable<String> cir) {
        /* This will bypass the check to truncate the chat string to 256 chars. */
        cir.setReturnValue(Modules.get().get(BetterChat.class).isInfiniteChatBox() ? chatText : cir.getReturnValue());
    }
}
