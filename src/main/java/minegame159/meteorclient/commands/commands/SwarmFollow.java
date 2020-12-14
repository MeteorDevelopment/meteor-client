package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SwarmFollow extends Command {

    public SwarmFollow(){
        super("s","(highlight)follow <?player>(default) - Follow a player. Defaults to the Queen.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("follow")
                .then(argument("name", EntityArgumentType.players()).executes(context -> {

                            return SINGLE_SUCCESS;
                        })
                )
        );
    }
}
