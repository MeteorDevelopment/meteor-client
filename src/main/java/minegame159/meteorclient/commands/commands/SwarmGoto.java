package minegame159.meteorclient.commands.commands;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.combat.Swarm;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SwarmGoto extends Command {

    public SwarmGoto() {
        super("s", "(highlight)goto <x> <z>(default) - Path to a destination.");
    }

    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("goto")
                .then(argument("x", IntegerArgumentType.integer())
                        .then(argument("z", IntegerArgumentType.integer()).executes(context -> {
                                    int x = context.getArgument("x", Integer.class);
                                    int z = context.getArgument("z", Integer.class);
                                    Swarm swarm = ModuleManager.INSTANCE.get(Swarm.class);
                                    if (swarm.isActive()) {
                                        if (swarm.currentMode.get() == Swarm.Mode.QUEEN && swarm.server != null) {
                                            swarm.server.sendMessage(context.getInput());
                                        } else if (swarm.currentMode.get() != Swarm.Mode.QUEEN) {
                                            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(x, z));
                                        }
                                    }
                                    return SINGLE_SUCCESS;
                                })
                        )
                )
        );
    }

}
