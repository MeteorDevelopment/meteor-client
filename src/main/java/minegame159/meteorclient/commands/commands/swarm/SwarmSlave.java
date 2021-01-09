package minegame159.meteorclient.commands.commands.swarm;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.combat.Swarm;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SwarmSlave extends Command {

    public SwarmSlave() {
        super("swarm", "(highlight)slave (default)- Slave this account to the Queen.");
    }


    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("slave").executes(context -> {
                    Swarm swarm = ModuleManager.INSTANCE.get(Swarm.class);
                    if (swarm.isActive()) {
                        if (swarm.client == null)
                            swarm.runClient();
                    }
                    return SINGLE_SUCCESS;
                })

        );
    }
}
