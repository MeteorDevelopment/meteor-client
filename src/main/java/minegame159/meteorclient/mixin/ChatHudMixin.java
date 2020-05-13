package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IChatHudLine;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.misc.AntiSpam;
import minegame159.meteorclient.modules.misc.LongerChat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.util.Texts;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        info.cancel();

        // Anti Spam
        if (ModuleManager.INSTANCE.isActive(AntiSpam.class)) {
            ChatHudLine lastMsg = visibleMessages.get(0);

            if (lastMsg.getText().asFormattedString().equals(message.asFormattedString())) {
                String string = lastMsg.getText().asFormattedString();
                string += Formatting.GRAY + " (2)";

                ((IChatHudLine) lastMsg).setText(new LiteralText(string));
                return;
            } else {
                String string = lastMsg.getText().asFormattedString();
                Matcher matcher = Pattern.compile(".*(\\([0-9]+\\)$)").matcher(string);

                if (matcher.matches()) {
                    String group = matcher.group(1);
                    int number = Integer.parseInt(group.substring(1, group.length() - 1));

                    int i = string.lastIndexOf(group);
                    string = string.substring(0, i - Formatting.GRAY.toString().length() - 1);

                    if (string.equals(message.asFormattedString())) {
                        string += Formatting.GRAY + " (" + (number + 1) + ")";
                        ((IChatHudLine) lastMsg).setText(new LiteralText(string));
                        return;
                    }
                }
            }
        }

        // Normal things
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

        while(this.visibleMessages.size() > ModuleManager.INSTANCE.get(LongerChat.class).getMaxLineCount()) {
            this.visibleMessages.remove(this.visibleMessages.size() - 1);
        }

        if (!bl) {
            this.messages.add(0, new ChatHudLine(timestamp, message, messageId));

            while(this.messages.size() > ModuleManager.INSTANCE.get(LongerChat.class).getMaxLineCount()) {
                this.messages.remove(this.messages.size() - 1);
            }
        }
    }
}
