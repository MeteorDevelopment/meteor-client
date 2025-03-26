package meteordevelopment.meteorclient.commands.commands;

import static meteordevelopment.meteorclient.utils.player.ChatUtils.sendMsg;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class SessionCommand extends Command {
    public SessionCommand() {
        super("session", "Get session info.");
    }

    @Override
    public void build(final LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            final var s = mc.getSession();
            sendMsg(Text.of(s.getUsername() + " session id: " + s.getSessionId())
                .getWithStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, s.getSessionId()))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("copy"))))
                .getFirst());
            return SINGLE_SUCCESS;
        });
    }
}
