package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.combat.Swarm;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SwarmQueen extends Command {

    public SwarmQueen() {
        super("s", "(highlight)queen (default)- Start a new swarm as the queen.");
    }


    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("queen").executes(context -> {
                    Swarm swarm = ModuleManager.INSTANCE.get(Swarm.class);
                    if (swarm.isActive()) {
                        if (swarm.server == null)
                            swarm.runServer();
                    }
                    return SINGLE_SUCCESS;
                })

        );
    }
}
