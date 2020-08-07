package minegame159.meteorclient.mixin;

import minegame159.meteorclient.accountsfriends.Friend;
import minegame159.meteorclient.accountsfriends.FriendManager;
import minegame159.meteorclient.commands.commands.Ignore;
import minegame159.meteorclient.mixininterface.IChatHudLine;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.misc.AntiSpam;
import minegame159.meteorclient.modules.render.FriendColor;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Shadow @Final private List<ChatHudLine> visibleMessages;

    @Shadow public abstract int getWidth();

    @Shadow @Final private List<ChatHudLine> messages;

    private String lastMessage = null;

    @Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/text/Text;IIZ)V", cancellable = true)
    private void onAddMessage(Text message, int messageId, int timestamp, boolean bl, CallbackInfo info) {
        // Ignore players
        for (String name : Ignore.ignoredPlayers) {
            if (message.toString().contains("<" + name + ">")) {
                info.cancel();
                return;
            }
        }

        // Anti Spam
        AntiSpam antiSpam = ModuleManager.INSTANCE.get(AntiSpam.class);
        for (int i = 0; i < antiSpam.getDepth(); i++) {
            if (checkMsg(message.asFormattedString(), timestamp, messageId, i)) {
                if (antiSpam.isMoveToBottom() && i != 0) {
                    ChatHudLine msg = visibleMessages.remove(i);
                    visibleMessages.add(0, msg);
                    messages.add(0, msg);
                }

                info.cancel();
                return;
            }
        }

        //Friend Colour
        if (ModuleManager.INSTANCE.get(FriendColor.class).isActive() && !message.asString().equals(lastMessage)) {
            String convert = message.asString();
            List<Friend> friends = FriendManager.INSTANCE.getAll();
            for (Friend friend : friends) {
                if (convert.contains(friend.name)) {
                    convert = convert.replaceAll(friend.name, "§d" + friend.name + "§r");
                }
            }
            lastMessage = convert;
            Utils.sendMessage(convert);
            lastMessage = null;
            info.cancel();
        }
    }

    private boolean checkMsg(String newMsg, int newTimestamp, int newId, int msgI) {
        ChatHudLine msg = visibleMessages.size() > msgI ? visibleMessages.get(msgI) : null;
        if (msg == null) return false;
        String msgString = msg.getText().asFormattedString();

        if (msgString.equals(newMsg)) {
            msgString += Formatting.GRAY + " (2)";

            ((IChatHudLine) msg).setText(new LiteralText(msgString));
            ((IChatHudLine) msg).setTimestamp(newTimestamp);
            ((IChatHudLine) msg).setId(newId);

            return true;
        } else {
            Matcher matcher = Pattern.compile(".*(\\([0-9]+\\)$)").matcher(msgString);

            if (matcher.matches()) {
                String group = matcher.group(1);
                int number = Integer.parseInt(group.substring(1, group.length() - 1));

                int i = msgString.lastIndexOf(group);
                msgString = msgString.substring(0, i - Formatting.GRAY.toString().length() - 1);

                if (msgString.equals(newMsg)) {
                    msgString += Formatting.GRAY + " (" + (number + 1) + ")";

                    ((IChatHudLine) msg).setText(new LiteralText(msgString));
                    ((IChatHudLine) msg).setTimestamp(newTimestamp);
                    ((IChatHudLine) msg).setId(newId);

                    return true;
                }
            }

            return false;
        }
    }
}
