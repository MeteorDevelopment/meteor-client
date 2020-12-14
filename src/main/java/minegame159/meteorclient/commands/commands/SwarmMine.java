package minegame159.meteorclient.commands.commands;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.combat.Swarm;
import net.minecraft.block.Block;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SwarmMine extends Command {

    public SwarmMine(){
        super("s","(highlight)mine <playername>(default) - Baritone Mine A Block");
    }
    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("mine")
                .then(argument("block", BlockStateArgumentType.blockState())
                        .executes(context -> {
                            String raw = context.getInput();
                            Block block = context.getArgument("block", BlockStateArgument.class).getBlockState().getBlock();
                            Swarm swarm = ModuleManager.INSTANCE.get(Swarm.class);
                            if(swarm.isActive()) {
                                if (swarm.currentMode.get() == Swarm.Mode.QUEEN && swarm.server != null)
                                    swarm.server.sendMessage(raw);
                                if (swarm.currentMode.get() != Swarm.Mode.QUEEN && block != null) {
                                    swarm.currentTaskSetting.set(Swarm.CurrentTask.BARITONE);
                                    BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().mine(block);
                                }
                            }
                            return SINGLE_SUCCESS;
                        })
                )
        );
    }
}
