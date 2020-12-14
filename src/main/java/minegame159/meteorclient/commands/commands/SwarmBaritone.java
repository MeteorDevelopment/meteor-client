package minegame159.meteorclient.commands.commands;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.combat.Swarm;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SwarmBaritone extends Command {

    public SwarmBaritone() {
        super("s", "(highlight)b <command> - Generic Baritone Command.");
    }


    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("b").then(argument("argument", StringArgumentType.greedyString()).executes(context -> {
            Swarm swarm = ModuleManager.INSTANCE.get(Swarm.class);
            if (swarm.isActive()) {
                if (swarm.currentMode.get() == Swarm.Mode.QUEEN && swarm.server != null) {
                    swarm.server.sendMessage(context.getInput());
                } else {
                    if (swarm.currentTaskSetting.set(Swarm.CurrentTask.BARITONE)) {
                        BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(context.getArgument("argument",String.class));
                    }
                }
            }
            return SINGLE_SUCCESS;
        })));
    }
}
