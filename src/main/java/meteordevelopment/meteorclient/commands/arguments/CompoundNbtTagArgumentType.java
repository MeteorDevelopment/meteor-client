/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.util.Collection;
import java.util.List;

// TODO(Ravel): ambiguous static import, members with name EXPECTED_COMPOUND have different new names
//
// TODO(Ravel): ambiguous static import, members with name EXPECTED_COMPOUND have different new names
//
import static net.minecraft.nbt.TagParser.EXPECTED_COMPOUND;

public class CompoundNbtTagArgumentType implements ArgumentType<CompoundTag> {
    private static final CompoundNbtTagArgumentType INSTANCE = new CompoundNbtTagArgumentType();
    private static final Collection<String> EXAMPLES = List.of("{foo:bar}", "{foo:[aa, bb],bar:15}");

    public static CompoundNbtTagArgumentType create() {
        return INSTANCE;
    }

    public static CompoundTag get(CommandContext<?> context) {
        return context.getArgument("nbt", CompoundTag.class);
    }

    private CompoundNbtTagArgumentType() {
    }

    @Override
    public CompoundTag parse(StringReader reader) throws CommandSyntaxException {
        reader.skipWhitespace();
        if (!reader.canRead()) {
            throw EXPECTED_COMPOUND.createWithContext(reader);
        }
        StringBuilder b = new StringBuilder();
        int open = 0;
        while (reader.canRead()) {
            if (reader.peek() == '{') {
                open++;
            } else if (reader.peek() == '}') {
                open--;
            }
            if (open == 0)
                break;
            b.append(reader.read());
        }
        reader.expect('}');
        b.append('}');
        return TagParser.readCompound(b.toString()
            .replace("$", "§")
            .replace("§§", "$")
        );
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
