package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.Config;
import minegame159.meteorclient.events.SendMessageEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class FancyChat extends ToggleModule {

    public FancyChat() {
        super(Category.Misc, "FancyChat", "Make your chat messages fancy!");
    }
    @EventHandler
    private final Listener<SendMessageEvent> onSendMessage = new Listener<>(event -> {
        if (!event.msg.startsWith(Config.INSTANCE.getPrefix() + "b")) {
            event.msg = changeMessage(event.msg);
        }
    });

    private String changeMessage(String changeFrom) {
        String output = changeFrom;
        output = output.replace("A","ᴀ");
        output = output.replace("a", "ᴀ");
        output = output.replace("B", "ʙ");
        output = output.replace("b", "ʙ");
        output = output.replace("C", "ᴄ");
        output = output.replace("c", "ᴄ");
        output = output.replace("D", "ᴅ");
        output = output.replace("d", "ᴅ");
        output = output.replace("E", "ᴇ");
        output = output.replace("e", "ᴇ");
        output = output.replace("F", "ꜰ");
        output = output.replace("f", "ꜰ");
        output = output.replace("G", "ɢ");
        output = output.replace("g", "ɢ");
        output = output.replace("H", "ʜ");
        output = output.replace("h", "ʜ");
        output = output.replace("I", "ɪ");
        output = output.replace("i", "ɪ");
        output = output.replace("J", "ᴊ");
        output = output.replace("j", "ᴊ");
        output = output.replace("K", "ᴋ");
        output = output.replace("k", "ᴋ");
        output = output.replace("L", "ʟ");
        output = output.replace("l", "ʟ");
        output = output.replace("M", "ᴍ");
        output = output.replace("m", "ᴍ");
        output = output.replace("N", "ɴ");
        output = output.replace("n", "ɴ");
        output = output.replace("O", "ᴏ");
        output = output.replace("o", "ᴏ");
        output = output.replace("P", "ᴩ");
        output = output.replace("p", "ᴩ");
        output = output.replace("Q", "q");
        output = output.replace("q", "q");
        output = output.replace("R", "ʀ");
        output = output.replace("r", "ʀ");
        output = output.replace("S", "ꜱ");
        output = output.replace("s", "ꜱ");
        output = output.replace("T", "ᴛ");
        output = output.replace("t", "ᴛ");
        output = output.replace("U", "ᴜ");
        output = output.replace("u", "ᴜ");
        output = output.replace("V", "ᴠ");
        output = output.replace("v", "ᴠ");
        output = output.replace("W", "ᴡ");
        output = output.replace("w", "ᴡ");
        output = output.replace("X", "x");
        output = output.replace("x", "x");
        output = output.replace("Y", "y");
        output = output.replace("y", "y");
        output = output.replace("Z", "ᴢ");
        output = output.replace("z", "ᴢ");

        return output;
    }
}
