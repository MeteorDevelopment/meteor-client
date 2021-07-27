package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.command.CommandSource;
import net.minecraft.text.BaseText;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import kaptainwutax.mcutils.version.MCVersion;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.arguments.EnumArgumentType;
import meteordevelopment.meteorclient.systems.seeds.Seed;
import meteordevelopment.meteorclient.systems.seeds.Seeds;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import com.mojang.brigadier.arguments.LongArgumentType;

public class SeedCommand extends Command {
    private final static SimpleCommandExceptionType NO_SEED = new SimpleCommandExceptionType(new LiteralText("No seed for current world saved."));

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

        builder.then(argument("seed", LongArgumentType.longArg()).executes(ctx -> {
            Seeds.get().setSeed(LongArgumentType.getLong(ctx, "seed"));
            return SINGLE_SUCCESS;
        }));

        builder.then(argument("seed", LongArgumentType.longArg()).then(argument("version", EnumArgumentType.enumArgument(MCVersion.v1_17_1)).executes(ctx -> {
            Seeds.get().setSeed(LongArgumentType.getLong(ctx, "seed"), EnumArgumentType.getEnum(ctx, "version", MCVersion.v1_17_1));
            return SINGLE_SUCCESS;
        })));
    }

}
