package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.command.CommandSource;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;

import kaptainwutax.mcutils.version.MCVersion;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.arguments.EnumArgumentType;
import meteordevelopment.meteorclient.systems.seeds.Seed;
import meteordevelopment.meteorclient.systems.seeds.Seeds;
import meteordevelopment.meteorclient.utils.Utils;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

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

        builder.then(literal("list").executes(ctx -> {
            Seeds.get().seeds.forEach((name, seed) -> {
                BaseText text = new LiteralText(name + " ");
                text.append(seed.toText());
                info(text);
            });
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("delete").executes(ctx -> {
            Seed seed = Seeds.get().getSeed();
            if (seed != null) {
                BaseText text = new LiteralText("Deleted ");
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
