package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import net.minecraft.command.CommandSource;

public class QuickSwapCommand extends Command {
    public QuickSwapCommand() {
        super("quick-swap", "Swap an item in your hotbar for an item in your inventory.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("from", IntegerArgumentType.integer(0, SlotUtils.MAIN_END - SlotUtils.MAIN_START))
                .then(argument("to", IntegerArgumentType.integer(0, SlotUtils.HOTBAR_END - SlotUtils.HOTBAR_START))
                        .executes(context -> {
                            // TODO: am i stupid? how do these fucking slot ids indexes work
                            final var f = context.getArgument("from", Integer.class);
                            final var t = context.getArgument("to", Integer.class);
                            InvUtils.quickSwap().fromHotbar(t + 1).toMain(f + 1);
                            return SINGLE_SUCCESS;
                        })));
    }
}
