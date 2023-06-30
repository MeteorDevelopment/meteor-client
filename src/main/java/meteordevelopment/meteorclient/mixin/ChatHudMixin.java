/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.mixininterface.IChatHudLine;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.misc.MeteorIdentifier;
import meteordevelopment.meteorclient.utils.misc.text.StringCharacterVisitor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.ChatVisibility;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin implements IChatHud {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Pattern METEOR_PREFIX_REGEX = Pattern.compile("^\\s{0,2}(<[0-9]{1,2}:[0-9]{1,2}>\\s)?\\[Meteor]");
    private static final Pattern BARITONE_PREFIX_REGEX = Pattern.compile("^\\s{0,2}(<[0-9]{1,2}:[0-9]{1,2}>\\s)?\\[Baritone]");
    private static final Identifier METEOR_CHAT_ICON = new MeteorIdentifier("textures/icons/chat/meteor.png");
    private static final Identifier BARITONE_CHAT_ICON = new MeteorIdentifier("textures/icons/chat/baritone.png");

    @Shadow @Final private List<ChatHudLine.Visible> visibleMessages;
    @Shadow private int scrolledLines;

    @Unique private int nextId;
    @Unique private boolean skipOnAddMessage;

    @Override
    public void add(Text message, int id) {
        nextId = id;
        addMessage(message);
        nextId = 0;
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", ordinal = 0, shift = At.Shift.AFTER))
    private void onAddMessageAfterNewChatHudLineVisible(Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh, CallbackInfo info) {
        ((IChatHudLine) (Object) visibleMessages.get(0)).setId(nextId);
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", ordinal = 1, shift = At.Shift.AFTER))
    private void onAddMessageAfterNewChatHudLine(Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh, CallbackInfo info) {
        ((IChatHudLine) (Object) messages.get(0)).setId(nextId);
    }

    @Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V", cancellable = true)
    private void onAddMessage(Text message, @Nullable MessageSignatureData signature, int ticks, @Nullable MessageIndicator indicator, boolean refresh, CallbackInfo info) {
        if (skipOnAddMessage) return;

        ReceiveMessageEvent event = MeteorClient.EVENT_BUS.post(ReceiveMessageEvent.get(message, indicator, nextId));

        if (event.isCancelled()) info.cancel();
        else {
            visibleMessages.removeIf(msg -> ((IChatHudLine) (Object) msg).getId() == nextId && nextId != 0);

            for (int i = messages.size() - 1; i > -1 ; i--) {
                if (((IChatHudLine) (Object) messages.get(i)).getId() == nextId && nextId != 0) {
                    messages.remove(i);
                    Modules.get().get(BetterChat.class).lines.remove(i);
                }
            }

            if (event.isModified()) {
                info.cancel();

                skipOnAddMessage = true;
                addMessage(event.getMessage(), signature, ticks, event.getIndicator(), refresh);
                skipOnAddMessage = false;
            }
        }
    }

    @ModifyExpressionValue(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V",
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/ChatHud;visibleMessages:Ljava/util/List;")), at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
    private int addMessageListSizeProxy(int size) {
        BetterChat betterChat = Modules.get().get(BetterChat.class);
        if (betterChat.isLongerChat() && betterChat.getChatLength() >= 100) return size - betterChat.getChatLength();
        return size;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int currentTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (!Modules.get().get(BetterChat.class).displayPlayerHeads()) return;
        if (mc.options.getChatVisibility().getValue() == ChatVisibility.HIDDEN) return;
        int maxLineCount = mc.inGameHud.getChatHud().getVisibleLineCount();

        double d = mc.options.getChatOpacity().getValue() * 0.8999999761581421D + 0.10000000149011612D;
        double g = 9.0D * (mc.options.getChatLineSpacing().getValue() + 1.0D);
        double h = -8.0D * (mc.options.getChatLineSpacing().getValue() + 1.0D) + 4.0D * mc.options.getChatLineSpacing().getValue() + 8.0D;

        float chatScale = (float) getChatScale();
        float scaledHeight = mc.getWindow().getScaledHeight();

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.scale(chatScale, chatScale, 1.0f);
        matrices.translate(2.0f, MathHelper.floor((scaledHeight - 40) / chatScale) - g - 0.1f, 10.0f);
        RenderSystem.enableBlend();
        for (int m = 0; m + scrolledLines < visibleMessages.size() && m < maxLineCount; ++m) {
            ChatHudLine.Visible chatHudLine = visibleMessages.get(m + scrolledLines);
            if (chatHudLine != null) {
                int x = currentTick - chatHudLine.addedTime();
                if (x < 200 || isChatFocused()) {
                    double o = isChatFocused() ? 1.0D : getMessageOpacityMultiplier(x);
                    if (o * d > 0.01D) {
                        double s = -m * g;
                        StringCharacterVisitor visitor = new StringCharacterVisitor();
                        chatHudLine.content().accept(visitor);
                        drawIcon(context, matrices, visitor.result.toString(), (int) (s + h), (float) (o * d));
                    }
                }
            }
        }
        RenderSystem.disableBlend();
        matrices.pop();

    }

    private boolean isChatFocused() {
        return mc.currentScreen instanceof ChatScreen;
    }

    @Shadow
    private static double getMessageOpacityMultiplier(int age) {
        throw new AssertionError();
    }

    @Shadow
    protected abstract void addMessage(Text message, @Nullable MessageSignatureData signature, int ticks, @Nullable MessageIndicator indicator, boolean refresh);

    @Shadow
    public abstract void addMessage(Text message);

    @Shadow
    @Final
    private List<ChatHudLine> messages;

    @Shadow
    public abstract double getChatScale();

    private void drawIcon(DrawContext drawContext, MatrixStack matrices, String line, int y, float opacity) {
        if (METEOR_PREFIX_REGEX.matcher(line).find()) {
            matrices.push();
            RenderSystem.setShaderColor(1, 1, 1, opacity);
            matrices.translate(0, y, 0);
            matrices.scale(0.125f, 0.125f, 1);
            drawContext.drawTexture(METEOR_CHAT_ICON, 0, 0, 0f, 0f, 64, 64, 64, 64);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            matrices.pop();
            return;
        } else if (BARITONE_PREFIX_REGEX.matcher(line).find()) {
            matrices.push();
            RenderSystem.setShaderColor(1, 1, 1, opacity);
            matrices.translate(0, y, 10);
            matrices.scale(0.125f, 0.125f, 1);
            drawContext.drawTexture(BARITONE_CHAT_ICON, 0, 0, 0f, 0f, 64, 64, 64, 64);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            matrices.pop();
            return;
        }

        Identifier skin = getMessageTexture(line);
        if (skin != null) {
            RenderSystem.setShaderColor(1, 1, 1, opacity);
            drawContext.drawTexture(skin, 0, y, 8, 8, 8.0F, 8.0F, 8, 8, 64, 64);
            drawContext.drawTexture(skin, 0, y, 8, 8, 40.0F, 8.0F, 8, 8, 64, 64);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    private static Identifier getMessageTexture(String message) {
        if (mc.getNetworkHandler() == null) return null;
        for (String part : message.split("(§.)|[^\\w]")) {
            if (part.isBlank()) continue;
            PlayerListEntry p = mc.getNetworkHandler().getPlayerListEntry(part);
            if (p != null) {
                return p.getSkinTexture();
            }
        }
        return null;
    }

    // No Message Signature Indicator

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;indicator()Lnet/minecraft/client/gui/hud/MessageIndicator;"))
    private MessageIndicator onRender_modifyIndicator(MessageIndicator indicator) {
        return Modules.get().get(NoRender.class).noMessageSignatureIndicator() ? null : indicator;
    }

    // Anti spam

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;isChatFocused()Z"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onBreakChatMessageLines(Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh, CallbackInfo ci, int i, List<OrderedText> list) {
        Modules.get().get(BetterChat.class).lines.add(0, list.size());
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V",
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/ChatHud;messages:Ljava/util/List;")), at = @At(value = "INVOKE", target = "Ljava/util/List;remove(I)Ljava/lang/Object;"))
    private void onRemoveMessage(Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh, CallbackInfo ci) {
        BetterChat betterChat = Modules.get().get(BetterChat.class);
        int size = betterChat.lines.size() - (betterChat.isLongerChat() && betterChat.getChatLength() >= 100 ? betterChat.getChatLength() : 0);

        while (size > 100) {
            betterChat.lines.remove(size - 1);
            size--;
        }
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void onClear(boolean clearHistory, CallbackInfo ci) {
        Modules.get().get(BetterChat.class).lines.clear();
    }

    @Inject(method = "refresh", at = @At("HEAD"))
    private void onRefresh(CallbackInfo ci) {
        Modules.get().get(BetterChat.class).lines.clear();
    }
}
