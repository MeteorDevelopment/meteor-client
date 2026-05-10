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
import meteordevelopment.meteorclient.mixininterface.IChatListener;
import meteordevelopment.meteorclient.mixininterface.IGuiMessage;
import meteordevelopment.meteorclient.mixininterface.IGuiMessageVisible;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
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
public abstract class ChatComponentMixin implements IChatHud {
    @Shadow
    @Final
    private Minecraft minecraft;

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
    private void onAddMessageAfterNewGuiMessageVisible(GuiMessage message, CallbackInfo ci) {
        ((IGuiMessage) (Object) trimmedMessages.getFirst()).meteor$setId(nextId);
    }

    @Inject(method = "addMessageToQueue", at = @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V", shift = At.Shift.AFTER))
    private void onAddMessageAfterNewGuiMessage(GuiMessage message, CallbackInfo ci) {
        ((IGuiMessage) (Object) allMessages.getFirst()).meteor$setId(nextId);
    }

    @SuppressWarnings("DataFlowIssue")
    @ModifyExpressionValue(method = "addMessageToDisplayQueue", at = @At(value = "NEW", target = "(Lnet/minecraft/client/multiplayer/chat/GuiMessage;Lnet/minecraft/util/FormattedCharSequence;Z)Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;"))
    private GuiMessage.Line onAddMessage_modifyGuiMessageLine(GuiMessage.Line line, @Local(name = "i") int i) {
        IChatListener handler = (IChatListener) minecraft.getChatListener();
        if (handler == null) return line;

        IGuiMessageVisible meteorLine = (IGuiMessageVisible) (Object) line;

        meteorLine.meteor$setSender(handler.meteor$getSender());
        meteorLine.meteor$setStartOfEntry(i == 0);

        return line;
    }

    @ModifyExpressionValue(method = "addMessage", at = @At(value = "NEW", target = "(ILnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)Lnet/minecraft/client/multiplayer/chat/GuiMessage;"))
    private GuiMessage onAddMessage_modifyGuiMessage(GuiMessage line) {
        IChatListener handler = (IChatListener) minecraft.getChatListener();
        if (handler == null) return line;

        ((IGuiMessage) (Object) line).meteor$setSender(handler.meteor$getSender());
        return line;
    }

    @Inject(at = @At("HEAD"), method = "addMessage", cancellable = true)
    private void onAddMessage(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag indicator, CallbackInfo ci, @Local(argsOnly = true, name = "contents") LocalRef<Component> contents, @Local(argsOnly = true, name = "tag") LocalRef<GuiMessageTag> tag) {
        ReceiveMessageEvent event = MeteorClient.EVENT_BUS.post(ReceiveMessageEvent.get(message, indicator, nextId));

        if (event.isCancelled()) ci.cancel();
        else {
            trimmedMessages.removeIf(msg -> ((IGuiMessage) (Object) msg).meteor$getId() == nextId && nextId != 0);

            for (int i = allMessages.size() - 1; i > -1; i--) {
                if (((IGuiMessage) (Object) allMessages.get(i)).meteor$getId() == nextId && nextId != 0) {
                    allMessages.remove(i);
                    getBetterChat().removeLine(i);
                }
            }

            if (event.isModified()) {
                contents.set(event.getMessage());
                tag.set(event.getIndicator());
            }
        }
    }

    //modify max lengths for messages and visible messages
    @ModifyExpressionValue(method = "addMessageToQueue", at = @At(value = "CONSTANT", args = "intValue=100"))
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
    private void onBreakChatMessageLines(GuiMessage message, CallbackInfo ci, @Local(name = "lines") List<FormattedCharSequence> lines) {
        if (Modules.get() == null) return; // baritone calls addMessage before we initialise

        getBetterChat().lines.addFirst(lines.size());
    }

    @Inject(method = "addMessageToQueue", at = @At(value = "INVOKE", target = "Ljava/util/List;removeLast()Ljava/lang/Object;"))
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
    private void onClearMessages(boolean history, CallbackInfo ci) {
        getBetterChat().lines.clear();
    }

    @Inject(method = "refreshTrimmedMessages", at = @At("HEAD"))
    private void onRefreshTrimmedMessages(CallbackInfo ci) {
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
