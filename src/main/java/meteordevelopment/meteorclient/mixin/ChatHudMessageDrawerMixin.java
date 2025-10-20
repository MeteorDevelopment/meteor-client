/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import net.minecraft.client.font.Alignment;
import net.minecraft.client.font.TextConsumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.gui.hud.ChatHud$class_12235", remap = false)
public class ChatHudMessageDrawerMixin { // todo rename this when it gets mapped
    @Unique
    private static BetterChat betterChat;

    @Shadow
    @Final
    private DrawContext context;

    @ModifyReceiver(method = "method_75807", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextConsumer;text(Lnet/minecraft/client/font/Alignment;IILnet/minecraft/client/font/TextConsumer$Transformation;Lnet/minecraft/text/OrderedText;)V"))
    private TextConsumer onRender_beforeDrawTextWithShadow(TextConsumer instance, Alignment alignment, int x, int y, TextConsumer.Transformation transformation, OrderedText orderedText) {
        getBetterChat().beforeDrawMessage(context, y, ColorHelper.getWhite(transformation.opacity()));
        return instance;
    }

    @ModifyArg(method = "method_75807", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextConsumer;text(Lnet/minecraft/client/font/Alignment;IILnet/minecraft/client/font/TextConsumer$Transformation;Lnet/minecraft/text/OrderedText;)V"), index = 1)
    private int modifyX(int x) {
        return getBetterChat().modifyChatWidth(x);
    }

    @Inject(method = "method_75807", at = @At("TAIL"))
    private void onRender_afterDrawTextWithShadow(int y, float f, OrderedText text, CallbackInfoReturnable<Boolean> cir) {
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
