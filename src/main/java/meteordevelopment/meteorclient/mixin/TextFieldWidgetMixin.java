/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin extends ClickableWidget {
    @Shadow
    private Predicate<String> textPredicate;

    @Shadow
    private String text;

    public TextFieldWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Shadow
    public abstract void setSelectionEnd(int index);

    @Shadow
    protected abstract void onChanged(String newText);

    @Shadow
    private int selectionStart;

    @Shadow
    private int selectionEnd;

    @Shadow
    public abstract void setSelectionStart(int cursor);

    @Shadow
    protected abstract MutableText getNarrationMessage();

    @Inject(method = "write", at = @At("HEAD"), cancellable = true)
    private void onWrite(String text, CallbackInfo ci) {
        if (!Modules.get().get(BetterChat.class).isInfiniteChatBox()) return;
        Text chatBoxTranslation = Text.translatable("chat.editBox");
        if (this.getMessage().getString() == chatBoxTranslation.getString()) return;
        int i = Math.min(this.selectionStart, this.selectionEnd);
        int j = Math.max(this.selectionStart, this.selectionEnd);
        String string = SharedConstants.stripInvalidChars(text);
        int l = string.length();

        String string2 = (new StringBuilder(this.text)).replace(i, j, string).toString();
        if (this.textPredicate.test(string2)) {
            this.text = string2;
            this.setSelectionStart(i + l);
            this.setSelectionEnd(this.selectionStart);
            this.onChanged(this.text);
        }
        ci.cancel();
    }
}
