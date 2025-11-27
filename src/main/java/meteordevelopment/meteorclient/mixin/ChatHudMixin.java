/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.mixininterface.IChatHudLine;
import meteordevelopment.meteorclient.mixininterface.IChatHudLineVisible;
import meteordevelopment.meteorclient.mixininterface.IMessageHandler;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
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

    @Shadow
    public abstract void addMessage(Text message, @Nullable MessageSignatureData signatureData, @Nullable MessageIndicator indicator);

    @Shadow
    public abstract void addMessage(Text message);

    @Override
    public void meteor$add(Text message, int id) {
        nextId = id;
        addMessage(message);
        nextId = 0;
    }

    @Inject(method = "addVisibleMessage", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", shift = At.Shift.AFTER))
    private void onAddMessageAfterNewChatHudLineVisible(ChatHudLine message, CallbackInfo ci) {
        ((IChatHudLine) (Object) visibleMessages.getFirst()).meteor$setId(nextId);
    }

    @Inject(method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", shift = At.Shift.AFTER))
    private void onAddMessageAfterNewChatHudLine(ChatHudLine message, CallbackInfo ci) {
        ((IChatHudLine) (Object) messages.getFirst()).meteor$setId(nextId);
    }

    @SuppressWarnings("DataFlowIssue")
    @ModifyExpressionValue(method = "addVisibleMessage", at = @At(value = "NEW", target = "(ILnet/minecraft/text/OrderedText;Lnet/minecraft/client/gui/hud/MessageIndicator;Z)Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;"))
    private ChatHudLine.Visible onAddMessage_modifyChatHudLineVisible(ChatHudLine.Visible line, @Local(ordinal = 1) int j) {
        IMessageHandler handler = (IMessageHandler) client.getMessageHandler();
        if (handler == null) return line;

        IChatHudLineVisible meteorLine = (IChatHudLineVisible) (Object) line;

        meteorLine.meteor$setSender(handler.meteor$getSender());
        meteorLine.meteor$setStartOfEntry(j == 0);

        return line;
    }

    @ModifyExpressionValue(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At(value = "NEW", target = "(ILnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)Lnet/minecraft/client/gui/hud/ChatHudLine;"))
    private ChatHudLine onAddMessage_modifyChatHudLine(ChatHudLine line) {
        IMessageHandler handler = (IMessageHandler) client.getMessageHandler();
        if (handler == null) return line;

        ((IChatHudLine) (Object) line).meteor$setSender(handler.meteor$getSender());
        return line;
    }

    @Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", cancellable = true)
    private void onAddMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci, @Local(argsOnly = true) LocalRef<Text> messageRef, @Local(argsOnly = true) LocalRef<MessageIndicator> indicatorRef) {
        ReceiveMessageEvent event = MeteorClient.EVENT_BUS.post(ReceiveMessageEvent.get(message, indicator, nextId));

        if (event.isCancelled()) ci.cancel();
        else {
            visibleMessages.removeIf(msg -> ((IChatHudLine) (Object) msg).meteor$getId() == nextId && nextId != 0);

            for (int i = messages.size() - 1; i > -1; i--) {
                if (((IChatHudLine) (Object) messages.get(i)).meteor$getId() == nextId && nextId != 0) {
                    messages.remove(i);
                    getBetterChat().removeLine(i);
                }
            }

            if (event.isModified()) {
                messageRef.set(event.getMessage());
                indicatorRef.set(event.getIndicator());
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

    @ModifyReceiver(method = "method_71991", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)V"))
    private DrawContext onRender_beforeDrawTextWithShadow(DrawContext context, TextRenderer textRenderer, OrderedText text, int x, int y, int color, @Local(argsOnly = true) ChatHudLine.Visible line) {
        getBetterChat().beforeDrawMessage(context, line, y, color);
        return context;
    }

    @Inject(method = "method_71991", at = @At("TAIL"))
    private void onRender_afterDrawTextWithShadow(int i, DrawContext context, float f, int j, int k, int l, ChatHudLine.Visible visible, int m, float g, CallbackInfo info) {
        getBetterChat().afterDrawMessage(context);
    }

    // No Message Signature Indicator

    @ModifyExpressionValue(method = "method_71992", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;indicator()Lnet/minecraft/client/gui/hud/MessageIndicator;"))
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
