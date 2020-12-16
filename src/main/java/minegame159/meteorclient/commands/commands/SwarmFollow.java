package minegame159.meteorclient.commands.commands;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.commands.arguments.PlayerArgumentType;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.combat.Swarm;
import minegame159.meteorclient.utils.Chat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SwarmFollow extends Command {

    public SwarmFollow() {
        super("s", "(highlight)follow <?player>(default) - Follow a player. Defaults to the Queen.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("follow").executes(context -> {
                    Swarm swarm = ModuleManager.INSTANCE.get(Swarm.class);
                    if (swarm.currentMode.get() == Swarm.Mode.QUEEN && swarm.server != null && MinecraftClient.getInstance().player != null) {
                        swarm.server.sendMessage(context.getInput() + " " + MinecraftClient.getInstance().player.getDisplayName());
                    }
                    return SINGLE_SUCCESS;
                }).then(argument("name", PlayerArgumentType.player()).executes(context -> {
                    PlayerEntity playerEntity = context.getArgument("name", PlayerEntity.class);
                    Swarm swarm = ModuleManager.INSTANCE.get(Swarm.class);
                    if (swarm.currentMode.get() == Swarm.Mode.QUEEN && swarm.server != null) {
                        swarm.server.sendMessage(context.getInput());
                    } else {
                        if (playerEntity != null) {
                            BaritoneAPI.getProvider().getPrimaryBaritone().getFollowProcess().follow(entity -> entity.getDisplayName().asString().equalsIgnoreCase(playerEntity.getDisplayName().asString()));
                        }
                    }
                    return SINGLE_SUCCESS;
                })
                )
        );
    }
}
