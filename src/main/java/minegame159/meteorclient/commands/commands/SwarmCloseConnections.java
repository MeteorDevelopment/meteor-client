package minegame159.meteorclient.commands.commands;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.combat.Swarm;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SwarmCloseConnections extends Command {

    public SwarmCloseConnections(){
        super("s","(highlight)close(default) - Close all network connections and cancel all tasks.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("close").executes(context -> {
                    try {
                        Swarm swarm = ModuleManager.INSTANCE.get(Swarm.class);
                        if(swarm.isActive()) {
                            swarm.closeAllServerConnections();
                            swarm.currentMode = Swarm.Mode.Idle;
                            if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
                                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
                            if (ModuleManager.INSTANCE.get(Swarm.class).isActive())
                                ModuleManager.INSTANCE.get(Swarm.class).toggle();
                        }
                    } catch (Exception ignored) {
                    }
                    return SINGLE_SUCCESS;
                })
        );
    }
}
