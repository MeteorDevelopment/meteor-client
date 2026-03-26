/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatHudMixin implements IChatHud {
    @Shadow
    @Final
    Minecraft minecraft;
    @Shadow
    @Final
    private List<GuiMessage.Line> trimmedMessages;
    @Shadow
    @Final
    private List<GuiMessage> allMessages;

    @Unique
    private BetterChat betterChat;
    @Unique
    private int nextId;

    @Shadow
    public abstract void addClientSystemMessage(Component message);

    @Override
    public void meteor$add(Component message, int id) {
        nextId = id;
        addClientSystemMessage(message);
        nextId = 0;
    }

    @Inject(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V", shift = At.Shift.AFTER))
    private void onAddMessageAfterNewChatHudLineVisible(GuiMessage message, CallbackInfo ci) {
        ((IChatHudLine) (Object) trimmedMessages.getFirst()).meteor$setId(nextId);
    }

    @Inject(method = "addMessageToQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V", at = @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V", shift = At.Shift.AFTER))
    private void onAddMessageAfterNewChatHudLine(GuiMessage message, CallbackInfo ci) {
        ((IChatHudLine) (Object) allMessages.getFirst()).meteor$setId(nextId);
    }

    @SuppressWarnings("DataFlowIssue")
    @ModifyExpressionValue(method = "addMessageToDisplayQueue", at = @At(value = "NEW", target = "(Lnet/minecraft/client/multiplayer/chat/GuiMessage;Lnet/minecraft/util/FormattedCharSequence;Z)Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;"))
    private GuiMessage.Line onAddMessage_modifyChatHudLineVisible(GuiMessage.Line line, @SuppressWarnings("LocalMayBeArgsOnly") @Local(index = 5) int j) {
        IMessageHandler handler = (IMessageHandler) minecraft.getChatListener();
        if (handler == null) return line;

        IChatHudLineVisible meteorLine = (IChatHudLineVisible) (Object) line;

        meteorLine.meteor$setSender(handler.meteor$getSender());
        meteorLine.meteor$setStartOfEntry(j == 0);

        return line;
    }

    @ModifyExpressionValue(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", at = @At(value = "NEW", target = "(ILnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)Lnet/minecraft/client/multiplayer/chat/GuiMessage;"))
    private GuiMessage onAddMessage_modifyChatHudLine(GuiMessage line) {
        IMessageHandler handler = (IMessageHandler) minecraft.getChatListener();
        if (handler == null) return line;

        ((IChatHudLine) (Object) line).meteor$setSender(handler.meteor$getSender());
        return line;
    }

    @Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", cancellable = true)
    private void onAddMessage(Component message, MessageSignature signatureData, GuiMessageSource source, GuiMessageTag indicator, CallbackInfo ci, @Local(argsOnly = true) LocalRef<Component> messageRef, @Local(argsOnly = true) LocalRef<GuiMessageTag> indicatorRef) {
        ReceiveMessageEvent event = MeteorClient.EVENT_BUS.post(ReceiveMessageEvent.get(message, indicator, nextId));

        if (event.isCancelled()) ci.cancel();
        else {
            trimmedMessages.removeIf(msg -> ((IChatHudLine) (Object) msg).meteor$getId() == nextId && nextId != 0);

            for (int i = allMessages.size() - 1; i > -1; i--) {
                if (((IChatHudLine) (Object) allMessages.get(i)).meteor$getId() == nextId && nextId != 0) {
                    allMessages.remove(i);
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
    @ModifyExpressionValue(method = "addMessageToQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V", at = @At(value = "CONSTANT", args = "intValue=100"))
    private int maxLength(int size) {
        if (Modules.get() == null || !getBetterChat().isLongerChat()) return size;

        return size + betterChat.getExtraChatLines();
    }

    @ModifyExpressionValue(method = "addMessageToDisplayQueue", at = @At(value = "CONSTANT", args = "intValue=100"))
    private int maxLengthVisible(int size) {
        if (Modules.get() == null || !getBetterChat().isLongerChat()) return size;

        return size + betterChat.getExtraChatLines();
    }

    // Player Heads

    @ModifyExpressionValue(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;ceil(F)I"))
    private int onRender_modifyWidth(int width) {
        return getBetterChat().modifyChatWidth(width);
    }

    // Anti spam

    @Inject(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;isChatFocused()Z"))
    private void onBreakChatMessageLines(GuiMessage message, CallbackInfo ci, @Local List<FormattedCharSequence> list) {
        if (Modules.get() == null) return; // baritone calls addMessage before we initialise

        getBetterChat().lines.addFirst(list.size());
    }

    @Inject(method = "addMessageToQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V", at = @At(value = "INVOKE", target = "Ljava/util/List;removeLast()Ljava/lang/Object;"))
    private void onRemoveMessage(GuiMessage message, CallbackInfo ci) {
        if (Modules.get() == null) return;

        int extra = getBetterChat().isLongerChat() ? getBetterChat().getExtraChatLines() : 0;
        int size = betterChat.lines.size();

        while (size > 100 + extra) {
            betterChat.lines.removeLast();
            size--;
        }
    }

    @Inject(method = "clearMessages", at = @At("HEAD"))
    private void onClear(boolean clearHistory, CallbackInfo ci) {
        getBetterChat().lines.clear();
    }

    @Inject(method = "refreshTrimmedMessages", at = @At("HEAD"))
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
