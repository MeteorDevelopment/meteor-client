package anticope.rejects.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ClientPosArgumentType implements ArgumentType<Vec3d> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "~0.5 ~1 ~-5");
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public ClientPosArgumentType() {
    }

    public static ClientPosArgumentType pos() {
        return new ClientPosArgumentType();
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (!(context.getSource() instanceof CommandSource)) {
            return Suggestions.empty();
        } else {
            String string = builder.getRemaining();
            Object collection2 = ((CommandSource)context.getSource()).getBlockPositionSuggestions();

            return CommandSource.suggestPositions(string, (Collection)collection2, builder, CommandManager.getCommandValidator(this::parse));
        }
    }

    public static Vec3d getPos(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Vec3d.class);
    }


    public Vec3d parse(StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();
        double x,y,z;
        CoordinateArgument coordinateArgument = CoordinateArgument.parse(reader);
        CoordinateArgument coordinateArgument2;
        CoordinateArgument coordinateArgument3;
        if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            coordinateArgument2 = CoordinateArgument.parse(reader);
            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip();
                coordinateArgument3 = CoordinateArgument.parse(reader);
            } else {
                reader.setCursor(i);
                throw Vec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
            }
        } else {
            reader.setCursor(i);
            throw Vec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
        }

        x = coordinateArgument.toAbsoluteCoordinate(mc.player.getX());
        y = coordinateArgument2.toAbsoluteCoordinate(mc.player.getY());
        z = coordinateArgument3.toAbsoluteCoordinate(mc.player.getZ());

        return new Vec3d(x,y,z);
    }

}
