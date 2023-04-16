/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

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
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.ChatVisibility;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.message.MessageSignatureData;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
            visibleMessages.removeIf((msg) -> ((IChatHudLine) (Object) msg).getId() == nextId && nextId != 0);
            messages.removeIf((msg) -> ((IChatHudLine) (Object) msg).getId() == nextId && nextId != 0);

            if (event.isModified()) {
                info.cancel();

                skipOnAddMessage = true;
                addMessage(event.getMessage(), signature, ticks, event.getIndicator(), refresh);
                skipOnAddMessage = false;
            }
        }
    }

    @Redirect(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V", slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/ChatHud;visibleMessages:Ljava/util/List;")), at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
    private int addMessageListSizeProxy(List<ChatHudLine> list) {
        BetterChat betterChat = Modules.get().get(BetterChat.class);
        if (betterChat.isLongerChat() && betterChat.getChatLength() >= 100) return list.size() - betterChat.getChatLength();
        return list.size();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(MatrixStack matrices, int currentTick, int mouseX, int mouseY, CallbackInfo info) {
        if (!Modules.get().get(BetterChat.class).displayPlayerHeads()) return;
        if (mc.options.getChatVisibility().getValue() == ChatVisibility.HIDDEN) return;
        int maxLineCount = mc.inGameHud.getChatHud().getVisibleLineCount();

        double d = mc.options.getChatOpacity().getValue() * 0.8999999761581421D + 0.10000000149011612D;
        double g = 9.0D * (mc.options.getChatLineSpacing().getValue() + 1.0D);
        double h = -8.0D * (mc.options.getChatLineSpacing().getValue() + 1.0D) + 4.0D * mc.options.getChatLineSpacing().getValue() + 8.0D;

        float chatScale = (float) this.getChatScale();
        float scaledHeight = mc.getWindow().getScaledHeight();

        matrices.push();
        matrices.scale(chatScale, chatScale, 1.0f);
        matrices.translate(2.0f, MathHelper.floor((scaledHeight - 40) / chatScale) - g - 0.1f, 10.0f);
        RenderSystem.enableBlend();
        for(int m = 0; m + this.scrolledLines < this.visibleMessages.size() && m < maxLineCount; ++m) {
            ChatHudLine.Visible chatHudLine = this.visibleMessages.get(m + this.scrolledLines);
            if (chatHudLine != null) {
                int x = currentTick - chatHudLine.addedTime();
                if (x < 200 || isChatFocused()) {
                    double o = isChatFocused() ? 1.0D : getMessageOpacityMultiplier(x);
                    if (o * d > 0.01D) {
                        double s = ((double)(-m) * g);
                        StringCharacterVisitor visitor = new StringCharacterVisitor();
                        chatHudLine.content().accept(visitor);
                        drawIcon(matrices, visitor.result.toString(), (int)(s + h), (float)(o * d));
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

    private void drawIcon(MatrixStack matrices, String line, int y, float opacity) {
        if (METEOR_PREFIX_REGEX.matcher(line).find()) {
            RenderSystem.setShaderTexture(0, METEOR_CHAT_ICON);
            matrices.push();
            RenderSystem.setShaderColor(1, 1, 1, opacity);
            matrices.translate(0, y, 0);
            matrices.scale(0.125f, 0.125f, 1);
            DrawableHelper.drawTexture(matrices, 0, 0, 0f, 0f, 64, 64, 64, 64);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            matrices.pop();
            return;
        } else if (BARITONE_PREFIX_REGEX.matcher(line).find()) {
            RenderSystem.setShaderTexture(0, BARITONE_CHAT_ICON);
            matrices.push();
            RenderSystem.setShaderColor(1, 1, 1, opacity);
            matrices.translate(0, y, 10);
            matrices.scale(0.125f, 0.125f, 1);
            DrawableHelper.drawTexture(matrices, 0, 0, 0f, 0f, 64, 64, 64, 64);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            matrices.pop();
            return;
        }

        Identifier skin = getMessageTexture(line);
        if (skin != null) {
            RenderSystem.setShaderColor(1, 1, 1, opacity);
            RenderSystem.setShaderTexture(0, skin);
            DrawableHelper.drawTexture(matrices, 0, y, 8, 8, 8.0F, 8.0F,8, 8, 64, 64);
            DrawableHelper.drawTexture(matrices, 0, y, 8, 8, 40.0F, 8.0F,8, 8, 64, 64);
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

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;indicator()Lnet/minecraft/client/gui/hud/MessageIndicator;"))
    private MessageIndicator onMessageIndicator(ChatHudLine.Visible message) {
        return Modules.get().get(NoRender.class).noMessageSignatureIndicator() ? null : message.indicator();
    }
}
