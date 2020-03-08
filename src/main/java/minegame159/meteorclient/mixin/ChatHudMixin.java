package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MixinValues;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.util.Texts;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Shadow public abstract void removeMessage(int messageId);

    @Shadow public abstract double getChatScale();

    @Shadow @Final private MinecraftClient client;

    @Shadow public abstract boolean isChatFocused();

    @Shadow @Final private List<ChatHudLine> visibleMessages;

    @Shadow public abstract int getWidth();

    @Shadow private int scrolledLines;

    @Shadow private boolean field_2067;

    @Shadow public abstract void scroll(double amount);

    @Shadow @Final private List<ChatHudLine> messages;

    @Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/text/Text;IIZ)V", cancellable = true)
    private void onAddMessage(Text message, int messageId, int timestamp, boolean bl, CallbackInfo info) {
        if (messageId != 0) {
            this.removeMessage(messageId);
        }

        int i = MathHelper.floor((double)this.getWidth() / this.getChatScale());
        List<Text> list = Texts.wrapLines(message, i, this.client.textRenderer, false, false);
        boolean bl2 = this.isChatFocused();

        Text text;
        for(Iterator var8 = list.iterator(); var8.hasNext(); this.visibleMessages.add(0, new ChatHudLine(timestamp, text, messageId))) {
            text = (Text)var8.next();
            if (bl2 && this.scrolledLines > 0) {
                this.field_2067 = true;
                this.scroll(1.0D);
            }
        }

        while(this.visibleMessages.size() > MixinValues.getChatLength()) {
            this.visibleMessages.remove(this.visibleMessages.size() - 1);
        }

        if (!bl) {
            this.messages.add(0, new ChatHudLine(timestamp, message, messageId));

            while(this.messages.size() > MixinValues.getChatLength()) {
                this.messages.remove(this.messages.size() - 1);
            }
        }

        info.cancel();
    }
}
