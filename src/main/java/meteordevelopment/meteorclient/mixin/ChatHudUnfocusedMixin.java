/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.ActiveTextCollector.Parameters;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess", remap = false)
public class ChatHudUnfocusedMixin {
    @Unique
    private static BetterChat betterChat;

    @Final
    @Shadow
    private GuiGraphicsExtractor graphics;

    // Offset text to make room for player heads
    @ModifyArg(method = "text", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector;text(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V"), index = 1)
    private int modifyX(int x) {
        return getBetterChat().modifyChatWidth(x);
    }

    // Player Heads for unfocused chat - draw before text
    @ModifyReceiver(method = "text", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector;text(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V"))
    private ActiveTextCollector onRender_beforeDrawText(ActiveTextCollector instance, TextAlignment alignment, int x, int y, Parameters transformation, FormattedCharSequence orderedText) {
        getBetterChat().beforeDrawMessage(graphics, y, ARGB.white(transformation.opacity()));
        return instance;
    }

    // Clean up after drawing
    @Inject(method = "text", at = @At("TAIL"))
    private void onRender_afterDrawText(int y, float f, FormattedCharSequence orderedText, CallbackInfoReturnable<Boolean> cir) {
        getBetterChat().afterDrawMessage();
    }

    @Unique
    private static BetterChat getBetterChat() {
        if (betterChat == null) {
            betterChat = Modules.get().get(BetterChat.class);
        }
        return betterChat;
    }
}
