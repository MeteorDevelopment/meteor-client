/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.events.game.ReceiveMessageEvent;
import motordevelopment.motorclient.mixininterface.IChatHud;
import motordevelopment.motorclient.mixininterface.IChatHudLine;
import motordevelopment.motorclient.mixininterface.IChatHudLineVisible;
import motordevelopment.motorclient.mixininterface.IMessageHandler;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.misc.BetterChat;
import motordevelopment.motorclient.systems.modules.render.NoRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin implements IChatHud {
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    @Final
    private List<ChatHudLine.Visible> visibleMessages;
    @Shadow
    @Final
    private List<ChatHudLine> messages;

    @Unique
    private BetterChat betterChat;
    @Unique
    private int nextId;
    @Unique
    private boolean skipOnAddMessage;

    @Shadow
    public abstract void addMessage(Text message, @Nullable MessageSignatureData signatureData, @Nullable MessageIndicator indicator);

    @Shadow
    public abstract void addMessage(Text message);

    @Override
    public void motor$add(Text message, int id) {
        nextId = id;
        addMessage(message);
        nextId = 0;
    }

    @Inject(method = "addVisibleMessage", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", shift = At.Shift.AFTER))
    private void onAddMessageAfterNewChatHudLineVisible(ChatHudLine message, CallbackInfo ci) {
        ((IChatHudLine) (Object) visibleMessages.getFirst()).motor$setId(nextId);
    }

    @Inject(method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", shift = At.Shift.AFTER))
    private void onAddMessageAfterNewChatHudLine(ChatHudLine message, CallbackInfo ci) {
        ((IChatHudLine) (Object) messages.getFirst()).motor$setId(nextId);
    }

    @SuppressWarnings("DataFlowIssue")
    @ModifyExpressionValue(method = "addVisibleMessage", at = @At(value = "NEW", target = "(ILnet/minecraft/text/OrderedText;Lnet/minecraft/client/gui/hud/MessageIndicator;Z)Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;"))
    private ChatHudLine.Visible onAddMessage_modifyChatHudLineVisible(ChatHudLine.Visible line, @Local(ordinal = 1) int j) {
        IMessageHandler handler = (IMessageHandler) client.getMessageHandler();
        if (handler == null) return line;

        IChatHudLineVisible motorLine = (IChatHudLineVisible) (Object) line;

        motorLine.motor$setSender(handler.motor$getSender());
        motorLine.motor$setStartOfEntry(j == 0);

        return line;
    }

    @ModifyExpressionValue(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At(value = "NEW", target = "(ILnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)Lnet/minecraft/client/gui/hud/ChatHudLine;"))
    private ChatHudLine onAddMessage_modifyChatHudLine(ChatHudLine line) {
        IMessageHandler handler = (IMessageHandler) client.getMessageHandler();
        if (handler == null) return line;

        ((IChatHudLine) (Object) line).motor$setSender(handler.motor$getSender());
        return line;
    }

    @Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", cancellable = true)
    private void onAddMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
        if (skipOnAddMessage) return;

        ReceiveMessageEvent event = MotorClient.EVENT_BUS.post(ReceiveMessageEvent.get(message, indicator, nextId));

        if (event.isCancelled()) ci.cancel();
        else {
            visibleMessages.removeIf(msg -> ((IChatHudLine) (Object) msg).motor$getId() == nextId && nextId != 0);

            for (int i = messages.size() - 1; i > -1; i--) {
                if (((IChatHudLine) (Object) messages.get(i)).motor$getId() == nextId && nextId != 0) {
                    messages.remove(i);
                    getBetterChat().removeLine(i);
                }
            }

            if (event.isModified()) {
                ci.cancel();

                skipOnAddMessage = true;
                addMessage(event.getMessage(), signatureData, event.getIndicator());
                skipOnAddMessage = false;
            }
        }
    }

    //modify max lengths for messages and visible messages
    @ModifyExpressionValue(method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", at = @At(value = "CONSTANT", args = "intValue=100"))
    private int maxLength(int size) {
        if (Modules.get() == null || !getBetterChat().isLongerChat()) return size;

        return size + betterChat.getExtraChatLines();
    }

    @ModifyExpressionValue(method = "addVisibleMessage", at = @At(value = "CONSTANT", args = "intValue=100"))
    private int maxLengthVisible(int size) {
        if (Modules.get() == null || !getBetterChat().isLongerChat()) return size;

        return size + betterChat.getExtraChatLines();
    }

    // Player Heads

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;ceil(F)I"))
    private int onRender_modifyWidth(int width) {
        return getBetterChat().modifyChatWidth(width);
    }

    @ModifyReceiver(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"))
    private DrawContext onRender_beforeDrawTextWithShadow(DrawContext context, TextRenderer textRenderer, OrderedText text, int x, int y, int color, @Local ChatHudLine.Visible line) {
        getBetterChat().drawPlayerHead(context, line, y, color);
        return context;
    }

    // No Message Signature Indicator

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;indicator()Lnet/minecraft/client/gui/hud/MessageIndicator;"))
    private MessageIndicator onRender_modifyIndicator(MessageIndicator indicator) {
        return Modules.get().get(NoRender.class).noMessageSignatureIndicator() ? null : indicator;
    }

    // Anti spam

    @Inject(method = "addVisibleMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;isChatFocused()Z"))
    private void onBreakChatMessageLines(ChatHudLine message, CallbackInfo ci, @Local List<OrderedText> list) {
        if (Modules.get() == null) return; // baritone calls addMessage before we initialise

        getBetterChat().lines.addFirst(list.size());
    }

    @Inject(method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", at = @At(value = "INVOKE", target = "Ljava/util/List;remove(I)Ljava/lang/Object;"))
    private void onRemoveMessage(ChatHudLine message, CallbackInfo ci) {
        if (Modules.get() == null) return;

        int extra = getBetterChat().isLongerChat() ? getBetterChat().getExtraChatLines() : 0;
        int size = betterChat.lines.size();

        while (size > 100 + extra) {
            betterChat.lines.removeLast();
            size--;
        }
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void onClear(boolean clearHistory, CallbackInfo ci) {
        getBetterChat().lines.clear();
    }

    @Inject(method = "refresh", at = @At("HEAD"))
    private void onRefresh(CallbackInfo ci) {
        getBetterChat().lines.clear();
    }

    // Other
    @Unique
    private BetterChat getBetterChat() {
        if (betterChat == null) {
            betterChat = Modules.get().get(BetterChat.class);
        }

        return betterChat;
    }
}
