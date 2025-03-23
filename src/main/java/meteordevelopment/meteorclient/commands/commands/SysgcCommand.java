package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

public class SysgcCommand extends Command {
    public SysgcCommand() {
        super("sysgc", "System.gc();");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            System.gc();
            return SINGLE_SUCCESS;
        });
    }
}
