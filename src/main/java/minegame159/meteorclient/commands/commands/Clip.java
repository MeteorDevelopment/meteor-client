package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.Vec3d;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Clip extends Command {
    public Clip() {
        super("clip", "Allows you to clip through blocks.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("h")
                .then(argument("blocks", DoubleArgumentType.doubleArg())
                    .executes(context -> {
                        ClientPlayerEntity player = MinecraftClient.getInstance().player;
                        assert player != null;

                        double blocks = context.getArgument("blocks", Double.class);
                        Vec3d forward = Vec3d.fromPolar(0, player.yaw).normalize();
                        player.updatePosition(player.getX() + forward.x * blocks, player.getY(), player.getZ() + forward.z * blocks);

                        return SINGLE_SUCCESS;
                    })))
                .then(literal("v").then(argument("blocks", DoubleArgumentType.doubleArg())
                        .executes(context -> {
                            ClientPlayerEntity player = MinecraftClient.getInstance().player;
                            assert player != null;

                            double blocks = context.getArgument("blocks", Double.class);
                            player.updatePosition(player.getX(), player.getY() + blocks, player.getZ());

                            return SINGLE_SUCCESS;
                        })));
    }
}
