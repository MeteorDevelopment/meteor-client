package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.combat.Swarm;
import minegame159.meteorclient.utils.Chat;
import net.minecraft.command.CommandSource;

import java.util.Arrays;
import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SwarmHelp extends Command {

    public SwarmHelp() {
        super("s", "(highlight)help(default) - Prints a list of all commands, and formatting information.");
    }

    private final List<Command> swarmCommands = Arrays.asList(new SwarmCloseConnections(),
            new SwarmEscape(),
            new SwarmGoto(),
            new SwarmHelp(),
            new SwarmInfinityMiner(),
            new SwarmMine(),
            new SwarmRelease(),
            new SwarmSlave(),
            new SwarmStop()
    );

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("help").executes(context -> {
                    Swarm swarm = ModuleManager.INSTANCE.get(Swarm.class);
                    if (swarm.isActive()) {
                        Chat.info("(highlight)Welcome to Swarm.");
                        Chat.info("Below are all listed commands, check the docs for more detailed information");
                        Chat.info("<> Denotes a field to fill. <?> Denotes an optional field.");
                        for (Command s : swarmCommands) {
                            Chat.info(s.description);
                        }
                    }
                    return SINGLE_SUCCESS;
                })
        );
    }
}
