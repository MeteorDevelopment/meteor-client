package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.combat.Swarm;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SwarmTarget extends Command {

    public SwarmTarget() {
        super("s","(highlight)target <player>");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("target").then(argument("target", EntityArgumentType.players()).executes(context -> {
            Swarm swarm = ModuleManager.INSTANCE.get(Swarm.class);
            if(swarm.isActive()) {
                if (swarm.currentMode.get() == Swarm.Mode.QUEEN && swarm.server != null) {
                    swarm.server.sendMessage(context.getInput());
                } else {
                    swarm.pathFinder.initiate(context.getArgument("target", Entity.class));
                }
            }
            return SINGLE_SUCCESS;
        })));
    }
}