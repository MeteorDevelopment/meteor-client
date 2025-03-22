package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

public class HotbarSwitchCommand extends Command {
    public HotbarSwitchCommand() {
        super("hotbar-switch", "Switch the held item for the one above it.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("row", IntegerArgumentType.integer(0, 3)).executes(context -> {
            // TODO
            return SINGLE_SUCCESS;
        }));
    }
}
