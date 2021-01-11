package minegame159.meteorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringNbtReader;

import java.util.Arrays;
import java.util.Collection;

import static net.minecraft.nbt.StringNbtReader.EXPECTED_VALUE;

public class CompoundNbtTagArgumentType implements ArgumentType<CompoundTag> {
    private static final Collection<String> EXAMPLES = Arrays.asList("{foo:bar}", "{foo:[aa, bb],bar:15}");

    public static CompoundNbtTagArgumentType nbtTag() {
        return new CompoundNbtTagArgumentType();
    }

    public CompoundTag parse(StringReader reader) throws CommandSyntaxException {
        reader.skipWhitespace();
        if (!reader.canRead()) {
            throw EXPECTED_VALUE.createWithContext(reader);
        }
        StringBuilder b = new StringBuilder();
        while (reader.canRead() && reader.peek() != '}') {
            b.append(reader.read());
        }
        reader.expect('}');
        b.append('}');
        return StringNbtReader.parse(b.toString()
                .replace("$", "\u00a7")
                .replace("\u00a7\u00a7", "$")
        );
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
