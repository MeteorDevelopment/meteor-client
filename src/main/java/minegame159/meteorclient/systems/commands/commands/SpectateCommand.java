package minegame159.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;

import minegame159.meteorclient.systems.commands.arguments.PlayerArgumentType;
import minegame159.meteorclient.systems.commands.Command;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SpectateCommand extends Command {

    public SpectateCommand() {
        super("spectate", "Allows you to spectate nearby players");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            mc.setCameraEntity(mc.player);
            return SINGLE_SUCCESS;
        });

        builder.then(argument("player", PlayerArgumentType.player()).executes(ctx -> {
            PlayerEntity playerEntity = ctx.getArgument("player", PlayerEntity.class);
            mc.setCameraEntity(playerEntity);
            return SINGLE_SUCCESS;
        }));
    }

}
