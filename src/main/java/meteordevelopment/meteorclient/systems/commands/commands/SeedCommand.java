package anticope.rejects.commands;

import anticope.rejects.arguments.EnumArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.command.CommandSource;

import com.seedfinding.mccore.version.MCVersion;
import meteordevelopment.meteorclient.systems.commands.Command;
import anticope.rejects.utils.seeds.Seed;
import anticope.rejects.utils.seeds.Seeds;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SeedCommand extends Command {
    private final static SimpleCommandExceptionType NO_SEED = new SimpleCommandExceptionType(Text.literal("No seed for current world saved."));

    public SeedCommand() {
        super("seed", "Get or set seed for the current world.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            Seed seed = Seeds.get().getSeed();
            if (seed == null) throw NO_SEED.create();
            info(seed.toText());
            return SINGLE_SUCCESS;
        });

        builder.then(literal("list").executes(ctx -> {
            Seeds.get().seeds.forEach((name, seed) -> {
                MutableText text = Text.literal(name + " ");
                text.append(seed.toText());
                info(text);
            });
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("delete").executes(ctx -> {
            Seed seed = Seeds.get().getSeed();
            if (seed != null) {
                MutableText text = Text.literal("Deleted ");
                text.append(seed.toText());
                info(text);
            }
            Seeds.get().seeds.remove(Utils.getWorldName());
            return SINGLE_SUCCESS;
        }));

        builder.then(argument("seed", StringArgumentType.string()).executes(ctx -> {
            Seeds.get().setSeed(StringArgumentType.getString(ctx, "seed"));
            return SINGLE_SUCCESS;
        }));

        builder.then(argument("seed", StringArgumentType.string()).then(argument("version", EnumArgumentType.enumArgument(MCVersion.latest())).executes(ctx -> {
            Seeds.get().setSeed(StringArgumentType.getString(ctx, "seed"), EnumArgumentType.getEnum(ctx, "version", MCVersion.latest()));
            return SINGLE_SUCCESS;
        })));
    }

}
