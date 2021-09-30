/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import meteordevelopment.meteorclient.utils.misc.text.StringCharacterVisitor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.ChatVisibility;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin implements IChatHud {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Pattern METEOR_PREFIX_REGEX = Pattern.compile("^\\s{0,2}(<[0-9]{1,2}:[0-9]{1,2}>\\s)?\\[Meteor\\]");
    private static final Pattern BARITONE_PREFIX_REGEX = Pattern.compile("^\\s{0,2}(<[0-9]{1,2}:[0-9]{1,2}>\\s)?\\[Baritone\\]");
    private static final Identifier METEOR_CHAT_ICON = new Identifier("meteor-client", "textures/icons/chat/meteor.png");
    private static final Identifier BARITONE_CHAT_ICON = new Identifier("meteor-client", "textures/icons/chat/baritone.png");

    @Shadow @Final private List<ChatHudLine<OrderedText>> visibleMessages;
    @Shadow private int scrolledLines;

    @Shadow protected abstract void addMessage(Text message, int messageId, int timestamp, boolean refresh);

    @Unique private boolean skipOnAddMessage;

    @Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/text/Text;I)V", cancellable = true)
    private void onAddMessage(Text text, int id, CallbackInfo info) {
        if (skipOnAddMessage) return;

        ReceiveMessageEvent event = MeteorClient.EVENT_BUS.post(ReceiveMessageEvent.get(text, id));

        if (event.isCancelled()) info.cancel();
        else if (event.isModified()) {
            info.cancel();

            skipOnAddMessage = true;
            addMessage(event.getMessage(), id);
            skipOnAddMessage = false;
        }
    }

    @Redirect(method = "addMessage(Lnet/minecraft/text/Text;IIZ)V", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
    private int addMessageListSizeProxy(List<ChatHudLine> list) {
        BetterChat betterChat = Modules.get().get(BetterChat.class);
        return betterChat.isLongerChat() && betterChat.getChatLength() > 100 ? 1 : list.size();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(MatrixStack matrices, int tickDelta, CallbackInfo ci) {
        if (!Modules.get().get(BetterChat.class).displayPlayerHeads()) return;
        if (mc.options.chatVisibility == ChatVisibility.HIDDEN) return;
        int maxLineCount = mc.inGameHud.getChatHud().getVisibleLineCount();

        double d = mc.options.chatOpacity * 0.8999999761581421D + 0.10000000149011612D;
        double g = 9.0D * (mc.options.chatLineSpacing + 1.0D);
        double h = -8.0D * (mc.options.chatLineSpacing + 1.0D) + 4.0D * mc.options.chatLineSpacing + 8.0D;

        matrices.push();
        matrices.translate(2, -0.1f, 10);
        RenderSystem.enableBlend();
        for(int m = 0; m + this.scrolledLines < this.visibleMessages.size() && m < maxLineCount; ++m) {
            ChatHudLine<OrderedText> chatHudLine = this.visibleMessages.get(m + this.scrolledLines);
            if (chatHudLine != null) {
                int x = tickDelta - chatHudLine.getCreationTick();
                if (x < 200 || isChatFocused()) {
                    double o = isChatFocused() ? 1.0D : getMessageOpacityMultiplier(x);
                    if (o * d > 0.01D) {
                        double s = ((double)(-m) * g);
                        StringCharacterVisitor visitor = new StringCharacterVisitor();
                        chatHudLine.getText().accept(visitor);
                        drawIcon(matrices, visitor.result.toString(), (int)(s + h), (float)(o * d));
                    }
                }
            }
        }
        RenderSystem.disableBlend();
        matrices.pop();

    }

    @Override
    public void add(Text message, int messageId, int timestamp, boolean refresh) {
        addMessage(message, messageId, timestamp, refresh);
    }

    private boolean isChatFocused() {
        return mc.currentScreen instanceof ChatScreen;
    }

    @Shadow
    private static double getMessageOpacityMultiplier(int age) {
        throw new AssertionError();
    }

    @Shadow
    protected abstract void addMessage(Text message, int messageId);

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
}
